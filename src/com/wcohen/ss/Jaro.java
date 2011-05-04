package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * Jaro distance metric. From 'An Application of the Fellegi-Sunter
 * Model of Record Linkage to the 1990 U.S. Decennial Census' by
 * William E. Winkler and Yves Thibaudeau.
 */

public class Jaro extends AbstractStringDistance
{
	public Jaro() { }

	public String toString() { return "[Jaro]"; }

	public double score(StringWrapper s,StringWrapper t) 
	{
		String str1 = s.unwrap();
		String str2 = t.unwrap();
		int halflen = halfLengthOfShorter(str1,str2);
		String common1 = commonChars(str1, str2, halflen);
		String common2 = commonChars(str2, str1, halflen);
		if (common1.length()!=common2.length()) return 0;
		if (common1.length()==0 || common2.length()==0) return 0;
		int transpositions = transpositions(common1,common2);
		double dist =
			 ( common1.length()/((double)str1.length()) + 
				 common2.length()/((double)str2.length()) + 
				 (common1.length()-transpositions)/((double)common1.length()) ) / 3.0;
		return dist;
	}

	public String explainScore(StringWrapper s, StringWrapper t)	
	{
		String str1 = s.unwrap();
		String str2 = t.unwrap();
		int halflen = halfLengthOfShorter(str1,str2);
		String common1 = commonChars(str1, str2, halflen);
		String common2 = commonChars(str2, str1, halflen);
		// count transpositions
		if (common1.length()!=common2.length()) 
			return "common1!=common2: '"+common1+"' != '"+common2+"'\nscore: "+score(s,t)+"\n";
		if (common1.length()==0 || common2.length()==0) 
			return "|commoni|=0: common1='"+common1+"' common2='"+common2+"'\nscore: "+score(s,t)+"\n";
		int transpositions = transpositions(common1,common2);
		String explanation  =
			"common1: '"+common1+"'\n" 
			+ "common2: '"+common2+"'\n" 
			+ "transpositions: "+transpositions+"\n";
		return explanation + "score: " + score(s,t)+"\n";
	}

	private int halfLengthOfShorter(String str1,String str2)
	{
		return (str1.length() > str2.length()) ? str2.length()/2 + 1 : str1.length()/2 + 1;
	}

	private String commonChars(String s,String t,int halflen) 
	{
		StringBuffer common = new StringBuffer(); 
		StringBuffer copy = new StringBuffer(t);
		for (int i=0; i<s.length(); i++) {
			char ch = s.charAt(i);
			boolean foundIt = false;
			for (int j=Math.max(0,i-halflen); !foundIt && j<Math.min(i+halflen,t.length()); j++) {
				if (copy.charAt(j)==ch) {
					foundIt = true;
					common.append(ch);
					copy.setCharAt(j,'*');
				}
			}
		}
		return common.toString();
	}

	private int transpositions(String common1,String common2)
	{
		int transpositions = 0;
		for (int i=0; i<common1.length(); i++) {
			if (common1.charAt(i)!=common2.charAt(i)) 
				transpositions++;
		}
		transpositions /= 2;
		return transpositions;
	}

	public StringWrapper prepare(String s) 
	{
		return new BasicStringWrapper(s.toLowerCase());	
	}

	static public void main(String[] argv) {
		doMain(new Jaro(), argv);
	}
}
