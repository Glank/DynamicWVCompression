package examples.netprototype;

import java.net.InetAddress;

//Reads audio data from the microphone
//compresses it
//sends the compressed data over a UDP socket
public class TestSender {
	public static void main(String[] args) throws Throwable{
		MicrophoneListener microphone = new MicrophoneListener();
		//DecibelThresholdFilter filter = new DecibelThresholdFilter(microphone, Constants.THRESHOLD);
		DynamicCompressor compressor = new DynamicCompressor(microphone);
		PacketSender sender = new PacketSender(compressor, InetAddress.getByName("127.0.0.1"), 31215);
		microphone.start();
		//filter.start();
		compressor.start();
		sender.start();
		System.out.println("Sender Running");
	}
}
