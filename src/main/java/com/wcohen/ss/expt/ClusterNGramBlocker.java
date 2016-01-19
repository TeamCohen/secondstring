package com.wcohen.ss.expt;

/**
 * TokenBlocker for clustering based on NGram co-occurence.
 */

public class ClusterNGramBlocker extends NGramBlocker
{
	public ClusterNGramBlocker() {
		super();
		setClusterMode(true);
	}
	public String toString() { return "[ClusterNGramBlocker]"; }
}
