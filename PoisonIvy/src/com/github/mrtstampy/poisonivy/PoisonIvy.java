package com.github.mrtstampy.poisonivy;

import static java.lang.System.exit;
import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoisonIvy {
	private static final Logger log = LoggerFactory.getLogger(PoisonIvy.class);

	public static final String MAIN_JAR_PARM = "mainjar";
	public static final String JAVA_OPTS_PARM = "javaopts";
	public static final String FORCE_PARM = "force";
	public static final String HELP_PARM = "help";
	public static final String LIB_DIR_PARM = "libdir";
	public static final String IVY_SETTINGS_PARM = "ivysettings";
	public static final String IVY_PARM = "ivy";
	public static final String RESOLVE_PATTERN_PARM = "rp";
	public static final String NO_CLEAN_PARM = "nc";

	public static final String POISONIVY_CONFIG = "poisonivy.config";

	private String[] args;

	public static void main(String[] args) {
		try {
			new PoisonIvy(args).execute();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Unexpected exception", e);
		}
	}

	public PoisonIvy(String... args) {
		this.args = args;
	}

	public boolean execute() throws Exception {
		String[] poisonArgs = getArgs(args);

		Parser parser = new BasicParser();
		CommandLine cli = parser.parse(getOptions(), poisonArgs);

		if (cli.hasOption(HELP_PARM) || !cli.hasOption(MAIN_JAR_PARM)) {
			printHelpMessage();
			return true;
		} else if (executeLibraryRetrieval(cli)) {
			exeuteMainJar(cli);
			return true;
		} else {
			log.error("Could not start application");
		}
		
		return false;
	}

	protected String[] getArgs(String[] args) throws IOException {
		File poisonIvyConfig = new File(POISONIVY_CONFIG);

		if ((args == null || args.length == 0) && !poisonIvyConfig.exists()) {
			printHelpMessage();
			exit(-1);
		}

		return args.length > 0 ? args : parsePoisonIvyConfig(poisonIvyConfig);
	}

	public boolean poisonIvyConfigExists() {
		return new File(POISONIVY_CONFIG).exists();
	}

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

	protected void printHelpMessage() {
		out.println("Poison Ivy - Java Library Dependency Resolver and Application Launcher");
		out.println();
		printCopyrightMessage();
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
		out.println("See some website for more information");
	}
	
	protected void printCopyrightMessage() {
		out.println("Licence: GPL 2.0");
		out.println("Copyright Burton Alexander 2014");
	}

	protected void exeuteMainJar(CommandLine cli) throws IOException {
		Runtime.getRuntime().exec(getCommand(cli));
	}

	protected String[] getCommand(CommandLine cli) {
		List<String> command = new ArrayList<String>();

		command.add("java");
		command.add("-cp");
		command.add(IvyLibraryRetriever.getClasspath());

		if (cli.hasOption(JAVA_OPTS_PARM)) {
			command.add(cli.getOptionValue(JAVA_OPTS_PARM));
		}

		command.add("-jar");
		command.add(cli.getOptionValue(MAIN_JAR_PARM));

		return command.toArray(new String[] {});
	}

	protected boolean executeLibraryRetrieval(CommandLine cli) throws Exception {
		IvyLibraryRetriever retriever = new IvyLibraryRetriever();

		String ivy = cli.getOptionValue(IVY_PARM);
		String ivysettings = cli.getOptionValue(IVY_SETTINGS_PARM);
		boolean force = cli.hasOption(FORCE_PARM);

		if (cli.hasOption(LIB_DIR_PARM)) retriever.setLibdir(cli.getOptionValue(LIB_DIR_PARM));
		
		retriever.setCleanSourcesAndJavadoc(!cli.hasOption(NO_CLEAN_PARM));

		return retriever.retrieveLibraries(force, ivy, ivysettings);
	}

	protected Options getOptions() {
		Options opts = new Options();

		opts.addOption(HELP_PARM, false, "Prints help message");
		opts.addOption(IVY_PARM, true, "The ivy file for library dependency resolution (default: ./ivy.xml)");
		opts.addOption(IVY_SETTINGS_PARM, true,
				"The ivy settings file for library dependency resolution (default: built in settings)");
		opts.addOption(RESOLVE_PATTERN_PARM, true, "The ivy resolve pattern (default: " + IvyLibraryRetriever.RESOLVE_PATTERN + ")");
		opts.addOption(FORCE_PARM, false, "Force clean library retrieval (default: false)");
		opts.addOption(NO_CLEAN_PARM, false, "Do not remove source and api documentation after library dependency retrieval (default: clean)");
		opts.addOption(LIB_DIR_PARM, true, "The directory to store the retrieved librarires (default: ./ivylib)");
		opts.addOption(JAVA_OPTS_PARM, true,
				"Java options to pass to the application jar (enclose in single quotes for multiple parameters)");
		opts.addOption(MAIN_JAR_PARM, true, "The application jar to execute (java -jar [mainjar]) *REQUIRED*");

		return opts;
	}

}
