package wpencoder;
/*
** WavpackConfig.java
**
** Copyright (c) 2008 - 2009 Peter McQuillan
**
** All Rights Reserved.
**
** Distributed under the BSD Software License (see license.txt)
**
*/
class WavpackConfig
{
    int bitrate;
    int shaping_weight;
    int bits_per_sample;
    int bytes_per_sample;
    int num_channels;
    int block_samples;
    long flags; // was uint32_t in C
    long sample_rate; // was uint32_t in C
    
    public String toString(){
    	String s = "";
    	s += "bitrate="+bitrate+"\n";
    	s += "shaping_weight="+shaping_weight+"\n";
    	s += "bits_per_sample="+bits_per_sample+"\n";
    	s += "bytes_per_sample="+bytes_per_sample+"\n";
    	s += "num_channels="+num_channels+"\n";
    	s += "block_samples="+block_samples+"\n";
    	s += "flags="+flags+"\n";
    	s += "sample_rate="+sample_rate;
    	return s;
    }
}
