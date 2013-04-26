package wpencoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;


/*
** WvDynamicEncode.java
**
** Modifier: Ernest Kirstein
** Modified From: WvEncoder.java
**
** Distributed under the BSD Software License (see license.txt)
**
*/
public class WvDynamicEncoder
{
	//recommended value for length: Defines.INPUT_SAMPLES = 65536
    public static void encode(InputStream in, OutputStream out, AudioFormat format, long length) throws EncodingException
    {
        WavpackConfig config = new WavpackConfig();

        //configuration
        config.flags |= Defines.CONFIG_FAST_FLAG;
        config.flags |= Defines.CONFIG_HYBRID_FLAG;
        config.bitrate = 512;

        pack_file(in, out, config, format, length);
    }

    // This function packs a single file "infilename" and stores the result at
    // "outfilename". If "out2filename" is specified, then the "correction"
    // file would go there. The files are opened and closed in this function
    // and the "config" structure specifies the mode of compression.
    static void pack_file(InputStream rawin, OutputStream out,
        WavpackConfig loc_config, AudioFormat format, long total_samples) throws EncodingException
    {
        WavpackContext wpc = new WavpackContext();
        wpc.in = new java.io.DataInputStream(rawin);
        wpc.out = out;
        
        loc_config.bytes_per_sample = format.getSampleSizeInBits()/8;
        loc_config.bits_per_sample = format.getSampleSizeInBits();
        loc_config.num_channels = format.getChannels();
        loc_config.sample_rate = (int)format.getSampleRate();

        //System.out.println(total_samples);
        WavPackUtils.WavpackSetConfiguration(wpc, loc_config, total_samples);

        // pack the audio portion of the file now
        pack_audio(wpc);

        // we're now done with any WavPack blocks, so flush any remaining data
        if (WavPackUtils.WavpackFlushSamples(wpc) == 0)
        {
            System.err.println(WavPackUtils.WavpackGetErrorMessage(wpc));
            throw new EncodingException();
        }

        // At this point we're done writing to the output files. However, in some
        // situations we might have to back up and re-write the initial blocks.
        // Currently the only case is if we're ignoring length.
        /*if (WavPackUtils.WavpackGetNumSamples(wpc) != WavPackUtils.WavpackGetSampleIndex(wpc))
        {
        	System.err.println(WavPackUtils.WavpackGetNumSamples(wpc));
        	System.err.println(WavPackUtils.WavpackGetSampleIndex(wpc));
            System.err.println("couldn't read all samples, file may be corrupt!!");
            throw new EncodingException();
        }*/
    }

    // This function handles the actual audio data compression. It assumes that the
    // input file is positioned at the beginning of the audio data and that the
    // WavPack configuration has been set. This is where the conversion from RIFF
    // little-endian standard the executing processor's format is done.
    static void pack_audio(WavpackContext wpc) throws EncodingException
    {
    	//StructPrint.print(wpc);
        long samples_remaining;
        int bytes_per_sample;

        WavPackUtils.WavpackPackInit(wpc);

        bytes_per_sample = WavPackUtils.WavpackGetBytesPerSample(wpc) * WavPackUtils.WavpackGetNumChannels(wpc);

        samples_remaining = WavPackUtils.WavpackGetNumSamples(wpc);

        byte[] input_buffer = new byte[Defines.INPUT_SAMPLES * bytes_per_sample];
        long[] sample_buffer = new long[(Defines.INPUT_SAMPLES * 4 * WavPackUtils.WavpackGetNumChannels(wpc))];

        int temp = 0;

        //while (temp < 1)
        while (true)
        {
            long sample_count;
            long bytes_read = 0;
            int bytes_to_read;

            temp = temp + 1;

            if (samples_remaining > Defines.INPUT_SAMPLES)
            {
                bytes_to_read = Defines.INPUT_SAMPLES * bytes_per_sample;
            }
            else
            {
                bytes_to_read = (int) (samples_remaining * bytes_per_sample);
            }

            samples_remaining -= (bytes_to_read / bytes_per_sample);
            try {
				bytes_read = wpc.in.read(input_buffer, 0, bytes_to_read);
			} catch (IOException e) {
				throw new EncodingException();
			}
            sample_count = bytes_read / bytes_per_sample;

            if (sample_count == 0)
            {
                break;
            }

            if (sample_count > 0)
            {
                int cnt = (int) (sample_count * WavPackUtils.WavpackGetNumChannels(wpc));
                                byte[] sptr = input_buffer;
                long[] dptr = sample_buffer;
                int loopBps = 0;

                loopBps = WavPackUtils.WavpackGetBytesPerSample(wpc);

                if (loopBps == 1)
                {
                    int intermalCount = 0;

                    while (cnt > 0)
                    {
                        dptr[intermalCount] = (sptr[intermalCount] & 0xff) - 128;
                        intermalCount++;
                        cnt--;
                    }
                }
                else if (loopBps == 2)
                {
                    int dcounter = 0;
                    int scounter = 0;

                    while (cnt > 0)
                    {
                        dptr[dcounter] = (sptr[scounter] & 0xff) | (sptr[scounter + 1] << 8);
                        scounter = scounter + 2;
                        dcounter++;
                        cnt--;
                    }
                }
                else if (loopBps == 3)
                {
                    int dcounter = 0;
                    int scounter = 0;

                    while (cnt > 0)
                    {
                        dptr[dcounter] = (sptr[scounter] & 0xff) |
                            ((sptr[scounter + 1] & 0xff) << 8) | (sptr[scounter + 2] << 16);
                        scounter = scounter + 3;
                        dcounter++;
                        cnt--;
                    }
                }
            }

            wpc.byte_idx = 0; // new WAV buffer data so reset the buffer index to zero

            if (WavPackUtils.WavpackPackSamples(wpc, sample_buffer, sample_count) == 0)
            {
                System.err.println(WavPackUtils.WavpackGetErrorMessage(wpc));
                throw new EncodingException();
            }
        }

        if (WavPackUtils.WavpackFlushSamples(wpc) == 0)
        {
            System.err.println(WavPackUtils.WavpackGetErrorMessage(wpc));
            throw new EncodingException();
        }
    }
}
