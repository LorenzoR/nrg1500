package com.nrgpatagonia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.util.SerialParameters;

public class DataReader implements Runnable {

	private DataReaderFrontEnd defaultTableModelDemo;

	private boolean storeInDB;
	private String outFilename;
	private long timeStep;

	private ModbusTCPTransaction tcpTrans;
	private ModbusSerialTransaction serialTrans;

	public AtomicBoolean done = new AtomicBoolean(false);

	public void setDone(boolean done) {
		this.done.set(done);
	}

	public boolean getDone() {
		return this.done.get();
	}

	// SQL
	private static Connection connect = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;

	boolean readSerial = false;

	public DataReader(ModbusTCPTransaction tcpTrans,
			ModbusSerialTransaction serialTrans) {

		this.tcpTrans = tcpTrans;
		this.serialTrans = serialTrans;
		storeInDB = true;

		/*
		 * if (args != null && args.length > 0) { String addressAndPort[] =
		 * args[0].split(":");
		 * 
		 * if (addressAndPort.length != 2) { System.out
		 * .println("Error. El primer argumento debe ser direccion:puerto.");
		 * System.exit(1); } else { try { tcpAddr =
		 * InetAddress.getByName(addressAndPort[0]); } catch
		 * (UnknownHostException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); System.exit(2); } tcpPort =
		 * Integer.valueOf(addressAndPort[1]); }
		 * 
		 * if (!(args[1].substring(0, 3).equalsIgnoreCase("com"))) { System.out
		 * .println("Error. El segundo argumento debe ser COMx.");
		 * System.exit(1); } else { serialPortname = args[1]; }
		 * 
		 * if (args.length > 2) { if (Integer.valueOf(args[2]) == 1) { storeInDB
		 * = true; } else if (Integer.valueOf(args[2]) == 0) { storeInDB =
		 * false; } }
		 * 
		 * }
		 */
		timeStep = 1 * 1000000000L;
		// outFilename =
		// "C:\\Documents and Settings\\USUARIO\\Escritorio\\output.txt";
		outFilename = "C:\\Documents and Settings\\nrgpatagonia\\Escritorio\\output.txt";

	}

	public boolean isStoreInDB() {
		return storeInDB;
	}

	public void setStoreInDB(boolean storeInDB) {
		this.storeInDB = storeInDB;
	}

	public boolean getStoreInDB() {
		return storeInDB;
	}

	public String getOutFilename() {
		return outFilename;
	}

	public void setOutFilename(String outFilename) {
		this.outFilename = outFilename;
	}

	public void setGUI(DataReaderFrontEnd defaultTableModelDemo) {
		this.defaultTableModelDemo = defaultTableModelDemo;
	}

