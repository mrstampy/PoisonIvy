/*
 * Poison Ivy - Java Library Dependency Resolver and Application Launcher 
 *
 * Copyright (C) 2014 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.poisonivy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class uses <a href="http://ant.apache.org/ivy>Apache Ivy</a> to retrieve
 * libraries for the ivy file specified. By default it is expected to be
 * {@link IvyLibraryRetriever#IVY_XML}.<br>
 * <br>
 * The libraries retrieved are saved by default to
 * {@link IvyLibraryRetriever#LIBRARIES_DIR}. Other parameters are explained
 * below.
 */
public class IvyLibraryRetriever {
	private static final Logger log = LoggerFactory.getLogger(IvyLibraryRetriever.class);

	/** The Constant IVY_XML {@value #IVY_XML} */
	public static final String IVY_XML = "./ivy.xml";

	/** The Constant LIBRARIES_DIR {@value #LIBRARIES_DIR}. */
	public static final String LIBRARIES_DIR = "./ivylib";

	/** The Constant RESOLVE_PATTERN {@value #RESOLVE_PATTERN}. */
	public static final String RESOLVE_PATTERN = "[artifact]-[revision](-[classifier]).[ext]";

	private static final String[] srcsNDocs = { "-javadoc.", "-javadocs.", "-doc.", "-source.", "-sources.", "-src." };

	private boolean cleanSourcesAndJavadoc = true;

	private String libdir = LIBRARIES_DIR;
	private String resolvePattern = libdir + File.separator + RESOLVE_PATTERN;

	private List<ProcessListener> listeners = new ArrayList<ProcessListener>();

	public void addProcessListeners(List<ProcessListener> listeners) {
		this.listeners.addAll(listeners);
	}

	public void addProcessListener(ProcessListener l) {
		if (l != null && !listeners.contains(l)) listeners.add(l);
	}

	public void removeProcessListener(ProcessListener l) {
		if (l != null) listeners.remove(l);
	}

	/**
	 * Retrieve libraries.
	 * 
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 */
	public boolean retrieveLibraries() throws Exception {
		return retrieveLibraries(false);
	}

	/**
	 * Retrieve libraries.
	 * 
	 * @param force
	 *          , if true will delete all libraries in the {@link #getLibdir()}
	 *          directory and pull fresh copies from the Maven repositories.
	 * 
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 */
	public boolean retrieveLibraries(boolean force) throws Exception {
		return retrieveLibraries(force, null);
	}

	/**
	 * Retrieve libraries.
	 * 
	 * @param force
	 *          , if true will delete all libraries in the {@link #getLibdir()}
	 *          directory and pull fresh copies from the Maven repositories.
	 * 
	 * @param ivyfile
	 *          the path and filename of the ivy file if not {@value #IVY_XML}
	 * 
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 */
	public boolean retrieveLibraries(boolean force, String ivyfile) throws Exception {
		return retrieveLibraries(force, ivyfile, null);
	}

	/**
	 * Retrieve libraries.
	 * 
	 * @param force
	 *          , if true will delete all libraries in the {@link #getLibdir()}
	 *          directory and pull fresh copies from the Maven repositories.
	 * 
	 * @param ivyfile
	 *          the path and filename of the ivy file if not {@value #IVY_XML}
	 * 
	 * @param ivysettings
	 *          the ivysettings to use for the supplied ivyfile value
	 * 
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 */
	public boolean retrieveLibraries(boolean force, String ivyfile, String ivysettings) throws Exception {
		boolean libsExist = librariesRetrieved();

		if (libsExist && !force) {
			log.debug("Libraries previously retrieved");
			return true;
		}

		if (force) clearLibraryDirectory();

		File ivy = getFile(ivyfile == null ? IVY_XML : ivyfile, "ivy");

		return execIvyMain(ivy.getAbsolutePath(), ivysettings);
	}

	/**
	 * Clear the library directory of all files.
	 */
	public void clearLibraryDirectory() {
		log.debug("Clearing library directory");
		File libdir = new File(getLibdir());

		if (libdir.exists()) {
			File[] files = libdir.listFiles();
			for (File f : files) {
				deleteFile(f);
			}
			deleteFile(libdir);
		}

		libdir.mkdir();
	}

	private void deleteFile(File f) {
		boolean b = f.delete();
		if (b) {
			log.debug("Deleted {}", f.getAbsolutePath());
		} else {
			log.error("Could not delete {}", f.getAbsolutePath());
		}
	}

	private boolean execIvyMain(String ivyfile, String ivysettings) throws Exception {
		log.debug("Retrieving libraries using {}", ivyfile);
		if (ivysettings != null) {
			log.debug("...and ivy settings {}", ivysettings);
			getFile(ivysettings, "ivy settings");
		}

		String[] cmd = createCommand(ivyfile, ivysettings);
		logCmd(cmd);

		ProcessContainer pc = new ProcessContainer(Runtime.getRuntime().exec(cmd), listeners);

		int code = pc.getProcess().waitFor();
		
		pc.setRunning(false);

		if (code == 0) {
			log.debug("Ivy library retrieval completed with {}", code);
			if (isCleanSourcesAndJavadoc()) cleanSourcesAndJavadoc();
			return true;
		}

		log.error("Ivy library retrieval completed with {}", code);

		return false;
	}

