package com.wcohen.ss.expt;

import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Finds all pairs that share a not-too-common character n-gram.
 */

public class NGramBlocker extends TokenBlocker 
{
	private int maxN=4, minN=4;

	public NGramBlocker() { super(); tokenizer=initTokenizer(); }

	public int getMaxNGramSize() { return maxN; }
	public int getMinNGramSize() { return minN; }
	public void setMaxNGramSize(int n) { maxN=n; tokenizer=initTokenizer(); }
	public void setMinNGramSize(int n) { minN=n; tokenizer=initTokenizer(); }

	private Tokenizer initTokenizer() 
	{
		return new NGramTokenizer(minN,maxN,false,SimpleTokenizer.DEFAULT_TOKENIZER);
	}


	public String toString() { return "[NGramBlocker: N="+minN+"-"+maxN+"]"; }
}
