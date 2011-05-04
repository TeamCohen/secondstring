package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * TFIDF-based distance metric, extended to use "soft" token-matching.
 * Specifically, tokens are considered a partial match if they get
 * a good score using an inner string comparator.
 *
 * <p>On the WHIRL datasets, thresholding JaroWinkler at 0.9 or 0.95
 * seems to be about right.
 */

public class SoftTFIDF extends TFIDF
{
	// distance to use to compare tokens
	private StringDistance tokenDistance;
	// threshold beyond which tokens are considered a match
	private double tokenMatchThreshold;
	// default token distance
	private static final StringDistance DEFAULT_TOKEN_DISTANCE = new JaroWinkler();

	public SoftTFIDF(Tokenizer tokenizer,StringDistance tokenDistance,double tokenMatchThreshold) { 
		super(tokenizer);	
		this.tokenDistance = tokenDistance;
		this.tokenMatchThreshold = tokenMatchThreshold;
	}
	public SoftTFIDF(StringDistance tokenDistance,double tokenMatchThreshold) { 
		super(); 
		this.tokenDistance = tokenDistance;
		this.tokenMatchThreshold = tokenMatchThreshold;
	}
	public SoftTFIDF(StringDistance tokenDistance) {
		this(tokenDistance, 0.9);
	}
	public void setTokenMatchThreshold(double d) { tokenMatchThreshold=d; }
	public void setTokenMatchThreshold(Double d) { tokenMatchThreshold=d.doubleValue(); }	
	public double getTokenMatchThreshold() { return tokenMatchThreshold; }

	public double score(StringWrapper s,StringWrapper t) {
		checkTrainingHasHappened(s,t);
		//if (!(s instanceof BagOfTokens)) System.out.println("s is "+s+" - "+s.getClass());
		UnitVector sBag = asUnitVector(s);
		UnitVector tBag = asUnitVector(t);
		double sim = 0.0;
		for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token tok = (Token)i.next();
	    if (tBag.contains(tok)) {
				sim += sBag.getWeight(tok) * tBag.getWeight(tok);
			} else {
				// find best matching token
				double matchScore = tokenMatchThreshold;
				Token matchTok = null;
				for (Iterator j=tBag.tokenIterator(); j.hasNext(); ) {
					Token tokJ = (Token)j.next();
					double distItoJ = tokenDistance.score( tok.getValue(), tokJ.getValue() );
					if (distItoJ>=matchScore) {
						matchTok = tokJ;
						matchScore = distItoJ;
					}
				}
				if (matchTok!=null) {
					sim += sBag.getWeight(tok) * tBag.getWeight(matchTok) * matchScore;
				}
			}
		}
		//System.out.println("common="+numCommon+" |s| = "+sBag.size()+" |t| = "+tBag.size());
		return sim;
	}
	
	/** Explain how the distance was computed. 
	 * In the output, the tokens in S and T are listed, and the
	 * common tokens are marked with an asterisk.
	 */
	public String explainScore(StringWrapper s, StringWrapper t) 
	{
		BagOfTokens sBag = (BagOfTokens)s;
		BagOfTokens tBag = (BagOfTokens)t;
		StringBuffer buf = new StringBuffer("");
		PrintfFormat fmt = new PrintfFormat("%.3f");
		buf.append("Common tokens: ");
		for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token tok = (Token)i.next();
			if (tBag.contains(tok)) {
				buf.append(" "+tok.getValue()+": ");
				buf.append(fmt.sprintf(sBag.getWeight(tok)));
				buf.append("*"); 
				buf.append(fmt.sprintf(tBag.getWeight(tok)));
			} else {
				// find best matching token
				double matchScore = tokenMatchThreshold;
				Token matchTok = null;
				for (Iterator j=tBag.tokenIterator(); j.hasNext(); ) {
					Token tokJ = (Token)j.next();
					double distItoJ = tokenDistance.score( tok.getValue(), tokJ.getValue() );
					if (distItoJ>=matchScore) {
						matchTok = tokJ;
						matchScore = distItoJ;
					}
				}
				if (matchTok!=null) {
					buf.append(" '"+tok.getValue()+"'~='"+matchTok.getValue()+"': ");
					buf.append(fmt.sprintf(sBag.getWeight(tok)));
					buf.append("*");
					buf.append(fmt.sprintf(tBag.getWeight(matchTok)));
					buf.append("*"); 
					buf.append(fmt.sprintf(matchScore));
				}
			}
		}
		buf.append("\nscore = "+score(s,t));
		return buf.toString(); 
	}
	public String toString() { return "[SoftTFIDF thresh="+tokenMatchThreshold+";"+tokenDistance+"]"; }
}
