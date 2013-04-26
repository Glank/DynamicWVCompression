package examples.netprototype;

public class Utils {
	public static float[] toFloat(byte[] buffer){
		float[] ret = MemoryPool.getFloatBuffer();
		for(int i = 0; i < Constants.BUFFER_SAMPLE_SIZE; i++){
			int s = buffer[2*i]&255;
			s += (buffer[2*i+1]&255)<<8;
			if(s>Short.MAX_VALUE)
				s = -((~(s|0xFFFF0000))+1);
			ret[i] = s/32768.0f;
		}
		return ret;
	}
	
	public static double getDecibels(float[] signal){
		double decibels = 0;
		for(int i = 0; i < signal.length; i++)
			decibels+=Math.abs(signal[i]);
		decibels/=signal.length;
		decibels = 100*Math.log(100*decibels)/Math.log(2);
		return decibels;
	}
	
	public static double getDecibels(byte[] signal){
		float[] buffer = toFloat(signal);
		double decibels = getDecibels(buffer);
		MemoryPool.returnBuffer(buffer);
		return decibels;
	}
}