	private void logCmd(String[] cmd) {
		if (!log.isDebugEnabled()) return;

		log.debug("Executing with the following command parameters:");
		for (String s : cmd) {
			log.debug("**** Command parameter: {}", s);
		}
	}

	private void cleanSourcesAndJavadoc() {
		File libdir = new File(getLibdir());

		File[] sourcesAndJavadoc = libdir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				String name = pathname.getName();
				for (String s : srcsNDocs) {
					if (name.indexOf(s) > 0) return true;
				}
				return false;
			}
		});

		for (File del : sourcesAndJavadoc) {
			deleteFile(del);
		}
	}

	private String[] createCommand(String ivyfile, String ivysettings) {
		List<String> command = new ArrayList<String>();

		command.add("java");
		command.add("-cp");
		command.add(getClasspath());
		command.add("org.apache.ivy.Main");
		command.add("-ivy");
		command.add(ivyfile);

		if (ivysettings != null) {
			command.add("-settings");
			command.add(ivysettings);
		}

		command.add("-retrieve");
		command.add(getResolvePattern());

		return command.toArray(new String[] {});
	}

	private File getFile(String fileName, String name) throws URISyntaxException, IOException {
		File file = getFromFileSystem(fileName, name);
		if (file == null || !file.exists()) file = getFromClassPath(fileName);

		if (file == null || !file.exists()) {
			log.error("No {} file found for {}", name, fileName);
			throw new FileNotFoundException(fileName);
		}

		log.debug("Returning file {}", file.getAbsolutePath());

		return file;
	}

	private File getFromClassPath(String fileName) throws URISyntaxException, IOException {
		String fn = fileName;
		URL url = getClass().getResource(fn);
		if (url == null) {
			fn = "/" + fn;
			url = getClass().getResource(fn);
		}

		return isJarUrl(url) ? extractToFileSystem(fn) : new File(url.toURI());
	}

	private boolean isJarUrl(URL url) {
		return url != null && url.toString().startsWith("jar");
	}

	private File extractToFileSystem(String fileName) throws IOException {
		BufferedInputStream is = new BufferedInputStream(getClass().getResourceAsStream(fileName));

		File temp = File.createTempFile("ivy", "xml");

		byte[] b = new byte[is.available()];
		is.read(b);

		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(temp));
			out.write(b);

			return temp;
		} finally {
			if (out != null) out.close();
		}
	}

	protected File getFromFileSystem(String fileName, String name) throws FileNotFoundException {
		return new File(fileName);
	}

	/**
	 * Returns true if the {@link #getLibdir()} exists and is not empty.
	 * 
	 * @return true, if successful
	 */
	public boolean librariesRetrieved() {
		File lib = new File(getLibdir());
		if (!lib.exists()) return false;
		if (!lib.isDirectory()) return false;

		String[] libs = lib.list();

		log.debug("Libraries in {}", getLibdir());
		for (String s : libs) {
			log.debug("**** Library: {}", s);
		}

		return libs != null && libs.length > 0;
	}

	/**
	 * Gets the classpath used to start the currently running Java process.
	 * 
	 * @return the classpath
	 */
	public static String getClasspath() {
		return System.getProperty("java.class.path");
	}

	/**
	 * If true any source and javadoc jars are deleted after library retrieval.
	 * 
	 * @return true, if is clean sources and javadoc
	 */
	public boolean isCleanSourcesAndJavadoc() {
		return cleanSourcesAndJavadoc;
	}

	/**
	 * If true any source and javadoc jars are deleted after library retrieval.
	 * 
	 * @param cleanSourcesAndJavadoc
	 *          the new clean sources and javadoc
	 */
	public void setCleanSourcesAndJavadoc(boolean cleanSourcesAndJavadoc) {
		this.cleanSourcesAndJavadoc = cleanSourcesAndJavadoc;
	}

	/**
	 * Gets the libdir, default {@value #LIBRARIES_DIR}.
	 * 
	 * @return the libdir
	 */
	public String getLibdir() {
		return libdir == null ? LIBRARIES_DIR : libdir;
	}

	/**
	 * Sets the libdir, default {@value #LIBRARIES_DIR}.
	 * 
	 * @param libdir
	 *          the new libdir
	 */
	public void setLibdir(String libdir) {
		this.libdir = libdir;
	}

	/**
	 * Gets the resolve pattern including the {@link #getLibdir()}, default
	 * {@value #LIBRARIES_DIR}/{@value #RESOLVE_PATTERN}.
	 * 
	 * @return the resolve pattern
	 */
	public String getResolvePattern() {
		return resolvePattern == null ? getLibdir() + File.separator + RESOLVE_PATTERN : resolvePattern;
	}

	/**
	 * Sets the resolve pattern, default {@value #RESOLVE_PATTERN}.
	 * 
	 * @param resolvePattern
	 *          the new resolve pattern
	 */
	public void setResolvePattern(String resolvePattern) {
		this.resolvePattern = resolvePattern;
	}

}
