package wpencoder;
/*
** WavPackUtils.java
**
** Copyright (c) 2008 - 2009 Peter McQuillan
**
** All Rights Reserved.
**
** Distributed under the BSD Software License (see license.txt)
**
*/
class WavPackUtils
{
    ///////////////////////////// local table storage ////////////////////////////
    static long[] sample_rates = 
        {
            6000, 8000, 9600, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200,
            96000, 192000
        };

    // This function returns a pointer to a string describing the last error
    // generated by WavPack.
    static String WavpackGetErrorMessage(WavpackContext wpc)
    {
        return wpc.error_message;
    }

    // Set configuration for writing WavPack files. This must be done before
    // sending any actual samples. The "config" structure contains the following
    // required information:
    // config.bytes_per_sample     see WavpackGetBytesPerSample() for info
    // config.bits_per_sample      see WavpackGetBitsPerSample() for info
    // config.num_channels         self evident
    // config.sample_rate          self evident
    // In addition, the following fields and flags may be set: 
    // config->flags:
    // --------------
    // o CONFIG_HYBRID_FLAG         select hybrid mode (must set bitrate)
    // o CONFIG_JOINT_STEREO        select joint stereo (must set override also)
    // o CONFIG_JOINT_OVERRIDE      override default joint stereo selection
    // o CONFIG_HYBRID_SHAPE        select hybrid noise shaping (set override &
    //                                                      shaping_weight != 0)
    // o CONFIG_SHAPE_OVERRIDE      override default hybrid noise shaping
    //                               (set CONFIG_HYBRID_SHAPE and shaping_weight)
    // o CONFIG_FAST_FLAG           "fast" compression mode
    // o CONFIG_HIGH_FLAG           "high" compression mode
    // o CONFIG_VERY_HIGH_FLAG      "very high" compression mode
    // o CONFIG_CREATE_WVC          create correction file
    // o CONFIG_OPTIMIZE_WVC        maximize bybrid compression (-cc option)
    // config->bitrate              hybrid bitrate in bits/sample (scaled up 2^8)
    // config->shaping_weight       hybrid noise shaping coefficient (scaled up 2^10)
    // config->block_samples        force samples per WavPack block (0 = use deflt)
    // If the number of samples to be written is known then it should be passed
    // here. If the duration is not known then pass -1. In the case that the size
    // is not known (or the writing is terminated early) then it is suggested that
    // the application retrieve the first block written and let the library update
    // the total samples indication. A function is provided to do this update and
    // it should be done to the "correction" file also. If this cannot be done
    // (because a pipe is being used, for instance) then a valid WavPack will still
    // be created, but when applications want to access that file they will have
    // to seek all the way to the end to determine the actual duration. A return of
    // FALSE indicates an error.
    static int WavpackSetConfiguration(WavpackContext wpc, WavpackConfig config, long total_samples)
    {
        long flags = (config.bytes_per_sample - 1);
        WavpackStream wps = wpc.stream;
        int bps = 0;
        int shift;
        int i;

        if (config.num_channels > 2)
        {
            wpc.error_message = "too many channels!";

            return Defines.FALSE;
        }

        wpc.total_samples = total_samples;
        wpc.config.sample_rate = config.sample_rate;
        wpc.config.num_channels = config.num_channels;
        wpc.config.bits_per_sample = config.bits_per_sample;
        wpc.config.bytes_per_sample = config.bytes_per_sample;
        wpc.config.block_samples = config.block_samples;
        wpc.config.flags = config.flags;

        if ((wpc.config.flags & Defines.CONFIG_VERY_HIGH_FLAG) > 0)
        {
            wpc.config.flags |= Defines.CONFIG_HIGH_FLAG;
        }

        shift = (config.bytes_per_sample * 8) - config.bits_per_sample;

        for (i = 0; i < 15; ++i)
        {
            if (wpc.config.sample_rate == sample_rates[i])
            {
                break;
            }
        }

        flags |= (i << Defines.SRATE_LSB);
        flags |= (shift << Defines.SHIFT_LSB);

        if ((config.flags & Defines.CONFIG_HYBRID_FLAG) != 0)
        {
            flags |= (Defines.HYBRID_FLAG | Defines.HYBRID_BITRATE | Defines.HYBRID_BALANCE);

            if (((wpc.config.flags & Defines.CONFIG_SHAPE_OVERRIDE) != 0) &&
                    ((wpc.config.flags & Defines.CONFIG_HYBRID_SHAPE) != 0) &&
                    (config.shaping_weight != 0))
            {
                wpc.config.shaping_weight = config.shaping_weight;
                flags |= (Defines.HYBRID_SHAPE | Defines.NEW_SHAPING);
            }

            if ((wpc.config.flags & Defines.CONFIG_OPTIMIZE_WVC) != 0)
            {
                flags |= Defines.CROSS_DECORR;
            }

            bps = config.bitrate;
        }
        else
        {
            flags |= Defines.CROSS_DECORR;
        }

        if (((config.flags & Defines.CONFIG_JOINT_OVERRIDE) == 0) ||
                ((config.flags & Defines.CONFIG_JOINT_STEREO) != 0))
        {
            flags |= Defines.JOINT_STEREO;
        }

        if ((config.flags & Defines.CONFIG_CREATE_WVC) != 0)
        {
            wpc.wvc_flag = Defines.TRUE;
        }

        wpc.stream_version = Defines.CUR_STREAM_VERS;

        wps.wphdr.ckID[0] = 'w';
        wps.wphdr.ckID[1] = 'v';
        wps.wphdr.ckID[2] = 'p';
        wps.wphdr.ckID[3] = 'k';

        // 32 is the size of the WavPack header
        wps.wphdr.ckSize = 32 - 8;
        wps.wphdr.total_samples = wpc.total_samples;
        wps.wphdr.version = wpc.stream_version;
        wps.wphdr.flags = flags | Defines.INITIAL_BLOCK | Defines.FINAL_BLOCK;
        wps.bits = bps;

        if (config.num_channels == 1)
        {
            wps.wphdr.flags &= ~(Defines.JOINT_STEREO | Defines.CROSS_DECORR |
            Defines.HYBRID_BALANCE);
            wps.wphdr.flags |= Defines.MONO_FLAG;
        }

        return Defines.TRUE;
    }

