package examples.netprototype;

import java.util.LinkedList;

public class DecibelThresholdFilter extends Thread implements DataQueue{
	private LinkedList<byte[]> signals = new LinkedList<byte[]>();
	private byte[] pre = null;
	private int silence = 0;
	private double threshold;
	private boolean stopped;
	private MicrophoneListener input;
	
	public DecibelThresholdFilter(MicrophoneListener input, double threshold){
		this.threshold = threshold;
		this.input = input;
	}
	
	@Override
	public void run(){
		byte[] buffer;
		while(!stopped){
			buffer = input.poll();
			double decibels = Utils.getDecibels(buffer);
			System.out.println(decibels);
			if(decibels>threshold){
				silence = 0;
				if(pre!=null){
					push(pre);
					pre = null;
				}
				push(buffer);
			}
			else if(silence<5){
				silence++;
				push(buffer);
			}
			else{
				if(pre!=null)
					MemoryPool.returnBuffer(pre);
				pre = buffer;
			}
		}
	}
	
	private void push(byte[] buffer){
		synchronized(signals){
			signals.add(buffer);
			signals.notify();
		}
	}
	
	public byte[] poll(){
		byte[] ret;
		synchronized(signals){
			while(signals.isEmpty())
				try {
					signals.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			ret = signals.poll();
		}
		return ret;
	}
}
