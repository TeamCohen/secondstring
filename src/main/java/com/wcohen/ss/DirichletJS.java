package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/**
 * Jensen-Shannon distance of two unigram language models, smoothed
 * using Dirichlet prior.
 */

public class DirichletJS extends JensenShannonDistance
{
	private double pseudoCount = 1.0;

	public double getPseudoCount() { return pseudoCount; }
	public void setPseudoCount(double c) { this.pseudoCount = c; }
	public void setPseudoCount(Double c) { this.pseudoCount = c.doubleValue(); }
	
	public DirichletJS(Tokenizer tokenizer,double pseudoCount) { 
		super(tokenizer);
		setPseudoCount(pseudoCount);
	}
	public DirichletJS() { 
		this(SimpleTokenizer.DEFAULT_TOKENIZER, 1.0); 
	}
	public String toString() { return "[DirichletJS pcount="+pseudoCount+"]"; }

	/** smoothed probability of the token */
	protected double smoothedProbability(Token tok, double freq, double totalWeight) 
	{
		return (freq + pseudoCount * backgroundProb(tok)) / (totalWeight + pseudoCount);
	}

	static public void main(String[] argv) {
		doMain(new DirichletJS(), argv);
	}
}
