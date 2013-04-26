package examples.netprototype;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PacketSender extends Thread{
	
	private DataQueue input;
	private DatagramSocket output;
	private byte[] metaPacketBuffer = new byte[19];
	private InetAddress address;
	private int port;
	private int sent;
	
	public PacketSender(DataQueue input, InetAddress address, int port){
		this.input = input;
		this.address = address;
		this.port = port;
	}
	
	@Override
	public void run(){
		try {
			output = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		DatagramPacket metaPacket = new DatagramPacket(metaPacketBuffer, metaPacketBuffer.length, address, port);
		DatagramPacket packet;
		while(true){
			byte[] data = input.poll();
			buildMetaPacket(data);
			packet = new DatagramPacket(data, data.length, address, port);
			try {
				output.send(metaPacket);
				sleep();
				output.send(packet);
				sleep();
				sent++;
				//System.out.println(++sent);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println("Sent.");
		}
	}
	
	private void sleep(){
		try {
			Thread.sleep(25);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] getMD5(byte[] data){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md.digest(data);
	}
	
	private void buildMetaPacket(byte[] data){
		byte[] md5 = getMD5(data);
		for(int i = 0; i < 16; i++){
			metaPacketBuffer[i] = md5[i];
		}
		metaPacketBuffer[16] = (byte)(data.length&255);
		metaPacketBuffer[17] = (byte)((data.length>>8)&255);
		byte cs = 0;
		for(int i = 0; i < 16; i++){
			if((data.length>>i)%2==1)
				cs+=3*i%5+7;
		}
		metaPacketBuffer[18] = cs;
	}
}
