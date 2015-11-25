package com.nrgpatagonia;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.nrgpatagonia.DataReaderFrontEnd.SQLSelect;

public class MainClass {

	private static ModbusConnections modbusConnections;

	private static Thread thread;

	private static DataReader dataReader;

	private static DataReaderFrontEnd dataReaderFrontEnd;

	static int rowCounter = 0;

	public MainClass() {

	}

	public static void main(String[] args) {

		// create Options object
		Options options = new Options();

		// add options
		options.addOption("dataBase", false, "guardar en base de datos");

		Option tcpPortOption = OptionBuilder.withArgName("address").hasArg()
				.withDescription("direccion:puerto molino")
				.create("tcpAddress");

		Option serialPortOption = OptionBuilder.withArgName("port").hasArg()
				.withDescription("puerto estacion meteorologica")
				.create("serialPort");

		Option outputFileOption = OptionBuilder.withArgName("file").hasArg()
				.withDescription("ruta los de datos").create("output");

		options.addOption(tcpPortOption);
		options.addOption(serialPortOption);
		options.addOption(outputFileOption);

		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("dataReader", options);

		modbusConnections = new ModbusConnections();

		System.out.println("Al principio TCP CON: "
				+ modbusConnections.getTcpCon().isConnected());
		System.out.println("al princicpio Serial Conn: "
				+ modbusConnections.getSerialCon().isOpen());

		dataReader = new DataReader(modbusConnections.getTcpTrans(),
				modbusConnections.getSerialTrans());

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// has the buildfile argument been passed?
			if (line.hasOption("tcpAddress")) {
				String addressAndPort[] = line.getOptionValue("tcpAddress")
						.split(":");

				if (addressAndPort.length != 2) {
					System.out
							.println("Error. El primer argumento debe ser direccion:puerto.");
					System.exit(1);
				} else {

					// try {
					// dataReader.setTcpAddr(InetAddress.getByName(addressAndPort
					// [0])); } catch (UnknownHostException e) { // TODO
					// Auto-generated catch block e.printStackTrace();
					// System.exit(2); }
					// dataReader.setTcpPort(Integer.valueOf(addressAndPort
					// [1]));

				}

				System.out.println(line.getOptionValue("tcpAddress"));

			}

			if (line.hasOption("output")) {
				dataReader.setOutFilename(line.getOptionValue("output"));
				System.out.println(line.getOptionValue("output"));
			}

			if (line.hasOption("serialPort")) {
				// dataReader.setSerialPortname(line.getOptionValue("serialPort"));
				System.out.println(line.getOptionValue("serialPort"));
			}

			if (line.hasOption("database")) {
				dataReader.setStoreInDB(true);
			} else {
				dataReader.setStoreInDB(false);
			}

		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		dataReaderFrontEnd = new DataReaderFrontEnd(dataReader);
		dataReader.setGUI(dataReaderFrontEnd);

		java.awt.EventQueue.invokeLater(new Runnable() {

			public void run() {

				JFrame frame = new JFrame("NRG Patagonia");
				frame.getContentPane().add(dataReaderFrontEnd.getComponent());

				WindowListener exitListener = new WindowAdapter() {

					@Override
					public void windowClosing(WindowEvent e) {
						int confirm = JOptionPane.showOptionDialog(null,
								"Are You Sure to Close Application?",
								"Exit Confirmation", JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (confirm == 0) {
							System.exit(100);
						}
					}
				};
				frame.addWindowListener(exitListener);

				// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				frame.setPreferredSize(new Dimension(1000, 800));
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);

			}
		});

		DataReaderFrontEnd.startButton.setEnabled(false);
		thread = new Thread(dataReader);
		thread.start();

		if (dataReader.getStoreInDB()) {
			SQLSelect sqlSelect = new SQLSelect();
			Thread SQLSelectThread = new Thread(sqlSelect, "SQL Select Thread");
			SQLSelectThread.start();
		}

	}
}
