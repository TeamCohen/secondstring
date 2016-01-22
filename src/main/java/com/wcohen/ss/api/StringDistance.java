package com.wcohen.ss.api;

import java.util.*;

/**
 * Compute the difference between pairs of strings.
 *
 * <p>For some types of distances, it is fine to simply create a
 * StringDistance object and then use it, e.g.,
 * <code>new JaroWinkler().compare("frederic", "fredrick")</code>.
 *
 * <p>Other string metrics benefit from caching information about a
 * string, especially when many comparisons are many concerning the
 * same string.  The prepare() method returns a {@link StringWrapper}
 * object, which can cache any appropriate information about the
 * String it 'wraps'.  The most frequent use of caching here is saving
 * a tokenized version of a string (as a BagOfTokens, which is
 * a subclass of StringWrapper.)
 *
 */
public interface StringDistance 
{
	/** Find the distance between s and t.  Larger values indicate more
			similar strings.
	*/
	public double score(StringWrapper s,StringWrapper t);
	
	/** Find the distance between s and t */
	public double score(String s, String t);
	
	/** Preprocess a string for distance computation */ 
	public StringWrapper prepare(String s);
	
	/** Explain how the distance was computed. */
	public String explainScore(StringWrapper s, StringWrapper t);
	
	/** Explain how the distance was computed. */
	public String explainScore(String s, String t);
}
