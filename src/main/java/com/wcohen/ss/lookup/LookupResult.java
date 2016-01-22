package com.wcohen.ss.lookup;

import java.io.*;
import java.util.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Shared code for SoftTFIDFDictionary and the rescoring variant of it.
 * 
 */

/*package-visible*/ class LookupResult implements Comparable
{
    private static final java.text.DecimalFormat fmt = new java.text.DecimalFormat("0.000");

    String found; // a string 'looked up' in a dictionary
    Object value; // the value associated with that string
    double score; // the score of the match between the looked-up string and 'found'

    public LookupResult(String found,Object value,double score) 
    {
        this.found=found; this.value=value; this.score=score; 
    }

    public int compareTo(Object o) 
    {
        double diff = ((LookupResult)o).score - score;
        return diff<0 ? -1 : (diff>0?+1:0);
    }

    public String toString() { return "["+fmt.format(score)+" "+found+"=>"+value+"]"; }
}
