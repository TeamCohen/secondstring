package com.wcohen.ss;


/**
 * Soft TFIDF-based distance metric, extended to use "soft" token-matching
 * with the Jaro distance metric.
 */

public class JaroTFIDF extends SoftTFIDF
{
	public JaroTFIDF() { super(new Jaro(), 0.30); }
	public String toString() { return "[JaroTFIDF:threshold="+getTokenMatchThreshold()+"]"; }
	
	static public void main(String[] argv) {
		doMain(new JaroTFIDF(), argv);
	}
}
