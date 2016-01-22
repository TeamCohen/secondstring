package com.wcohen.ss;

/**
 * Jaro distance metric, as extended by Winkler.  
 */
public class JaroWinkler extends WinklerRescorer
{
	public JaroWinkler() { super(new Jaro()); }
	static public void main(String[] argv) {	doMain(new JaroWinkler(), argv);	}
}
