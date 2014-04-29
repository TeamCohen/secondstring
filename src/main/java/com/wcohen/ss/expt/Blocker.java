package com.wcohen.ss.expt;

import com.wcohen.ss.api.*;

import java.io.Serializable;
import java.util.*;

/**
 * Produces candidate pairs from a MatchData structure, and provides
 * access to those candidate pairs.
 */

public abstract class Blocker 
{
	protected boolean clusterMode;
	
	/** Load matchdata and prepare it for production of candidate pairs. */
	abstract public void block(MatchData data);
	
	/** Get the i-th candidate pair, as produced from most recently block()-ed data */
	abstract public Pair getPair(int i);
	
	/**  Return number of candidate pairs, as produced from most recently block()-ed data */
	abstract public int size();
	
	/** In clusterMode, consider pairings between instances from the same
			source.  If clusterMode is false, only consider pairing between
			instances from different sources.
	*/
	final public void setClusterMode(boolean flag) { clusterMode=flag; }  
	final public void setClusterMode(Boolean flag) { clusterMode=flag.booleanValue(); }  

	/**
	 * Holds a pair of instances, with mutable distance between them.
	 */
	public static class Pair implements Comparable, Serializable, DistanceInstance 
	{
		// for serialization control
		private static final long serialVersionUID = 1;
		private static int CURRENT_SERIALIZED_VERSION_NUMBER = 1;
		transient private final StringWrapper a;
		transient private final StringWrapper b;
		private boolean sameIds;
		private double distance = -9999;
		public Pair(MatchData.Instance a,MatchData.Instance b,boolean sameIds) { 
			this.a = a;
			this.b = b;
			this.sameIds = sameIds;
		}
		public String toString() { return "[pair: "+a+";"+b+"]"; }
		public StringWrapper getA() { return a; }
		public StringWrapper getB() { return b; }
		public boolean isCorrect() { return sameIds ; }
		public double getDistance() { return distance; } 
		public void setDistance(double d) { distance=d; } 
		public int compareTo(Object o) {
	    Pair other = (Pair)o;
	    if (other.distance > distance) return +1;
	    else if (other.distance < distance) return -1;
	    else return 0;
		}
	}

	/** Return total number of correct pairs in the dataset. */
	abstract public int numCorrectPairs();

	/** Compute number of correct pairs betwn src1 and src2, where src2>src1  */
	protected int countCorrectPairs(MatchData data) 
	{
		// count the number of times each id appears in each source */
		Map counter = new HashMap();
		for (int i=0; i<data.numSources(); i++) {
			String src = data.getSource(i);
			for (int j=0; j<data.numInstances(src); j++){
				String id = data.getInstance(src, j).getId(); 
				if (id!=null) {
					IdKey key = new IdKey(id,src); 
					Integer c = (Integer)counter.get(key);
					counter.put( key, (c==null ? new Integer(1) : new Integer(c.intValue()+1)) );
				}
			}
		}

		/*
		// show the counter
		for (Iterator i=counter.keySet().iterator(); i.hasNext(); ) {
			IdKey key = (IdKey) i.next();
			System.out.println( key.src+"#"+key.id+" = "+counter.get(key) );
		}
		*/

		// count the number of correct pairs
		int numCorrectPairs = 0;
		Set idsInSrc1 = new HashSet();
		for (int i=0; i<data.numSources(); i++) {
			String src1 = data.getSource(i);
			idsInSrc1.clear();
			for (int j=0; j<data.numInstances(src1);j++) {
				String id = data.getInstance(src1, j).getId(); 
				idsInSrc1.add(id);
				for (int k=i+1; k<data.numSources(); k++) {
					String src2 = data.getSource(k);
					Integer cInteger = (Integer) counter.get( new IdKey(id,src2) );
					if (cInteger!=null) {
						numCorrectPairs += cInteger.intValue();
					}
					//System.out.println( "src1:"+src1+" id:"+id+" src2:"+src2+" c:"+cInteger); 
				}
			}
			if (clusterMode) {
				// count how often something in src1 can be matched correctly with something
				// else in src1
				for (Iterator j=idsInSrc1.iterator(); j.hasNext(); ) {
					String id = (String)j.next();
					Integer cInteger = (Integer) counter.get( new IdKey(id,src1) );
					int c = cInteger.intValue();
					numCorrectPairs += c*(c-1)/2;
				}
			}
		}
		return numCorrectPairs;
	}
	private static class IdKey {
		public String id;
		public String src;
		public IdKey(String id,String src) { this.id = id; this.src = src; }
		public int hashCode() { return id.hashCode() ^ src.hashCode(); }
		public boolean equals(Object o) {
			IdKey key = (IdKey)o;
			return key.id.equals(id) && key.src.equals(src);
		}
	}
}
