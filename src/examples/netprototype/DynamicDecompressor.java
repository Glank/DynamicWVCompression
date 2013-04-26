package examples.netprototype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import wpdecoder.WvDynamicDecoder;

public class DynamicDecompressor extends Thread implements DataQueue{
	private DataQueue input;
	private LinkedList<byte[]> output;
	
	public DynamicDecompressor(DataQueue input){
		this.input = input;
		output = new LinkedList<byte[]>();
	}
	
	@Override
	public void run(){
		ByteArrayInputStream bais;
		ByteArrayOutputStream baos;
		while(true){
			bais = new ByteArrayInputStream(input.poll());
			baos = new ByteArrayOutputStream();
			WvDynamicDecoder.decode(bais, baos);
			byte[] data = baos.toByteArray();
			push(data);
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
}
