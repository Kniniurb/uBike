package com.plusot.common.ntp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.Date;

import com.plusot.common.Globals;
import com.plusot.common.share.LLog;

public class NtpLookup {
	private static final String CLASSTAG = NtpLookup.class.getSimpleName();
	

	private static final String[] ntpServers= {
		//"ntp.championchip.com",
		"pool.ntp.org",
		"europe.pool.ntp.org",
		"ntp2.nl.net",
		"0.pool.ntp.org",
		"1.pool.ntp.org",
		"2.pool.ntp.org",
		"3.pool.ntp.org"
		//"ntp.csr.net",
		//"time-b.nist.gov"
	};
	private static int timeServerIndex = -1; 
	
	private static String getServer(){
		if(timeServerIndex < 0 || timeServerIndex > (ntpServers.length -1))
			timeServerIndex = (int)Math.floor(Math.abs(Math.random() * Float.valueOf(ntpServers.length) - 0.01));
		
		return ntpServers[timeServerIndex];
	}
	
	private static void selectNextServer(){
		timeServerIndex++;
		if(timeServerIndex >= ntpServers.length) timeServerIndex = 0;	
	}
	
	public static class NtpResult {
		final String info;
		final double clockOffset;
		public NtpResult(final double clockOffset, final String info) {
			this.info = info;
			this.clockOffset = clockOffset;
		}
	}
	
	public static NtpResult getInfo(String serverName) throws IOException {
		if (serverName == null)
			serverName = getServer();
		
		// Send request
		DatagramSocket socket = new DatagramSocket();
		InetAddress address = InetAddress.getByName(serverName);
		byte[] buf = new NtpMessage().toByteArray();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 123);

		// Set the transmit timestamp *just* before sending the packet
		// ToDo: Does this actually improve performance or not?
		NtpMessage.encodeTimestamp(packet.getData(), 40,
				(System.currentTimeMillis()/1000.0) + 2208988800.0);

		socket.send(packet);

		// Get response
		LLog.d(Globals.TAG, CLASSTAG + ".getInfo: NTP request sent, waiting for response...");
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		// Immediately record the incoming timestamp
		double destinationTimestamp =
			(System.currentTimeMillis()/1000.0) + 2208988800.0;

		// Process response
		NtpMessage msg = new NtpMessage(packet.getData());

		// Corrected, according to RFC2030 errata
		double roundTripDelay = (destinationTimestamp-msg.originateTimestamp) -
			(msg.transmitTimestamp-msg.receiveTimestamp);

		double localClockOffset =
			((msg.receiveTimestamp - msg.originateTimestamp) +
			(msg.transmitTimestamp - destinationTimestamp)) / 2;

		// Display response
		StringBuffer sb = new StringBuffer();
		sb.append("NTP server: ").append(serverName).append('\n');
		sb.append(msg.toString()); //.replace("\n", "</br>"));
		sb.append("Dest. timestamp: ").append(NtpMessage.timestampToString(destinationTimestamp)).append('\n');
		sb.append("Round-trip: ").append(new DecimalFormat("0.00").format(roundTripDelay)).append(" s\n");
		sb.append("Clock offset: ").append(new DecimalFormat("0.00").format(localClockOffset)).append(" s\n");
		socket.close();
		return new NtpResult(localClockOffset, sb.toString());
	}
	
	public static NtpResult getInfo() throws IOException {
		try {
			return getInfo(null);
		} finally {
			timeServerIndex = -1;
		}
	}
		
	
	public static Date getDate(String serverName) throws IOException, NtpException{
		if (serverName == null)
			serverName = getServer();
		
		//System.out.println("ntp Server " + timeServerIndex + ": " + serverName);
		
		// Send request
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(5000);
		
		InetAddress address = InetAddress.getByName(serverName);
		byte[] buf = new NtpMessage().toByteArray();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 123);

		// Set the transmit timestamp *just* before sending the packet
		// ToDo: Does this actually improve performance or not?
		double timeStamp = (System.currentTimeMillis()/1000.0) + 2208988800.0;
		NtpMessage.encodeTimestamp(packet.getData(), 40, timeStamp);
		
		socket.send(packet);

		// Get response
		//System.out.println("NTP request sent, waiting for response...\n");
		packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);

		// Immediately record the incoming timestamp
		double destinationTimestamp = (System.currentTimeMillis()/1000.0) + 2208988800.0;

		// Process response
		NtpMessage msg = new NtpMessage(packet.getData());

		socket.close();
		
		if(msg.receiveTimestamp == 0)
			throw new NtpException("unreliable time on " + serverName);
		
		return NtpMessage.timestampToDate(
				destinationTimestamp +
				((msg.receiveTimestamp - msg.originateTimestamp) +
				(msg.transmitTimestamp - destinationTimestamp)) / 2);
	}
	
	public static Date getDate() throws IOException {
		int retries = ntpServers.length + 1;
		while(retries > 0){
			try{
				return getDate(null);
			} catch (SocketTimeoutException e) {
				selectNextServer();
				retries--;
			} catch (NtpException e) {
				LLog.e(Globals.TAG, CLASSTAG +".getDate: " + e.getMessage());
				selectNextServer();
				retries--;
			} catch (IOException e) {
				selectNextServer();
				timeServerIndex = -1; 
				throw e;
			}	
		}
		
		timeServerIndex = -1;
		throw new IOException("Unable to connect to anny of the NTP-servers");
	}
}
