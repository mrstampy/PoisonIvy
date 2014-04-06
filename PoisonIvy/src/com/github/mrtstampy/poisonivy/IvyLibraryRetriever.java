package com.github.mrtstampy.poisonivy;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IvyLibraryRetriever {
	private static final Logger log = LoggerFactory.getLogger(IvyLibraryRetriever.class);

	public static final String LIBRARIES_DIR = "ivylib";
	public static final String RESOLVE_PATTERN = LIBRARIES_DIR + "/[artifact]-[revision](-[classifier]).[ext]";

	private static final String[] srcsNDocs = { "-javadoc.", "-javadocs.", "-doc.", "-source.", "-sources.", "-src." };

	private boolean cleanSourcesAndJavadoc = true;

	public boolean retrieveLibraries() throws Exception {
		return retrieveLibraries(false);
	}

	public boolean retrieveLibraries(boolean force) throws Exception {
		return retrieveLibraries(force, null);
	}

	public boolean retrieveLibraries(boolean force, String ivyfile) throws Exception {
		return retrieveLibraries(force, ivyfile, null);
	}

	public boolean retrieveLibraries(boolean force, String ivyfile, String ivysettings) throws Exception {
		boolean libsExist = librariesRetrieved();

		if (libsExist && !force) {
			log.debug("Libraries previously retrieved");
			return true;
		}
		
		if(force) clearLibraryDirectory();

		File ivy = getFile(ivyfile == null ? "./ivy.xml" : ivyfile, "ivy");

		return execIvyMain(ivy.getAbsolutePath(), ivysettings);
	}

	public void clearLibraryDirectory() {
		log.debug("Clearing library directory");
		File libdir = new File(LIBRARIES_DIR);
		
		if(libdir.exists()) {
			File[] files = libdir.listFiles();
			for(File f : files) {
				deleteFile(f);
			}
			deleteFile(libdir);
		}
		
		libdir.mkdir();
	}
	
	private void deleteFile(File f) {
		boolean b = f.delete();
		if(b) {
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

		Process p = Runtime.getRuntime().exec(createCommand(ivyfile, ivysettings));
		int code = p.waitFor();

		logOutput(p.getInputStream());
		logError(p.getErrorStream());

		if (code == 0) {
			log.debug("Ivy library retrieval completed with {}", code);
			if (isCleanSourcesAndJavadoc()) cleanSourcesAndJavadoc();
			return true;
		}
		
		log.error("Ivy library retrieval completed with {}", code);
		
		return false;
	}

	private void cleanSourcesAndJavadoc() {
		File libdir = new File(LIBRARIES_DIR);

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

	private void logError(InputStream in) throws IOException {
		String error = getOutput(in);
		if (error != null) {
			System.err.println(error);
			log.error(error);
		}
	}

	private void logOutput(InputStream in) throws IOException {
		String out = getOutput(in);
		if (out != null) {
			System.out.println(out);
			log.debug(getOutput(in));
		}
	}

	private String getOutput(InputStream in) throws IOException {
		int available = in.available();
		if (available <= 0) return null;

		byte[] b = new byte[available];
		in.read(b);

		return new String(b);
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
		command.add(RESOLVE_PATTERN);

		return command.toArray(new String[] {});
	}

	private File getFile(String fileName, String name) throws FileNotFoundException {
		File file = new File(fileName);

		if (file == null || !file.exists()) {
			log.error("No {} file found for {}", name, fileName);
			throw new FileNotFoundException(fileName);
		}

		return file;
	}

	public boolean librariesRetrieved() {
		File lib = new File(LIBRARIES_DIR);
		if (!lib.exists()) return false;
		if (!lib.isDirectory()) return false;

		String[] libs = lib.list();

		log.debug("Libraries: ", (Object[]) libs);

		return libs != null && libs.length > 0;
	}

	public static String getClasspath() {
		return System.getProperty("java.class.path");
	}

	public boolean isCleanSourcesAndJavadoc() {
		return cleanSourcesAndJavadoc;
	}

	public void setCleanSourcesAndJavadoc(boolean cleanSourcesAndJavadoc) {
		this.cleanSourcesAndJavadoc = cleanSourcesAndJavadoc;
	}

}
