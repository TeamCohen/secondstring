package com.wcohen.ss;

/**
 * Abstract distance between characters.
 *
 */

abstract public class CharMatchScore 
{
	abstract public double matchScore(char c,char d);
	
	/** Scores match as 0, mismatch as -1. */
	static public CharMatchScore DIST_01 = 
	new CharMatchScore() {
		public double matchScore(char c,char d) {
			return Character.toLowerCase(c)==Character.toLowerCase(d) ? 0 : -1;
		}
	};
	
	/** Scores match as +2, mismatch as -1. */
	static public CharMatchScore DIST_21 = 
	new CharMatchScore() {
		public double matchScore(char c,char d) {
			return Character.toLowerCase(c)==Character.toLowerCase(d) ? 2 : -1;
		}
	};
	
}
