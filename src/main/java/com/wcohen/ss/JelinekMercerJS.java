package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/**
 * Jensen-Shannon distance of two unigram language models, smoothed
 * using Jelinek-Mercer mixture model.
 */

public class JelinekMercerJS extends JensenShannonDistance
{
	private double lambda = 0.5;

	public double getLambda() { return lambda; }
	public void setLambda(double lambda) { this.lambda = lambda; }
	public void setLambda(Double lambda) { this.lambda = lambda.doubleValue(); }
	
	public JelinekMercerJS(Tokenizer tokenizer,double lambda) { 
		super(tokenizer);
		setLambda(lambda);
	}
	public JelinekMercerJS() { 
		this(SimpleTokenizer.DEFAULT_TOKENIZER, 0.2); 
	}

	/** smoothed probability of the token */
	protected double smoothedProbability(Token tok, double freq, double totalWeight) 
	{
		return (1-lambda) * (freq/totalWeight) + lambda * backgroundProb(tok);
	}
	public String toString() { return "[JelinekMercerJS lambda="+lambda+"]"; }

	static public void main(String[] argv) {
		doMain(new JelinekMercerJS(), argv);
	}
}
