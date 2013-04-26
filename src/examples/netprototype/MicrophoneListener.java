package examples.netprototype;

import java.io.IOException;
import java.util.LinkedList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class MicrophoneListener extends Thread implements DataQueue{
	private LinkedList<byte[]> output;
	private AudioInputStream in;
	private TargetDataLine lineIn;
	private boolean stopped;
	private boolean finished;
	private Object closingLock = new Object();
	
	public MicrophoneListener(){
		output = new LinkedList<byte[]>();
	}
	
	@Override
	public void run(){
		DataLine.Info infoIn = new DataLine.Info(TargetDataLine.class, Constants.FORMAT);
		try {
			lineIn = (TargetDataLine) AudioSystem.getLine(infoIn);
			lineIn.open(Constants.FORMAT, Constants.BUFFER_SAMPLE_SIZE*2);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		in = new AudioInputStream(lineIn);
		lineIn.start();
		byte[] buffer;
		while(!stopped){
			buffer = new byte[Constants.BUFFER_SAMPLE_SIZE*2];
			try {
				in.read(buffer, 0, buffer.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			push(buffer);
		}
		synchronized(closingLock){
			finished = true;
			closingLock.notify();
		}
	}
	
	private void push(byte[] data){
		synchronized(output){
			output.add(data);
			output.notifyAll();
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
		synchronized(closingLock){
			stopped = true;
			while(!finished)
				try {
					closingLock.wait(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		lineIn.stop();
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