    // Prepare to actually pack samples by determining the size of the WavPack
    // blocks and initializing the stream. Call after WavpackSetConfiguration()
    // and before WavpackPackSamples(). A return of FALSE indicates an error.
    static int WavpackPackInit(WavpackContext wpc)
    {
        if (wpc.config.block_samples > 0)
        {
            wpc.block_samples = wpc.config.block_samples;
        }
        else
        {
            if ((wpc.config.flags & Defines.CONFIG_HIGH_FLAG) > 0)
            {
                wpc.block_samples = wpc.config.sample_rate;
            }
            else if ((wpc.config.sample_rate % 2) == 0)
            {
                wpc.block_samples = wpc.config.sample_rate / 2;
            }
            else
            {
                wpc.block_samples = wpc.config.sample_rate;
            }

            while ((wpc.block_samples * wpc.config.num_channels) > 150000)
            {
                wpc.block_samples /= 2;
            }

            while ((wpc.block_samples * wpc.config.num_channels) < 40000)
            {
                wpc.block_samples *= 2;
            }
        }

        PackUtils.pack_init(wpc);

        return Defines.TRUE;
    }

    // Pack the specified samples. Samples must be stored in longs in the native
    // endian format of the executing processor. The number of samples specified
    // indicates composite samples (sometimes called "frames"). So, the actual
    // number of data points would be this "sample_count" times the number of
    // channels. Note that samples are immediately packed into the block(s)
    // currently being built. If the predetermined number of sample per block
    // is reached, or the block being built is approaching overflow, then the
    // block will be completed and written. If an application wants to break a
    // block at a specific sample, then it must simply call WavpackFlushSamples()
    // to force an early termination. Completed WavPack blocks are send to the
    // function provided in the initial call to WavpackOpenFileOutput(). A
    // return of FALSE indicates an error.
    static int WavpackPackSamples(WavpackContext wpc, long[] sample_buffer, long sample_count)
    {
        WavpackStream wps = wpc.stream;
        long flags = wps.wphdr.flags;

        if ((flags & Defines.SHIFT_MASK) != 0)
        {
            int shift = (int) ((flags & Defines.SHIFT_MASK) >> Defines.SHIFT_LSB);
            long[] ptr = sample_buffer;
            long cnt = sample_count;
            int ptrIndex = 0;

            if ((flags & (Defines.MONO_FLAG | Defines.FALSE_STEREO)) != 0)
            {
                while (cnt > 0)
                {
                    ptr[ptrIndex] = ptr[ptrIndex] >>> shift; // was >>
                    ptrIndex++;
                    cnt--;
                }
            }
            else
            {
                while (cnt > 0)
                {
                    ptr[ptrIndex] = ptr[ptrIndex] >>> shift; // was >>
                    ptrIndex++;
                    ptr[ptrIndex] = ptr[ptrIndex] >>> shift; // was >>
                    ptrIndex++;
                    cnt--;
                }
            }
        }

        while (sample_count > 0)
        {
            long samples_to_pack;
            long samples_packed;

            if (wpc.acc_samples == 0)
            {
                flags &= ~Defines.MAG_MASK;
                flags += ((1L << Defines.MAG_LSB) * (((flags & Defines.BYTES_STORED) * 8) + 7));

                wps.wphdr.block_index = wps.sample_index;
                wps.wphdr.flags = flags;
                PackUtils.pack_start_block(wpc);
            }

            if ((wpc.acc_samples + sample_count) > wpc.block_samples)
            {
                samples_to_pack = wpc.block_samples - wpc.acc_samples;
            }
            else
            {
                samples_to_pack = sample_count;
            }

            samples_packed = PackUtils.pack_samples(wpc, sample_buffer, samples_to_pack);
            sample_count -= samples_packed;

            if (((wpc.acc_samples += samples_packed) == wpc.block_samples) ||
                    (samples_packed != samples_to_pack))
            {
                if (finish_block(wpc) == 0)
                {
                    return Defines.FALSE;
                }
            }
        }

        return Defines.TRUE;
    }

