package examples.netprototype;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import wpencoder.WvDynamicEncoder2;

public class DynamicCompressor extends Thread implements DataQueue{
	private DataQueue input;
	private LinkedList<byte[]> output; //output packets
	private boolean stopped;
	
	public DynamicCompressor(DataQueue input){
		this.input = input;
		output = new LinkedList<byte[]>();
	}
	
	@Override
	public void run(){
		ByteArrayInputStream bais;
		ByteArrayOutputStream baos;
		while(!stopped){
			byte[] in = input.poll();
			bais = new ByteArrayInputStream(in);
			baos = new ByteArrayOutputStream();
			try {
				WvDynamicEncoder2.encode(bais, baos, Constants.FORMAT, in.length/2);
			} catch (IOException e) {
				e.printStackTrace();
			}
			MemoryPool.returnBuffer(in);
			push(baos.toByteArray());
		}
	}
	
	private void push(byte[] array) {
		synchronized(output){
			output.add(array);
			//System.out.println(array.length/(Constants.BUFFER_SAMPLE_SIZE*2.0f));
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

	public void close(){
		stopped = true;
	}
}
