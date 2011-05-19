package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.*;

/**
 * Abstract class StringDistance defined over Strings that are broken
 * into fields.  This could actually be used in several ways:
 * <ol>
 * <li>To merge together scores from a single StringDistance d
 * applied to several different subfields f1,.., fk of a string.
 * <li>To merge together scores from a multiple StringDistances
 * d1, ..., dk, where di applied to the corresponding fi. 
 * <p>
 * <li>With a little extra coding, this could also be
 * used to merge together scores from a multiple StringDistances
 * applied a single string. This would require using a new 
 * MultiStringWrapper constructor that makes k copies of 
 * a single string, rather than splitting a string into k 
 * disjoint parts.
 * </ol>
 */

public abstract class MultiStringDistance implements StringDistance
{
	private String delim;

	public MultiStringDistance(String delim) { 
		this.delim = delim; 
	}

	final public double score(StringWrapper s,StringWrapper t) 
	{
		MultiStringWrapper ms = asMultiStringWrapper(s);
		MultiStringWrapper mt = asMultiStringWrapper(t);
		return scoreCombination( multiScore(ms,mt) );
	}

	/** Combine the scores for each primitive distance function on each field. */
	abstract protected double scoreCombination(double[] multiScore);

	/** Compute the scores for each primitive distance function on each field. */
	private double[] multiScore(MultiStringWrapper ms,MultiStringWrapper mt) 
	{
		if (ms.size() != mt.size()) {
			throw new IllegalArgumentException("inputs have different numbers of fields");
		}
		int n = ms.size();
		double scores[] = new double[n];
		for (int i=0; i<n; i++) {
			scores[i] = getDistance(i).score(ms.get(i), mt.get(i));
		}
		return scores;
	}

	final public String explainScore(StringWrapper s, StringWrapper t) 
	{
		MultiStringWrapper ms = asMultiStringWrapper(s);
		MultiStringWrapper mt = asMultiStringWrapper(t);
		if (ms.size() != mt.size()) {
			throw new IllegalArgumentException("inputs have different numbers of fields");
		}
		int n = ms.size();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<n; i++) {
			buf.append("Field "+(i+1)+": s='"+ms.get(i)+"' t='"+mt.get(i)+"':\n");
			buf.append( getDistance(i).explainScore( ms.get(i), mt.get(i)) );
		}
		buf.append("combination:\n");
		buf.append( explainScoreCombination(multiScore(ms,mt)) );
		return buf.toString();
	}

	/** Explain how to combine the scores for each primitive distance
	 * function on each field. */
	abstract protected String explainScoreCombination(double[] multiScore);

  /** Strings are scored by converting them to StringWrappers with the
	 * prepare function. */
	final public double score(String s, String t) {
		return score(prepare(s), prepare(t));
	}
	
	/** Scores are explained by converting Strings to StringWrappers
	 * with the prepare function. */
	final public String explainScore(String s, String t) {
		return explainScore(prepare(s),prepare(t));
	}
	
	/** Prepare a string.
	 */
	final public StringWrapper prepare(String s) {
		MultiStringWrapper ms = new MultiStringWrapper(s,delim);
		if (!isLegalMultiStringWrapperSize(ms.size())) {
			throw new IllegalArgumentException("string has invalid number of fields");
		}
		for (int i=0; i<ms.size(); i++) {
			ms.set(i, getDistance(i).prepare( ms.get(i).unwrap() ));
		}
		return ms;
	}

	/** Lazily prepare a string. Ie, if it's already a
	 * MultiStringWrapper, do nothing, otherwise use prepare() to
	 * convert to a MultiStringWrapper.
	 */
	protected MultiStringWrapper asMultiStringWrapper(StringWrapper w) {
		if (w instanceof MultiStringWrapper) return (MultiStringWrapper)w;
		else return (MultiStringWrapper)prepare(w.unwrap());
	}

	/** Get the distance used for the i-th pair of fields */
	abstract protected StringDistance getDistance(int i);

	/** Check if a string has a valid number of fields. Override this
	 * method if some assumption is made about the number of fields.
	 */
	protected boolean isLegalMultiStringWrapperSize(int n) {
		return n!=0;
	}

	/** Default main routine for testing */
	final protected static void doMain(StringDistance d,String[] argv) 
	{
		if (argv.length!=2) {
		    System.out.println("usage: string1 string2");
		} else {
		    System.out.println(d.explainScore(argv[0],argv[1]));
		}
	}

}
