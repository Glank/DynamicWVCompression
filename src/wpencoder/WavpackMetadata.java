package wpencoder;
/*
** WavpackMetadata.java
**
** Copyright (c) 2008 - 2009 Peter McQuillan
**
** All Rights Reserved.
**
** Distributed under the BSD Software License (see license.txt)
**
*/
class WavpackMetadata
{
    int byte_length;
    byte[] temp_data = new byte[64];
    byte[] data;
    short id; // was uchar in C
}
