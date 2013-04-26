package examples.netprototype;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class SpeakerSender extends Thread{
	private DataQueue input;
	
	public SpeakerSender(DataQueue input){
		this.input = input;
	}
	
	@Override
	public void run(){
		SourceDataLine auline = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, Constants.FORMAT);
        try {
			auline = (SourceDataLine) AudioSystem.getLine(info);
			auline.open(Constants.FORMAT, Constants.BUFFER_SAMPLE_SIZE*2);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
        auline.start();
        while(true){
        	byte[] data = input.poll();
        	auline.write(data, 0, data.length);
        }
	}
}
