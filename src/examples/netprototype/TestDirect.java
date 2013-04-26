package examples.netprototype;

//A basic test the reads data from the microphone and plays it over the 
//speakers.
public class TestDirect {
	public static void main(String[] args){
		MicrophoneListener microphone = new MicrophoneListener();
		SpeakerSender sender = new SpeakerSender(microphone);
		sender.start();
		microphone.start();
	}
}