	public void run() {

		System.out.println("Running...");

		ReadInputRegistersResponse tcpRes = null; // the response
		ReadMultipleRegistersResponse serialRes = null; // the response

		// TCP Variables
		short rotorRPMLow = 0;
		short rotorRPMHigh = 0;
		float nacellePos = 0;
		short actualOperateStateNumeric = 0;
		float rotorRPM = 0;
		float bladeAngle1 = 0;
		float bladeAngle2 = 0;
		float bladeAngle3 = 0;

		// Serial variables
		float anemometer38 = 0;
		float anemometer72 = 0;
		float thermistor = 0;
		float activePower = 0;
		float reactivePower = 0;
		float windVane69 = 0;
		float windVane38 = 0;
		float barometer = 0;

		long startTime = 0;

		String newLine;

		long lastTimestamp = 0;
		long currentTimestamp = 0;

		PrintWriter monthlyOutput = null;
		PrintWriter dailyOutput = null;

		String outputPath = "C:\\Documents and Settings\\nrgpatagonia\\Escritorio\\datos\\";

		try {

			// Logger
			// out = new PrintWriter(new BufferedWriter(new FileWriter(
			// outFilename, true)));

			newLine = "Timestamp" + ";RPM Rotor" + ";Angulo YAW"
					+ ";Angulo Pitch Pala 1" + ";Angulo Pitch Pala 2"
					+ ";Angulo Pitch Pala 3" + ";Potencia Activa"
					+ ";Potencia Reactiva" + ";Anemometro 72m"
					+ ";Anemometro 38m" + ";Barometro 69m" + ";Veleta 69m"
					+ ";Veleta 38m" + ";Temperatura 69m" + ";Palabra de Estado";

			// if (!new File(outFilename).exists()) {
			// out.println(newLine);
			// }
			System.out.println(newLine);

			// This will load the MySQL driver, each DB has its own
			// driver
			if (storeInDB) {
				Class.forName("com.mysql.jdbc.Driver");
				// Setup the connection with the DB
				connect = DriverManager
						.getConnection("jdbc:mysql://localhost/molino?"
								+ "user=root&password=nrg1500");

				// Statements allow to issue SQL queries to the database
				statement = connect.createStatement();

				// PreparedStatements can use variables and are more
				// efficient
				preparedStatement = connect
						.prepareStatement("insert ignore into MOLINO.MOLINO values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			}

		} catch (Exception ex) {

		}

		// 5. Execute the transaction repeat times
		do {
			try {

				currentTimestamp = System.currentTimeMillis();
				startTime = System.nanoTime();

				String monthlyFilename = outputPath
						+ new SimpleDateFormat("MM-yyyy")
								.format(currentTimestamp) + ".txt";

				String monthlyPath = outputPath
						+ new SimpleDateFormat("MM-yyyy")
								.format(currentTimestamp) + "\\";

				String dailyFilename = new SimpleDateFormat("dd-MM-yyyy")
						.format(currentTimestamp) + ".txt";

				if (!new File(monthlyPath).exists()) {
					new File(monthlyPath).mkdirs();
				}

				// Logger
				monthlyOutput = new PrintWriter(new BufferedWriter(
						new FileWriter(monthlyFilename, true)));

				dailyOutput = new PrintWriter(new BufferedWriter(
						new FileWriter(monthlyPath + dailyFilename, true)));

				newLine = "Timestamp" + ";RPM Rotor" + ";Angulo YAW"
						+ ";Angulo Pitch Pala 1" + ";Angulo Pitch Pala 2"
						+ ";Angulo Pitch Pala 3" + ";Potencia Activa"
						+ ";Potencia Reactiva" + ";Anemometro 72m"
						+ ";Anemometro 38m" + ";Barometro 69m" + ";Veleta 69m"
						+ ";Veleta 38m" + ";Temperatura 69m"
						+ ";Palabra de Estado";

				if (!new File(monthlyFilename).exists()) {
					monthlyOutput.println(newLine);
				}

				// System.out.println("CICLO");

				tcpTrans.execute();

				tcpRes = (ReadInputRegistersResponse) tcpTrans.getResponse();

				if (tcpRes.getByteCount() > 0) {

					rotorRPMLow = (short) tcpRes.getRegisterValue(0);
					rotorRPMHigh = (short) tcpRes.getRegisterValue(1);
					nacellePos = (float) tcpRes.getRegisterValue(2) / 10;
					actualOperateStateNumeric = (short) tcpRes
							.getRegisterValue(9);
					rotorRPM = ((rotorRPMHigh << 16) | rotorRPMLow) / 1000;
					bladeAngle1 = getBladeAngle(tcpRes, 3, 4);
					bladeAngle2 = getBladeAngle(tcpRes, 5, 6);
					bladeAngle3 = getBladeAngle(tcpRes, 7, 8);
				}

				serialTrans.execute();

				serialRes = (ReadMultipleRegistersResponse) serialTrans
						.getResponse();

				if (serialRes.getByteCount() > 0) {
					anemometer72 = joinRegisters(serialRes, 2, 3);
					anemometer38 = joinRegisters(serialRes, 4, 5);
					thermistor = joinRegisters(serialRes, 26, 27);
					windVane38 = joinRegisters(serialRes, 34, 35);
					windVane69 = joinRegisters(serialRes, 36, 37);
					barometer = joinRegisters(serialRes, 38, 39);
					activePower = joinRegisters(serialRes, 30, 31);
					reactivePower = joinRegisters(serialRes, 32, 33);
				}

				newLine = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
						.format(currentTimestamp)
						+ ";"
						+ rotorRPM
						+ ";"
						+ nacellePos
						+ ";"
						+ bladeAngle1
						+ ";"
						+ bladeAngle2
						+ ";"
						+ bladeAngle3
						+ ";"
						+ activePower
						+ ";"
						+ reactivePower
						+ ";"
						+ anemometer72
						+ ";"
						+ anemometer38
						+ ";"
						+ barometer
						+ ";"
						+ windVane69
						+ ";"
						+ windVane38
						+ ";"
						+ thermistor
						+ ";"
						+ actualOperateStateNumeric + ";";

				Object[] newRow = {
						new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
								.format(currentTimestamp), rotorRPM,
						nacellePos, bladeAngle1, bladeAngle2, bladeAngle3,
						activePower, reactivePower, anemometer72, anemometer38,
						barometer, windVane69, windVane38, thermistor,
						actualOperateStateNumeric };

				defaultTableModelDemo.addData(newRow);

				if (lastTimestamp != currentTimestamp) {
					// System.out.println(newLine);
					monthlyOutput.println(newLine);
					monthlyOutput.flush();

					dailyOutput.println(newLine);
					dailyOutput.flush();
				}

				if (storeInDB) {

					preparedStatement.setLong(1, currentTimestamp / 1000);
					preparedStatement.setFloat(2, rotorRPM);
					preparedStatement.setFloat(3, nacellePos);
					preparedStatement.setFloat(4, bladeAngle1);
					preparedStatement.setFloat(5, bladeAngle2);
					preparedStatement.setFloat(6, bladeAngle3);
					preparedStatement.setFloat(7, activePower);
					preparedStatement.setFloat(8, reactivePower);
					preparedStatement.setFloat(9, anemometer38);
					preparedStatement.setFloat(10, anemometer72);
					preparedStatement.setFloat(11, barometer);
					preparedStatement.setFloat(12, windVane38);
					preparedStatement.setFloat(13, windVane69);
					preparedStatement.setFloat(14, thermistor);
					preparedStatement.setShort(15, actualOperateStateNumeric);
					preparedStatement.executeUpdate();
				}

				if (System.nanoTime() - startTime < timeStep) {
					Thread.sleep((timeStep - (System.nanoTime() - startTime)) / 1000000);
				}
				lastTimestamp = currentTimestamp;

				if (monthlyOutput != null) {
					monthlyOutput.close();
				}

				if (dailyOutput != null) {
					dailyOutput.close();
				}

				/*
				 * if (currentTimestamp - timeLastWrite > timeStep * 20) {
				 * 
				 * tcpCon.close(); serialCon.close();
				 * 
				 * serialCon.open(); serialCon.close(); Thread.sleep(5000);
				 * serialCon.open();
				 * 
				 * tcpCon.connect();
				 * 
				 * System.out.println("Current : " + new
				 * SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
				 * .format(currentTimestamp));
				 * System.out.println("TimeLastWrite: " + new
				 * SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
				 * .format(timeLastWrite)); System.out.println(currentTimestamp
				 * - timeLastWrite + " > " + timeStep * 20); }
				 */

			} catch (InterruptedException interruptedException) {
				System.out.println("**** me interrumpieron");
				done.set(true);

			} catch (Exception ex) {
				// ex.printStackTrace();
				// System.out.println("*****Otro Error");
				/*
				 * if (System.nanoTime() - startTime < timeStep) { try {
				 * Thread.sleep((timeStep - (System.nanoTime() - startTime)) /
				 * 1000000); } catch (InterruptedException e) { } lastTimestamp
				 * = currentTimestamp; }
				 */

			}

		} while (done.get() != true);

		System.out.println("CIERRRRRRO TODOOOO");
		// 6. Close the connection
		// tcpCon.close();
		// serialCon.close();
		// if (out != null) {
		// out.close();
		// }

		if (monthlyOutput != null) {
			monthlyOutput.close();
		}

		if (dailyOutput != null) {
			dailyOutput.close();
		}

		try {
			if (statement != null) {
				statement.close();
			}

			if (connect != null) {
				connect.close();
			}
		} catch (Exception ex) {

		}
	}// main

	private float joinRegisters(ReadMultipleRegistersResponse serialRes,
			int lowReg, int highReg) {
		int lowValue = (((serialRes.getRegisterValue(lowReg) >> 8) & 0xff) | ((serialRes
				.getRegisterValue(lowReg) << 8) & 0xff00));
		int highValue = (((serialRes.getRegisterValue(highReg) >> 8) & 0xff) | ((serialRes
				.getRegisterValue(highReg) << 8) & 0xff00)) << 16;
		return Float.intBitsToFloat(((int) highValue | lowValue));
	}

	private float getBladeAngle(ReadInputRegistersResponse tcpRes, int lowReg,
			int highReg) {
		short low = (short) tcpRes.getRegisterValue(lowReg);
		short high = (short) tcpRes.getRegisterValue(highReg);

		return ((high << 16) | low) / 100;
	}

}