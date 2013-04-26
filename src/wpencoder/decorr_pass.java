package wpencoder;
/*
** decorr_pass.java
**
** Copyright (c) 2008 - 2009 Peter McQuillan
**
** All Rights Reserved.
**
** Distributed under the BSD Software License (see license.txt)
**
*/
class decorr_pass
{
    short term;
    short delta;
    short weight_A;
    short weight_B;
    int[] samples_A = new int[Defines.MAX_TERM];
    int[] samples_B = new int[Defines.MAX_TERM];
    int aweight_A;
    int aweight_B;
}
