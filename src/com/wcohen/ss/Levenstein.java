package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * Levenstein string distance. Levenstein distance is basically
 * NeedlemanWunsch with unit costs for all operations.
 */

public class Levenstein extends NeedlemanWunsch
{
	public Levenstein() {
		super(CharMatchScore.DIST_01, 1.0 );
	}
	public String toString() { return "[Levenstein]"; }

	static public void main(String[] argv) {
		doMain(new Levenstein(), argv);
	}
}
