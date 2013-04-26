package examples.netprototype;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public class PacketReciever extends Thread implements DataQueue{
	private DatagramSocket input;
	private LinkedList<byte[]> output;
	private byte[] metaPacketBuffer = new byte[19];
	private int port;
	private int recieved;
	
	public PacketReciever(int port){
		this.port = port;
		output = new LinkedList<byte[]>();
	}
	
	@Override
	public void run(){
		try {
			input = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		DatagramPacket metaPacket = new DatagramPacket(metaPacketBuffer, metaPacketBuffer.length);
		DatagramPacket packet;
		while(true){
			try {
				input.receive(metaPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Integer length = getLength();
			if(length==null)
				throw new RuntimeException("INVALID STREAM");
			else{
				byte[] data = new byte[length];
				packet = new DatagramPacket(data, length);
				try {
					input.receive(packet);
					recieved++;
					//System.out.println(recieved);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(verify(data))
					push(data);
				else
					throw new RuntimeException("INVALID STREAM");
			}
		}
	}
	
	private void push(byte[] data){
		synchronized(output){
			output.add(data);
			output.notify();
		}
	}
	
	public byte[] poll(){
		synchronized(output){
			while(output.isEmpty())
				try {
					output.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			return output.poll();
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
	
	private boolean verify(byte[] data){
		byte[] md5 = getMD5(data);
		for(byte i = 0; i < 16; i++)
			if(metaPacketBuffer[i]!=md5[i])
				return false;
		return true;
	}
	
	private Integer getLength(){
		int length = metaPacketBuffer[16]&255;
		length+= (metaPacketBuffer[17]&255)<<8;
		byte cs = 0;
		for(int i = 0; i < 16; i++){
			if((length>>i)%2==1)
				cs+=3*i%5+7;
		}
		if(cs!=metaPacketBuffer[18])
			return null;
		return length;
	}
}