    // Flush all accumulated samples into WavPack blocks. This is normally called
    // after all samples have been sent to WavpackPackSamples(), but can also be
    // called to terminate a WavPack block at a specific sample (in other words it
    // is possible to continue after this operation). A return of FALSE indicates
    // an error.
    static int WavpackFlushSamples(WavpackContext wpc)
    {
        if ((wpc.acc_samples != 0) && (finish_block(wpc) == 0))
        {
            return Defines.FALSE;
        }

        return Defines.TRUE;
    }

    static int finish_block(WavpackContext wpc)
    {
        WavpackStream wps = wpc.stream;
        long bcount;
        int result = 0;

        result = PackUtils.pack_finish_block(wpc);

        wpc.acc_samples = 0;

        if (result == 0)
        {
            wpc.error_message = "output buffer overflowed!";

            return result;
        }

        bcount = (wps.blockbuff[4] & 0xff) + ((wps.blockbuff[5] & 0xff) << 8) +
            ((wps.blockbuff[6] & 0xff) << 16) + ((wps.blockbuff[7] & 0xff) << 24) + 8;

        try
        {
            wpc.out.write(wps.blockbuff, 0, (int) bcount);
        }
        catch (Exception e)
        {
            result = Defines.FALSE;
        }

        if (result == 0)
        {
            wpc.error_message = "can't write WavPack data, disk probably full!";

            return result;
        }

        wpc.filelen += bcount;

        if (wps.block2buff[0] == 'w') // if starts with w then has a WavPack header i.e. it is defined 
        {
            bcount = (wps.block2buff[4] & 0xff) + ((wps.block2buff[5] & 0xff) << 8) +
                ((wps.block2buff[6] & 0xff) << 16) + ((wps.block2buff[7] & 0xff) << 24) + 8;

            try
            {
                wpc.correction_outfile.write(wps.block2buff, 0, (int) bcount);
            }
            catch (Exception e)
            {
                result = Defines.FALSE;
            }

            if (result == 0)
            {
                wpc.error_message = "can't write WavPack data, disk probably full!";

                return result;
            }

            wpc.file2len += bcount;
        }

        return result;
    }

    // Get total number of samples contained in the WavPack file, or -1 if unknown
    static long WavpackGetNumSamples(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return (wpc.total_samples);
        }
        else
        {
            return (long) -1;
        }
    }

    // Get the current sample index position, or -1 if unknown
    static long WavpackGetSampleIndex(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return wpc.stream.sample_index;
        }

        return (long) -1;
    }

    // Returns the sample rate of the specified WavPack file
    static long WavpackGetSampleRate(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return (wpc.config.sample_rate);
        }
        else
        {
            return (long) 44100;
        }
    }

    // Returns the number of channels of the specified WavPack file.
    static int WavpackGetNumChannels(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return (wpc.config.num_channels);
        }
        else
        {
            return 2;
        }
    }

    // Returns the actual number of valid bits per sample contained in the
    // original file from 1 to 24, and which may or may not be a multiple
    // of 8. When this value is not a multiple of 8, then the "extra" bits
    // are located in the LSBs of the results. That is, values are right
    // justified when unpacked into ints, but are left justified in the
    // number of bytes used by the original data.
    static int WavpackGetBitsPerSample(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return (wpc.config.bits_per_sample);
        }
        else
        {
            return 16;
        }
    }

    // Returns the number of bytes used for each sample (1 to 4) in the original
    // file. This is required information for the user of this module because the
    // audio data is returned in the LOWER bytes of the long buffer and must be
    // left-shifted 8, 16, or 24 bits if normalized longs are required.
    static int WavpackGetBytesPerSample(WavpackContext wpc)
    {
        if (null != wpc)
        {
            return (wpc.config.bytes_per_sample);
        }
        else
        {
            return 2;
        }
    }
}