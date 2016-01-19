package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Soft TFIDF-based distance metric, extended to use "soft" token-matching
 * with the JaroWinkler distance metric.
 */

public class JaroWinklerTFIDF extends SoftTFIDF
{
	public JaroWinklerTFIDF() { super(new JaroWinkler(), 0.9); }
	public String toString() { return "[JaroWinklerTFIDF:threshold="+getTokenMatchThreshold()+"]"; }
	
	static public void main(String[] argv) {
		doMain(new JaroWinklerTFIDF(), argv);
	}
}
