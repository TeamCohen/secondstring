package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.*;

/**
 * Abstract class which implements StringDistanceLearner as well as StringDistance.
 * The abstract class provides default implementations of most of the StringDistanceLearner
 * functions, making it easy to implement StringDistances which do little or no
 * learning.
 */

public abstract class AbstractStringDistance implements StringDistance,StringDistanceLearner
{
	//
	// implement StringDistance
	//

	/** This method needs to be implemented by subclasses. 
	 */
	abstract public double score(StringWrapper s,StringWrapper t);

	/** This method needs to be implemented by subclasses. 
	 */
	abstract public String explainScore(StringWrapper s, StringWrapper t);

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
	
	/** Default way to preprocess a string for distance computation.  If
	 * this is an expensive operations, then override this method to
	 * return a StringWrapper implementation that caches appropriate
	 * information about s.
	 */
	public StringWrapper prepare(String s) {
		return new BasicStringWrapper(s);
	}
	
	//
	// implement StringDistanceLearner
	//

	/** Implements the StringDistanceLearner api, by providing a way to
	 * accumulate statistics for a set of related strings.  This is for
	 * distance metrics like TFIDF that use statistics on unlabeled
	 * strings to adjust a distance metric.  The Default is to do
	 * nothing; override this method if it's necessary to accumulate
	 * statistics.
	 */
	public void setStringWrapperPool(StringWrapperIterator i) { 
		/* Do nothing. */ ; 
	}

	/** Implements StringDistanceLearner api by providing a way to 
	 * accept a pool of unlabeled DistanceInstance's.  Default is
	 * to not use this information. 
	 */
	public void setDistanceInstancePool(DistanceInstanceIterator i) {
		/* Do nothing */;
	}

	/** Implements StringDistanceLearner api by informing a teacher
	 * if the learner has DistanceInstance queries.  Default
	 * is to make no queries.
	 */
	public boolean hasNextQuery() {
		return false;
	}

	/** Implements StringDistanceLearner api by querying for
	 * DistanceInstance labels.
	 */
	public DistanceInstance nextQuery() {
		return null;
	}

	/** Implements StringDistanceLearner api by accepting new
	 * DistanceInstance labels.
	 */
	public void addExample(DistanceInstance answeredQuery) {
		/* do nothing */;
	}

	/** Implements StringDistanceLearner api by providing a way to prep a
	 * StringWrapperIterator for training.  By default this makes no
	 * changes to the iterator. */
	public StringWrapperIterator prepare(StringWrapperIterator i) {
		return i;
	}

	/** Implements StringDistanceLearner api by providing a way to prep a
	 * DistanceInstanceIterator for training.  By default this makes no
	 * changes to the iterator. */
	public DistanceInstanceIterator prepare(DistanceInstanceIterator i) {
		return i;
	}

	/** Implements the StringDistanceLearner api by return a StringDistance.
	 * By default, returns this object, which also implements StringDistance.
	 */
	public StringDistance getDistance() {	return this; }

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
