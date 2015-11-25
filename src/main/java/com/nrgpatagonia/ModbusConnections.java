package com.nrgpatagonia;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.util.SerialParameters;

public class ModbusConnections {

	// TCP
	private TCPMasterConnection tcpCon = null; // the connection
	private ModbusTCPTransaction tcpTrans = null; // the transaction
	private ReadInputRegistersRequest tcpReq = null; // the request

	private int tcpRef = 0; // the reference; offset where to start reading from
	private int tcpCount = 18; // the number of DI's to read

	// SERIAL
	private SerialConnection serialCon = null; // the connection
	private ModbusSerialTransaction serialTrans = null; // the transaction
	private ReadMultipleRegistersRequest serialReq = null; // the request

	private int serialUnitid = 1; // the unit identifier we will be talking to
	private int serialRef = 0; // the reference, where to start reading from
	private int serialCount = 40; // the count of IR's to read

	private SerialParameters params = new SerialParameters();

	private static InetAddress tcpAddr;
	private static int tcpPort;
	private static String serialPortname;

	private boolean useSerial = false;

	public ModbusConnections() {

		/* Variables for storing the parameters */
		try {
			tcpAddr = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(2);
		}

		tcpPort = Modbus.DEFAULT_PORT;

		/* Variables for storing the parameters */
		serialPortname = "COM4"; // the name of the serial port to be used

		// TCP
		// 2. Open the connection
		tcpCon = new TCPMasterConnection(tcpAddr);
		tcpCon.setPort(tcpPort);

		// SERIAL
		// 2. Set master identifier
		ModbusCoupler.getReference().setUnitID(1);

		// 3. Setup serial parameters
		params.setPortName(serialPortname);
		params.setBaudRate(19200);
		params.setDatabits(8);
		params.setParity("None");
		params.setStopbits(1);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);

		try {
			// TCP
			// 2. Open the connection
			tcpCon.connect();
			// 3. Prepare the request
			// req = new ReadInputDiscretesRequest(ref, count);
			tcpReq = new ReadInputRegistersRequest(tcpRef, tcpCount);
			// 4. Prepare the transaction
			tcpTrans = new ModbusTCPTransaction(tcpCon);
			tcpTrans.setRequest(tcpReq);
			tcpTrans.setReconnecting(false);

			// SERIAL
			// 4. Open the connection
			if (useSerial) {
				serialCon = new SerialConnection(params);
				serialCon.open();
				serialCon.close();
				Thread.sleep(1000);
				serialCon.open();

				// 5. Prepare a request
				serialReq = new ReadMultipleRegistersRequest(serialRef,
						serialCount);
				serialReq.setUnitID(serialUnitid);
				serialReq.setHeadless();

				// 6. Prepare a transaction
				serialTrans = new ModbusSerialTransaction(serialCon);
				serialTrans.setRequest(serialReq);
			}
		} catch (Exception ex) {

		}
	}

	public ModbusTCPTransaction getTcpTrans() {
		return tcpTrans;
	}

	public ModbusSerialTransaction getSerialTrans() {
		return serialTrans;
	}

	public TCPMasterConnection getTcpCon() {
		return tcpCon;
	}

	public SerialConnection getSerialCon() {
		return serialCon;
	}

	public void reconnectTCP() {
		if (!tcpCon.isConnected()) {
			try {
				tcpCon = new TCPMasterConnection(tcpAddr);
				tcpCon.setPort(tcpPort);
				// TCP
				// 2. Open the connection
				tcpCon.close();
				tcpCon.connect();
				// 3. Prepare the request
				// req = new ReadInputDiscretesRequest(ref, count);
				tcpReq = new ReadInputRegistersRequest(tcpRef, tcpCount);
				// 4. Prepare the transaction
				tcpTrans = new ModbusTCPTransaction(tcpCon);
				tcpTrans.setRequest(tcpReq);
				tcpTrans.setReconnecting(false);
			} catch (Exception ex) {

			}
		} else {
			System.out.println("TCP NO ESTA DESCONECTADO");
		}
	}

	public void reconnectSerial() {
		if (!serialCon.isOpen()) {
			try {
				// SERIAL
				// 4. Open the connection
				serialCon.close();
				serialCon = new SerialConnection(params);
				serialCon.open();
				serialCon.close();
				Thread.sleep(1000);
				serialCon.open();

				// 5. Prepare a request
				serialReq = new ReadMultipleRegistersRequest(serialRef,
						serialCount);
				serialReq.setUnitID(serialUnitid);
				serialReq.setHeadless();

				// 6. Prepare a transaction
				serialTrans = new ModbusSerialTransaction(serialCon);
				serialTrans.setRequest(serialReq);
			} catch (Exception ex) {

			}
		} else {
			System.out.println("SERIAL NO ESTA DESCONECTADO");
		}
	}

}