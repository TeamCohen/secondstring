package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Mixture-based distance metric.
 */

public class Mixture extends AbstractStatisticalTokenDistance
{
	private double minChange = 0.01;
	private double maxIterate = 20;

	public Mixture(Tokenizer tokenizer) { super(tokenizer);	}
	public Mixture() { super(); }

	/** Distance is argmax_lambda prod_{w in s} lambda Pr(w|t) * (1-lambda) Pr(w|background). 
	 * This is computed with E/M. */
	public double score(StringWrapper s,StringWrapper t) 
	{
		BagOfTokens sBag = asBagOfTokens(s);
		BagOfTokens tBag = asBagOfTokens(t);
		double lambda = 0.5;
		int iterations = 0;
		while (true) {
			double newLamba = 0.0;
			// E step: compute prob each token is draw from T
			for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
				Token tok = (Token)i.next();
				double sWeight = sBag.getWeight(tok);
				double tWeight = tBag.getWeight(tok);
				double probTokGivenT = tWeight/tBag.getTotalWeight();
				double probTokGivenCorpus = ((double)getDocumentFrequency(tok))/totalTokenCount;
				double probDrawnFromT = lambda * probTokGivenT;
				double probDrawnFromCorpus = (1.0-lambda) * probTokGivenCorpus;
				double normalizingConstant = probTokGivenT + probTokGivenCorpus;
				probDrawnFromT /= normalizingConstant;
				probDrawnFromCorpus /= normalizingConstant;
				newLamba += probDrawnFromT * sWeight;
			}
			// M step: find best value of lambda
			newLamba /= sBag.getTotalWeight();
			// halt if converged
			double change = newLamba - lambda;
			if (iterations>maxIterate || (change>= -minChange && change<=minChange)) break;
			else lambda = newLamba;
		}
		return lambda;
	}
	
	/** Explain how the distance was computed. 
	 * In the output, the tokens in S and T are listed, and the
	 * common tokens are marked with an asterisk.
	 */
	public String explainScore(StringWrapper s, StringWrapper t) 
	{
		return "can't explain";
	}

	public String toString() { return "[Mixture]"; }
	
	static public void main(String[] argv) {
		doMain(new Mixture(), argv);
	}
}
