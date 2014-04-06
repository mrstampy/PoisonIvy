package com.github.mrtstampy.poisonivy;

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

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		String[] poisonArgs = getArgs(args);
		
		Parser parser = new BasicParser();
		CommandLine cli = parser.parse(getOptions(), poisonArgs);

		if (cli.hasOption("help") || !cli.hasOption("mainjar")) {
			printHelpMessage();
		} else if (executeLibraryRetrieval(cli)) {
			exeuteMainJar(cli);
		} else {
			log.error("Could not start application, exiting");
			System.exit(-1);
		}
	}

	private static String[] getArgs(String[] args) throws IOException {
		File poisonIvyConfig = new File("poisonivy.config");
		
		if((args == null || args.length == 0) && !poisonIvyConfig.exists()) {
			printHelpMessage();
			System.exit(-1);
		}
		
		return args.length > 0 ? args : parsePoisonIvyConfig(poisonIvyConfig);
	}

	private static String[] parsePoisonIvyConfig(File poisonIvyConfig) throws IOException {
		BufferedReader reader = null; 
		
		try {
			reader = new BufferedReader(new FileReader(poisonIvyConfig));
			String cmdline = reader.readLine();
			
			return cmdline.split(" ");
		} finally {
			if(reader != null) reader.close();
		}
	}

	private static void printHelpMessage() {
		System.out.println("Usage: ");
		System.out.println();
		
		Options opts = getOptions();

		for (Object op : opts.getOptions()) {
			Option o = (Option)op;
			System.out.println("-" + o.getOpt() + (o.hasArg() ? " [ARG]" : "") + " - " + o.getDescription());
		}
		
		System.out.println();
		
		System.out.println("The '-mainjar' parameter must be specified");
	}

	private static void exeuteMainJar(CommandLine cli) throws IOException {
		Runtime.getRuntime().exec(getCommand(cli));
	}
	
	private static String[] getCommand(CommandLine cli) {
		List<String> command = new ArrayList<String>();
		
		command.add("java");
		
		if(cli.hasOption("javaopts")) {
			command.add(cli.getOptionValue("javaopts"));
		}
		
		command.add("-jar");
		command.add(cli.getOptionValue("mainjar"));
		
		return command.toArray(new String[]{});
	}

	private static boolean executeLibraryRetrieval(CommandLine cli) throws Exception {
		IvyLibraryRetriever retriever = new IvyLibraryRetriever();

		String ivy = cli.getOptionValue("ivy");
		String ivysettings = cli.getOptionValue("ivysettings");
		boolean force = cli.hasOption("force");

		return retriever.retrieveLibraries(force, ivy, ivysettings);
	}

	private static Options getOptions() {
		Options opts = new Options();

		opts.addOption("help", false, "Prints help message");
		opts.addOption("ivy", true, "The ivy file for library dependency resolution");
		opts.addOption("ivysettings", true, "The ivy settings file for library dependency resolution");
		opts.addOption("force", false, "Force clean library retrieval");
		opts.addOption("javaopts", true, "Java options to pass to the application jar");
		opts.addOption("mainjar", true, "The application jar to execute (java -jar [mainjar])");

		return opts;
	}

}
