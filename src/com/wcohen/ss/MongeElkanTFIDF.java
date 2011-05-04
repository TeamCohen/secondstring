package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Soft TFIDF-based distance metric, extended to use "soft" token-matching
 * with the MongeElkan distance metric.
 */

public class MongeElkanTFIDF extends SoftTFIDF
{
	public MongeElkanTFIDF() { super(new MongeElkan(), 0.10); }
	public String toString() { return "[MongeElkanTFIDF:threshold="+getTokenMatchThreshold()+"]"; }
	
	static public void main(String[] argv) {
		doMain(new MongeElkanTFIDF(), argv);
	}
}
