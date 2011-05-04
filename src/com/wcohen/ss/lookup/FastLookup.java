package com.wcohen.ss.lookup;

import java.io.*;
import java.util.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Interface for SoftTFIDFDictionary and the rescoring variant of it.
 * 
 */

public interface FastLookup
{
    /** Lookup items similar to 'toFind', and return the number of
     * items found.  The found items must have a similarity score
     * greater than minScore to 'toFind'.
     */

    public int lookup(double minScore,String toFind);
    
    /** Get the i'th string found by the last lookup */
    public String getResult(int i);

    /** Get the value of the i'th string found by the last lookup */
    public Object getValue(int i);

    /** Get the score of the i'th string found by the last lookup */
    public double getScore(int i);

}
