package org.jrenner.fps.utils;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jrenner.fps.Main;
import org.jrenner.fps.net.NetManager;
import org.jrenner.fps.net.NetServer;
import org.jrenner.fps.net.ServerType;

/** only for Desktop and Headless */
public class CommandArgs {
	public static ScreenSize process(String[] args) {
		Options options = new Options();
		options.addOption("s", "server", false, "start online server");
		options.addOption("p", "port", true, "specify TCP port to either host on (server) or connect to (client)");
		options.addOption("c", "client", false, "connect to server as a client");
		options.addOption("a", "address", true, "supply hostname address to connect to");
		options.addOption("d", "lag-delay", true, "simulate lag with argument = milliseconds of lag");
		options.addOption("z", "screensize", true, "supply screen size in the form of WIDTHxHEIGHT, i.e. 1920x1080");
		options.addOption("h", "help", false, "print help");

		boolean printHelpAndQuit = false;

		ScreenSize screenSize = null;

		CommandLineParser parser = new BasicParser();
		CommandLine cli = null;
		try {
			cli = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			printHelpAndQuit = true;
		}
		if (cli != null && cli.hasOption('h')) {
			printHelpAndQuit = true;
		}
		if (cli != null && cli.getOptions().length == 0) {
			Main.serverType = ServerType.Local;
			Main.hasClient = true;
		}
		else if (cli != null && !printHelpAndQuit) {
			if (cli.hasOption('z')) {
				String[] pieces = cli.getOptionValue('z').split("x");
				int width = Integer.parseInt(pieces[0]);
				int height = Integer.parseInt(pieces[1]);
				screenSize = new ScreenSize();
				screenSize.width = width;
				screenSize.height = height;
			}
			if (cli.hasOption('s')) {
				Main.serverType = ServerType.Online;
				System.out.println("server type: online");
			}
			if (cli.hasOption('l')) {
				if (Main.serverType != null) {
					System.out.println("please choose local or online server, not both");
					printHelpAndQuit = true;
				} else {
					Main.serverType = ServerType.Local;
				}
			}
			if (cli.hasOption('c')) {
				Main.hasClient = true;
			}
			if (cli.hasOption('a')) {
				String value = cli.getOptionValue('a');
				if (value != null) {
					NetManager.host = value;
				}
			}
			if (cli.hasOption('p')) {
				int port = Integer.parseInt(cli.getOptionValue('p'));
				NetManager.tcpPort = port;
			}
			if (cli.hasOption('d')) {
				NetServer.simulateLag = true;
				int lagMillis = Integer.parseInt(cli.getOptionValue('d'));
				NetServer.lagMin = lagMillis;
				NetServer.lagMax = lagMillis;
			}
			// verify
			if (!Main.isClient() && !Main.isServer()) {
				System.out.println("please choose client and server options");
				printHelpAndQuit = true;
			}
		}
		if (printHelpAndQuit) {
			HelpFormatter hf = new HelpFormatter();
			hf.printHelp("java -jar myjarfile.jar <args>", options);
			System.exit(1);
		}
		return screenSize;
	}

	public static class ScreenSize {
		public int width, height;
	}
}
