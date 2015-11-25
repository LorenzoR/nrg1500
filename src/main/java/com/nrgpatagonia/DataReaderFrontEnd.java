package com.nrgpatagonia;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class DataReaderFrontEnd {

	private static DataReader dataReader;

	private JTable table = new JTable();
	private JPanel mainPanel = new JPanel(new BorderLayout());

	private static DataReaderFrontEnd dataReaderFrontEnd;
	private CustomTableModel customTableModel;

	static int rowCounter = 0;

	public static JButton startButton;
	public static JButton stopButton;
	public static JButton restartButton;
	
	private static boolean useSerial = false;

	public DataReaderFrontEnd(final DataReader dataReader) {
		startButton = new JButton("Start");
		stopButton = new JButton("Stop");
		restartButton = new JButton("Restart");
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(startButton);
		startButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				startDataReader();
			}
		});

		buttonPanel.add(stopButton);
		stopButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				//stopDataReader();
				thread.interrupt();
			}
		});

		buttonPanel.add(restartButton);
		restartButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				stopDataReader();
				startDataReader();
			}
		});

		table = new JTable() {

			private static final long serialVersionUID = 1L;
			/*
			 * @Override public Component prepareRenderer(TableCellRenderer
			 * renderer, int row, int column) { Component c =
			 * super.prepareRenderer(renderer, row, column); if
			 * (isRowSelected(row) && isColumnSelected(column)) { ((JComponent)
			 * c).setBorder(new LineBorder(Color.red)); }
			 * 
			 * getTableHeader().setPreferredSize(new Dimension(10, 50));
			 * 
			 * return c; }
			 */
		};

		customTableModel = new CustomTableModel();

		table.setModel(customTableModel);

		CustomHeaderRenderer customHeaderRenderer = new CustomHeaderRenderer();

		Enumeration<TableColumn> e = table.getColumnModel().getColumns();
		while (e.hasMoreElements()) {
			((TableColumn) e.nextElement())
					.setHeaderRenderer(customHeaderRenderer);
		}

		table.getColumnModel().getColumn(0).setMinWidth(130);

		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
	}

	private static Thread thread;
	private static AtomicBoolean dataReaderRunning = new AtomicBoolean();

	private static void startDataReader() {
		dataReader.setDone(false);
		startButton.setEnabled(false);
		stopButton.setEnabled(true);
		restartButton.setEnabled(true);
		thread = new Thread(dataReader);
		thread.start();
		dataReaderRunning.set(true);
	}

	private static void stopDataReader() {
		// dataReader.interrupt();
		thread.interrupt();
		dataReader.setDone(true);
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		restartButton.setEnabled(false);
		dataReaderRunning.set(false);
	}

	public void addData(Object[] row) {
		// model.addRow(row);
		customTableModel.addRow(row);
		table.getSelectionModel().setSelectionInterval(rowCounter, rowCounter);
		table.scrollRectToVisible(new Rectangle(table.getCellRect(rowCounter,
				0, true)));
		rowCounter++;

		if (rowCounter > 500) {
			// model.removeRow(0);
			customTableModel.removeRow(0);
			rowCounter--;
		}
	}

	public JComponent getComponent() {
		return mainPanel;
	}

	private static ModbusConnections modbusConnections;

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
				.withDescription("ruta de los datos").create("output");

		options.addOption(tcpPortOption);
		options.addOption(serialPortOption);
		options.addOption(outputFileOption);

		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("dataReader", options);

		modbusConnections = new ModbusConnections();

		System.out.println("Al principio TCP CON: "
				+ modbusConnections.getTcpCon().isConnected());
		if (useSerial = false) {
			System.out.println("al princicpio Serial Conn: "
					+ modbusConnections.getSerialCon().isOpen());
		}

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

			if (line.hasOption("dataBase")) {
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
		thread = new Thread(dataReader, "DataReader Thread");
		thread.start();

		if (dataReader.getStoreInDB()) {
			SQLSelect sqlSelect = new SQLSelect();
			Thread SQLSelectThread = new Thread(sqlSelect, "SQL Select Thread");
			SQLSelectThread.start();
		}
	}

	static class CustomHeaderRenderer extends JTextPane implements
			TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public CustomHeaderRenderer() {
			setOpaque(true);
			setForeground(Color.LIGHT_GRAY);
			setBackground(Color.LIGHT_GRAY);
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			StyledDocument doc = this.getStyledDocument();
			MutableAttributeSet standard = new SimpleAttributeSet();
			StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
			StyleConstants.setFontFamily(standard, "Arial");
			doc.setParagraphAttributes(0, 0, standard, true);
		}

		public Component getTableCellRendererComponent(JTable jTable,
				Object obj, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setText((String) obj);
			return this;
		}
	}

	static class CustomTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		public CustomTableModel() {

			Object[] columnNames = { "Timestamp", "RPM del\nRotor",
					"Angulo\nYAW", "Angulo\nPitch\nPala 1",
					"Angulo\nPitch\nPala 2", "Angulo\nPitch\nPala 3",
					"Potencia\nActiva\n[kW]", "Potencia\nReactiva\n[kW]",
					"Anemometro\n72m SSW [m/s]", "Anemometro\n38m SSW\n[m/s]",
					"Barometro\n69m", "Veleta\n69m", "Veleta\n38m",
					"Temperatura\n69m", "Palabra\nde\nEstado" };

			this.setDataVector(null, columnNames);

		}

	}

	static class SQLSelect implements Runnable {

		private Connection connect;
		private PreparedStatement statement;
		private final long keepAliveTime = 10;

		public SQLSelect() {
			try {
				Class.forName("com.mysql.jdbc.Driver");

				// Setup the connection with the DB
				connect = DriverManager
						.getConnection("jdbc:mysql://localhost/molino?"
								+ "user=root&password=nrg1500");

				String query = "SELECT MAX(timestamp) AS timestamp FROM MOLINO";

				statement = connect.prepareStatement(query);
				statement.setMaxRows(1);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void run() {

			while (true) {
				try {

					Thread.sleep(keepAliveTime * 1000);

					ResultSet rs = statement.executeQuery();
					if (rs.next()) {
						Long timestamp = rs.getLong("timestamp");
						// System.out.println("TIMESTAMP: " + timestamp);

						/*
						 * System.out.println("LAST TIME: " + timestamp);
						 * System.out.println("CURRENT TIME: " +
						 * System.currentTimeMillis() / 1000); System.out
						 * .println("CURRENT - LAST: " +
						 * ((System.currentTimeMillis() / 1000) - timestamp));
						 */
						// http://docs.oracle.com/javase/tutorial/uiswing/concurrency/worker.html
						if ((System.currentTimeMillis() / 1000) - timestamp > keepAliveTime) {
							System.out.println("DEBUG");
							System.out
									.println("************* durante, TCP CON: "
											+ modbusConnections.getTcpCon()
													.isConnected());
							System.out
									.println("durante, Serial Conn: "
											+ modbusConnections.getSerialCon()
													.isOpen());

							thread.interrupt();
							// System.exit(1);
							// while (testFlag && !dataReader.done.get()) {
							// }
							System.out.println("Duermo y abro de nuevo");

							Thread.sleep(4000);
							System.out.println("Pongo false en done");
							dataReader.done.set(false);
							System.out.println("Reconecto serial");
							modbusConnections.reconnectSerial();
							System.out.println("Reconecto tcp");
							modbusConnections.reconnectTCP();
							System.out.println("new thread");
							thread = new Thread(dataReader,
									"DataReader New Thread");
							thread.start();
						}

					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
	}

}