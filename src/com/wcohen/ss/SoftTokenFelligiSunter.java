package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.expt.*;

/**
 * Highly simplified model of Felligi-Sunter's method 1,
 * applied to tokens.
 */

public class SoftTokenFelligiSunter extends AbstractStatisticalTokenDistance
{
	private double mismatchFactor;

	// distance to use to compare tokens
	private StringDistance tokenDistance;
	// threshold beyond which tokens are considered a match
	private double tokenMatchThreshold;
	// default token distance
	private static final StringDistance DEFAULT_TOKEN_DISTANCE = new JaroWinkler();
	
	public SoftTokenFelligiSunter(Tokenizer tokenizer,StringDistance tokenDistance,
																double tokenMatchThreshold,double mismatchFactor) 
	{
		super(tokenizer);
		this.tokenDistance = tokenDistance;
		this.tokenMatchThreshold = tokenMatchThreshold;
		this.mismatchFactor = mismatchFactor;
	}
	public SoftTokenFelligiSunter() {
		this(SimpleTokenizer.DEFAULT_TOKENIZER, DEFAULT_TOKEN_DISTANCE, 0.90, 0.5 );
	}
	public void setMismatchFactor(double d) { mismatchFactor=d; }
	public void setMismatchFactor(Double d) { mismatchFactor=d.doubleValue(); }
	public void setTokenMatchThreshold(double d) { tokenMatchThreshold=d; }
	public void setTokenMatchThreshold(Double d) { tokenMatchThreshold=d.doubleValue(); }	

	public double score(StringWrapper s,StringWrapper t) 
	{
		computeTokenDistances();

		BagOfTokens sBag = (BagOfTokens)s;
		BagOfTokens tBag = (BagOfTokens)t;
		double sim = 0.0;
		for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token tok = (Token)i.next();
			double	df = getDocumentFrequency(tok);
			if (tBag.contains(tok)) {
				double w = -Math.log( df/collectionSize );
				sim += w;
			} else {
				Token matchTok = null;
				double matchScore = tokenMatchThreshold;
				for (Iterator j=tBag.tokenIterator(); j.hasNext(); ) {
					Token tokJ = (Token)j.next();
					double distItoJ = tokenDistance.score( tok.getValue(), tokJ.getValue() );
					if (distItoJ>=matchScore) {
						matchTok = tokJ;
						matchScore = distItoJ;
					}
				}
				if (matchTok!=null) {
					df = neighborhoodDocumentFrequency(tok,  matchScore);
					double w = -Math.log( df/collectionSize );
					sim += w;
				} else {
					double w = -Math.log( df/collectionSize );					
					sim -= w * mismatchFactor;
				}
			}
		}
		return sim;
	}
	
	/** Preprocess a string by finding tokens */ 
	public StringWrapper prepare(String s) 
	{
		return new BagOfTokens(s, tokenizer.tokenize(s));
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
				buf.append(fmt.sprintf(tBag.getWeight(tok)));
			}
		}
		buf.append("\nscore = "+score(s,t));
		return buf.toString(); 
	}
	public String toString() { return "[SoftTokenFelligiSunter]"; }
	
	//
	// compute pairwise distance between all tokens
	//
	private boolean tokenDistancesComputed = false;
	private Map neighborMap;

	private void computeTokenDistances() 
	{
		if (tokenDistancesComputed) return;
		// use blocker to compute pairwise distances between similar tokens
		neighborMap = new HashMap();
		MatchData tokenData = new MatchData();
		for (Iterator i=documentFrequency.keySet().iterator(); i.hasNext(); ) {
			Token tok = (Token)i.next();
			tokenData.addInstance("tokens",tok.getValue(),tok.getValue());
		}
		Blocker tokenBlocker = new ClusterNGramBlocker();
		tokenBlocker.block(tokenData);
		for (int i=0; i<tokenBlocker.size(); i++) {
			Blocker.Pair pair = tokenBlocker.getPair(i);
			String s = pair.getA().unwrap();
			String t = pair.getB().unwrap();
			double d = tokenDistance.score( s, t );
			if (d>=tokenMatchThreshold) {
				addNeighbor( s, t, d);
			}
		}
		// never do this again
		tokenDistancesComputed = true;
	}
	private void addNeighbor(String s, String t, double d) {
		TreeSet set = (TreeSet)neighborMap.get(s);
		if (set==null) {
			set = new TreeSet();
			neighborMap.put(s, set);
		}
		set.add( new TokenNeighbor(t, d) );
	}

	/** Encodes a neighboring token, the document frequency of that
	 * neighboring token, and the distance to it. Goal is that an
	 * ordered set of these will let you quickly find the DF of
	 * all tokens closer than a threshold D.  */
	private class TokenNeighbor implements Comparable {
		public String tokVal;
		public int freq;
		public double score;
		public TokenNeighbor(String tokVal,double score) { 
			this.tokVal=tokVal; 
			this.score=score; 
			this.freq = getDocumentFrequency(tokenizer.intern(tokVal));
		}
		// sort by score, closest first
		public int compareTo(Object o) {
			TokenNeighbor other = (TokenNeighbor)o;
	    if (other.score > score) return +1;
	    else if (other.score < score) return -1;
			else return 0;
		}
		public int hashCode() {
			return tokVal.hashCode();
		}
	}

	private int neighborhoodDocumentFrequency(Token tok, double d) {
		int df = getDocumentFrequency(tok);
		String s = tok.getValue();
		TreeSet neighbors = (TreeSet)neighborMap.get(s);
		if (neighbors==null) return df;
		for (Iterator i=neighbors.iterator(); i.hasNext(); ) {
			TokenNeighbor neighbor = (TokenNeighbor)i.next();
			if (neighbor.score<d) break;
			df += neighbor.freq;
		}
		return df;
	}

	static public void main(String[] argv) {
		doMain(new SoftTokenFelligiSunter(), argv);
	}

}
