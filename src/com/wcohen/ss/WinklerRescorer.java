package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * Winkler's reweighting scheme for distance metrics.  In the
 * literature, this was applied to the Jaro metric ('An Application of
 * the Fellegi-Sunter Model of Record Linkage to the 1990
 * U.S. Decennial Census' by William E. Winkler and Yves Thibaudeau.)
 */

public class WinklerRescorer extends AbstractStringDistance
{
	private StringDistance innerDistance;

	/** Rescore the innerDistance's scores, to account for the
	 * subjectively greater importance of the first few characters.
	 * <p>
	 * Note: the innerDistance must produce scores between 0 and 1.
	 */
	public WinklerRescorer(StringDistance innerDistance) { this.innerDistance = innerDistance; }

	public String toString() { return "[WinklerRescorer:"+innerDistance+"]"; }

	public double score(StringWrapper s,StringWrapper t) 
	{
		double dist = innerDistance.score(s,t);
		if (dist<0 || dist>1) 
			throw new IllegalArgumentException("innerDistance should produce scores between 0 and 1"); 
		int prefLength = commonPrefixLength(4,s.unwrap(),t.unwrap());
		dist = dist + prefLength*0.1 * (1 - dist);
		return dist;
	}

	public String explainScore(StringWrapper s, StringWrapper t)	
	{
		double dist = innerDistance.score(s,t);
		int prefLength = commonPrefixLength(4,s.unwrap(),t.unwrap());
		dist = dist + prefLength*0.1 * (1 - dist);
		StringBuffer buf = new StringBuffer("");
		buf.append("original score using "+innerDistance+":\n");
		buf.append(innerDistance.explainScore(s,t)+"\n");
		buf.append("prefLength = max(4,commonPrefixLength) = "+prefLength+"\n");
		buf.append("Corrected score = dist + "+prefLength+"/10 * (1-dist) = "+score(s,t)+"\n");
		return buf.toString();
	}
	private static int commonPrefixLength(int maxLength,String common1,String common2)
	{
		int n = Math.min(maxLength, Math.min(common1.length(), common2.length()) );
		for (int i=0; i<n; i++) {
			if (common1.charAt(i)!=common2.charAt(i)) return i;
		}
		return n; // first n characters are the same
	}
	public StringWrapper prepare(String s) { return innerDistance.prepare(s); }

}
