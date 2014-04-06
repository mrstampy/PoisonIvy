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

import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class PoisonIvy is designed to be used in one of three ways:<br/>
 * <br/>
 * 
 * 1. As a separate application.<br/>
 * 2. As an integrated class.<br/>
 * 3. As a main class superclass.<br/>
 * <br/>
 * 
 * Parameters can be specified on the command line or specified in a file called
 * {@value #POISONIVY_CONFIG}.
 */
public class PoisonIvy {
	private static final Logger log = LoggerFactory.getLogger(PoisonIvy.class);

	/** The Constant MAIN_JAR_PARM -{@value #MAIN_JAR_PARM}. */
	public static final String MAIN_JAR_PARM = "mj";

	/** The Constant MAIN_JAR_PARM -{@value #MAIN_CLASS_PARM}. */
	public static final String MAIN_CLASS_PARM = "mc";

	/** The Constant FORCE_PARM -{@value #FORCE_PARM}. */
	public static final String FORCE_PARM = "f";

	/** The Constant HELP_PARM -{@value #HELP_PARM}. */
	public static final String HELP_PARM = "h";

	/** The Constant LIB_DIR_PARM -{@value #LIB_DIR_PARM}. */
	public static final String LIB_DIR_PARM = "libdir";

	/** The Constant IVY_SETTINGS_PARM -{@value #IVY_SETTINGS_PARM}. */
	public static final String IVY_SETTINGS_PARM = "ivysettings";

	/** The Constant IVY_PARM -{@value #IVY_PARM}. */
	public static final String IVY_PARM = "ivy";

	/** The Constant RESOLVE_PATTERN_PARM -{@value #RESOLVE_PATTERN_PARM}. */
	public static final String RESOLVE_PATTERN_PARM = "rp";

	/** The Constant NO_CLEAN_PARM -{@value #NO_CLEAN_PARM}. */
	public static final String NO_CLEAN_PARM = "nc";

	/** The Constant POISONIVY_CONFIG {@value #POISONIVY_CONFIG}. */
	public static final String POISONIVY_CONFIG = "poisonivy.config";

	private String[] args;

	/**
	 * The main method.
	 * 
	 * @param args
	 *          the arguments
	 */
	public static void main(String[] args) {
		try {
			new PoisonIvy(args).execute();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Unexpected exception", e);
		}
	}

	/**
	 * Instantiates a new poison ivy.
	 * 
	 * @param args
	 *          the args
	 */
	public PoisonIvy(String... args) {
		this.args = args;
	}

	/**
	 * Execute.
	 * 
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 */
	public boolean execute() {
		try {
			return executeImpl();
		} catch (Exception e) {
			log.error("Could not execute", e);
			out.println(stackTraceToString(e));
			printHelpMessage();
		}

		return false;
	}

	protected boolean executeImpl() throws Exception {
		String[] poisonArgs = getArgs();
		logArgs(poisonArgs);

		Parser parser = new BasicParser();
		CommandLine cli = parser.parse(getOptions(), poisonArgs);

		if (cli.hasOption(HELP_PARM)) {
			printHelpMessage();
			return true;
		}
		
		if (executeLibraryRetrieval(cli)) {
			if (cli.hasOption(MAIN_JAR_PARM) || cli.hasOption(MAIN_CLASS_PARM)) executeMain(cli);
			return true;
		}
		
		log.error("Could not retrieve libraries via ivy");
		
		return false;
	}

	private String stackTraceToString(Exception e) {
		StringWriter writer = new StringWriter();

		e.printStackTrace(new PrintWriter(writer));

		return writer.toString();
	}

	protected void logArgs(String[] args) {
		if (!log.isDebugEnabled()) return;

		log.debug("Executing with the following command parameters:");
		for (String s : args) {
			log.debug("**** Command parameter: {}", s);
		}
	}

	/**
	 * Gets the args.
	 * 
	 * @return the args
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
	protected String[] getArgs() throws IOException {
		File poisonIvyConfig = new File(POISONIVY_CONFIG);

		if ((args == null || args.length == 0) && !poisonIvyConfig.exists()) {
			printHelpMessage();
			exit(-1);
		}

		return args.length > 0 ? args : parsePoisonIvyConfig(poisonIvyConfig);
	}

	/**
	 * Poison ivy config exists.
	 * 
	 * @return true, if successful
	 */
	public boolean poisonIvyConfigExists() {
		return new File(POISONIVY_CONFIG).exists();
	}

	/**
	 * Parses the poison ivy config, reading the first line of the file as if it
	 * was passed on the command line.
	 * 
	 * @param poisonIvyConfig
	 *          the poison ivy config
	 * @return the string[] arguments
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
	protected String[] parsePoisonIvyConfig(File poisonIvyConfig) throws IOException {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(poisonIvyConfig));
			String cmdline = reader.readLine();

			return cmdline.split(" ");
		} finally {
			if (reader != null) reader.close();
		}
	}

	/**
	 * Prints the help message to System.out.
	 */
	protected void printHelpMessage() {
		out.println("Poison Ivy - Java Library Dependency Resolver and Application Launcher");
		out.println();
		out.println("Licence: GPL 2.0");
		out.println("Copyright Burton Alexander 2014");
		out.println();
		out.println("Usage: ");
		out.println();

		Options opts = getOptions();

		for (Object op : opts.getOptions()) {
			Option o = (Option) op;
			out.println("-" + o.getOpt() + (o.hasArg() ? " [ARG]" : "") + " - " + o.getDescription());
		}

		out.println();
		out.println("The required options can be put into a file named '" + POISONIVY_CONFIG + "'");
		out.println();
		printExamples();
		out.println();
		out.println("See some website for more information");
	}

	private void printExamples() {
		out.println("Examples:");
		out.println("");
		out.println("-mj MyApplication.jar");
		out.println("");
		out.println("-mj MyApplication.jar -X mx1000m -X ms500m");
		out.println("");
		out.println("-mc com.my.MainClass -X mx1000m -X ms500m -D my.settings.file=/some/path/and/file");
		out.println("");
		out.println("-ivy /path/to/ivy.xml -ivysettings /path/to/ivysettings.xml -libdir /path/to/ivylib -f -nc");
	}

	/**
	 * Execute the main jar Java application or main class using the command
	 * options specified.
	 * 
	 * @param cli
	 *          the cli
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 * 
	 * @see #MAIN_CLASS_PARM
	 * @see #MAIN_JAR_PARM
	 * @see #JAVA_OPTS_PARM
	 */
	protected void executeMain(CommandLine cli) throws IOException {
		Runtime.getRuntime().exec(getCommand(cli));
	}

	/**
	 * Gets the command used to execute the main jar, if requested.
	 * 
	 * @param cli
	 *          the cli
	 * @return the command
	 * 
	 * @see #MAIN_CLASS_PARM
	 * @see #MAIN_JAR_PARM
	 * @see #JAVA_OPTS_PARM
	 */
	protected String[] getCommand(CommandLine cli) {
		List<String> command = new ArrayList<String>();

		command.add("java");
		command.add("-cp");
		command.add(IvyLibraryRetriever.getClasspath());
		
		addDOptions(cli, command);
		addXOptions(cli, command);

		if (cli.hasOption(MAIN_JAR_PARM)) {
			command.add("-jar");
			command.add(cli.getOptionValue(MAIN_JAR_PARM));
		} else if (cli.hasOption(MAIN_CLASS_PARM)) {
			command.add(cli.getOptionValue(MAIN_CLASS_PARM));
		}

		return command.toArray(new String[] {});
	}

	private void addXOptions(CommandLine cli, List<String> command) {
		String[] xs = cli.getOptionValues("X");
		if(xs == null || xs.length == 0) return;
		
		for(String x : xs) {
			command.add("-X" + x);
		}
	}

	private void addDOptions(CommandLine cli, List<String> command) {
		Properties props = cli.getOptionProperties("D");
		if(props == null || props.isEmpty()) return;
		
		for(Entry<Object, Object> entry : props.entrySet()) {
			command.add("-D" + entry.getKey() + "=" + entry.getValue());
		}
	}

	/**
	 * Execute library retrieval.
	 * 
	 * @param cli
	 *          the cli
	 * @return true, if successful
	 * @throws Exception
	 *           the exception
	 * 
	 * @see #IVY_PARM
	 * @see #IVY_SETTINGS_PARM
	 * @see #NO_CLEAN_PARM
	 * @see #RESOLVE_PATTERN_PARM
	 * @see #LIB_DIR_PARM
	 * @see #FORCE_PARM
	 */
	protected boolean executeLibraryRetrieval(CommandLine cli) throws Exception {
		IvyLibraryRetriever retriever = new IvyLibraryRetriever();

		String ivy = cli.getOptionValue(IVY_PARM);
		String ivysettings = cli.getOptionValue(IVY_SETTINGS_PARM);
		boolean force = cli.hasOption(FORCE_PARM);

		if (cli.hasOption(LIB_DIR_PARM)) retriever.setLibdir(cli.getOptionValue(LIB_DIR_PARM));

		retriever.setCleanSourcesAndJavadoc(!cli.hasOption(NO_CLEAN_PARM));

		return retriever.retrieveLibraries(force, ivy, ivysettings);
	}

	/**
	 * Gets the options.
	 * 
	 * @return the options
	 */
	@SuppressWarnings("static-access")
	protected Options getOptions() {
		Options opts = new Options();

		opts.addOption(HELP_PARM, false, "Prints help message");
		opts.addOption(IVY_PARM, true, "The ivy file for library dependency resolution (default: ./ivy.xml)");
		opts.addOption(IVY_SETTINGS_PARM, true,
				"The ivy settings file for library dependency resolution (default: built in settings)");
		opts.addOption(RESOLVE_PATTERN_PARM, true, "The ivy resolve pattern (default: "
				+ IvyLibraryRetriever.RESOLVE_PATTERN + ")");
		opts.addOption(FORCE_PARM, false, "Force clean library retrieval (default: false)");
		opts.addOption(NO_CLEAN_PARM, false,
				"Do not remove source and api documentation after library dependency retrieval (default: clean)");
		opts.addOption(LIB_DIR_PARM, true, "The directory to store the retrieved librarires (default: ./ivylib)");
		opts.addOption(MAIN_JAR_PARM, true, "The application jar to execute");
		opts.addOption(MAIN_CLASS_PARM, true, "The main class to execute");

		opts.addOption(OptionBuilder.withArgName("property=value").hasArgs(2).withValueSeparator()
				.withDescription("Java -Dproperty=value command line properties").create("D"));

		opts.addOption(OptionBuilder.withArgName("Xparm").hasArgs(1).withDescription("Java -Xparm command line properties")
				.create("X"));

		return opts;
	}

}
