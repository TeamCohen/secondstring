package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.*;

/**
 * The match method proposed by Monge and Elkan.  They called this
 * Smith-Waterman, but actually, this uses an affine gap model, so
 * it's not Smith-Waterman at all, according to the terminology in
 * Durban, Sec 2.3.
 *
 * Costs are as follows: 
 * mismatched char = -3, match = +5 (case insensitive), approximate match = +3,
 * for pairings in {dt} {gj} {lr} {mn} {bpv} {aeiou} {,.}, start gap = +5, 
 * continue gap = +1
 */

public class MongeElkan extends AffineGap
{
	private boolean scaling = true;
	/** If scaling is true, then distances are scaled to 0-1 */
	public void setScaling(boolean flag) { scaling=flag; }

	/** For interfacing with reflection in MatchExptScript.  Scaling is
	 * true iff flag!=0. */
	public void setScaling(Double flag) { scaling=flag.doubleValue()!=0.0; }

	private static final int CHAR_EXACT_MATCH_SCORE = +5;
	private static final int CHAR_APPROX_MATCH_SCORE = +3;
	private static final int CHAR_MISMATCH_MATCH_SCORE = -3;
	static private Set[] approx;
	static { 
		approx = new Set[7];
		approx[0] = new HashSet(); 
		approx[0].add(new Character('d')); approx[0].add(new Character('t'));
		approx[1] = new HashSet();
		approx[1].add(new Character('g')); approx[1].add(new Character('j'));
		approx[2] = new HashSet();
		approx[2].add(new Character('l')); approx[2].add(new Character('r'));
		approx[3] = new HashSet();
		approx[3].add(new Character('m')); approx[3].add(new Character('n'));
		approx[4] = new HashSet();
		approx[4].add(new Character('b')); approx[4].add(new Character('p')); approx[4].add(new Character('v'));
		approx[5] = new HashSet();
		approx[5].add(new Character('a')); approx[5].add(new Character('e')); approx[5].add(new Character('i'));
		approx[5].add(new Character('o')); approx[5].add(new Character('u'));
		approx[6] = new HashSet();
		approx[6].add(new Character(',')); approx[6].add(new Character('.'));
	}
	static private final CharMatchScore MY_CHAR_MATCH_SCORE = new CharMatchScore() {
			public double matchScore(char c, char d) {
				c = Character.toLowerCase(c);
				d = Character.toLowerCase(d);
				if (c==d) return CHAR_EXACT_MATCH_SCORE;
				else { 
					Character objC = new Character(c);
					Character objD = new Character(d);
					for (int i=0; i<approx.length; i++) {
						if (approx[i].contains(objC) && approx[i].contains(objD))
							return CHAR_APPROX_MATCH_SCORE;
					}
					return CHAR_MISMATCH_MATCH_SCORE;
				}
			}
		};
	public MongeElkan() { 
		super(MY_CHAR_MATCH_SCORE, -5, -1, 0 ); 
		setScaling(true);
	}
	public String toString() { return "[MongeElkan]"; }	

	/** Version of distance which is possibly scaled to [0,1]. */
	public double score(StringWrapper s,StringWrapper t) {
		if (scaling) {
			int minLen = Math.min( s.unwrap().length(), t.unwrap().length() );
			return super.score(s,t) / (minLen * CHAR_EXACT_MATCH_SCORE);
		} else {
			return super.score(s,t);
		}
	}
	/** Version where distance which is possibly scaled to [0,1]. */	
	public String explainScore(StringWrapper s,StringWrapper t) 
	{
		if (scaling) {
			int minLen = Math.min( s.unwrap().length(), t.unwrap().length() );
			double scaledDist =  super.score(s,t) / (minLen * CHAR_EXACT_MATCH_SCORE);
			return super.explainScore(s,t)
				+ "\nScaling factor: "+(minLen*CHAR_EXACT_MATCH_SCORE) 
				+ "\nScaled score: "+scaledDist;
		} else {
			return super.explainScore(s,t);
		}
	}

	static public void main(String[] argv) {
		doMain(new MongeElkan(), argv);
	}
}

