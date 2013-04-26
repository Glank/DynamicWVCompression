package examples.netprototype;

import java.util.LinkedList;

public class MemoryPool {
	private static LinkedList<byte[]> byteBuffers = new LinkedList<byte[]>();
	private static LinkedList<float[]> floatBuffers = new LinkedList<float[]>();
	
	public static byte[] getByteBuffer(){
		byte[] ret = null;
		synchronized(byteBuffers){
			if(!byteBuffers.isEmpty())
				ret = byteBuffers.poll();
		}
		if(ret==null)
			ret = new byte[Constants.BUFFER_SAMPLE_SIZE*2];
		return ret;
	}
	
	public static float[] getFloatBuffer(){
		float[] ret = null;
		synchronized(floatBuffers){
			if(!floatBuffers.isEmpty())
				ret = floatBuffers.poll();
		}
		if(ret==null)
			ret = new float[Constants.BUFFER_SAMPLE_SIZE];
		return ret;
	}
	
	public static void returnBuffer(byte[] ret){
		synchronized(byteBuffers){
			byteBuffers.add(ret);
		}
	}
	
	public static void returnBuffer(float[] ret){
		synchronized(floatBuffers){
			floatBuffers.add(ret);
		}
	}
}
