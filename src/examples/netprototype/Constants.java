package examples.netprototype;

import javax.sound.sampled.AudioFormat;

public class Constants {
	public static final int SAMPLE_RATE = 44100; //Hz
	public static final int BUFFER_SAMPLE_SIZE = 1<<12;
	public static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
	public static final double THRESHOLD = 60;
}
