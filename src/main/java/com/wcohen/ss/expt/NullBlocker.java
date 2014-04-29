package com.wcohen.ss.expt;

import java.util.ArrayList;
import com.wcohen.ss.api.*;


/**
 * Inefficient exhausitive blocking algorithm.  Produces all possible
 * pairs from a MatchData structure.
 */

public class NullBlocker extends Blocker {
	
	private ArrayList pairList;
	private int numCorrectPairs;
	
	public NullBlocker() {;}
	
	public void block(MatchData data) 
	{
		numCorrectPairs = countCorrectPairs(data);
		pairList = new ArrayList();
		for (int i=0; i<data.numSources(); i++) {
			int lo1 = clusterMode? i : i+1;
	    for (int j=lo1; j<data.numSources(); j++) {
				String src1 = data.getSource(i);
				String src2 = data.getSource(j);
				for (int k=0; k<data.numInstances(src1); k++) {
					int lo2 = (clusterMode && src1==src2) ? k+1 : 0;
					for (int el=lo2; el<data.numInstances(src2); el++) {
						MatchData.Instance a = data.getInstance(src1,k);
						MatchData.Instance b = data.getInstance(src2,el);
						Pair p = new Pair(  a, b, a.sameId(b) );
						pairList.add(p);
					}
				}
	    }
		}
	}
	public int size() { return pairList.size();  }
	public Pair getPair(int i) { return (Pair)pairList.get(i); }
	public String toString() { return "[NullBlocker:clusterMode="+clusterMode+"]"; }
	public int numCorrectPairs() { return numCorrectPairs; }
}
