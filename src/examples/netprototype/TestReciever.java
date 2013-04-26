package examples.netprototype;

//Receives compressed audio data from a UDP socket
//then decompresses it
//then plays it over the speakers
public class TestReciever {
	public static void main(String[] args){
		PacketReciever reciever = new PacketReciever(31215);
		DynamicDecompressor decompressor = new DynamicDecompressor(reciever);
		SpeakerSender sender = new SpeakerSender(decompressor);
		reciever.start();
		decompressor.start();
		sender.start();
		System.out.println("Reciever Running");
	}
}
