package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * "Level 2" recursive field matching algorithm using Levenstein
 * distance.
 */

public class Level2Levenstein extends Level2
{
	private static final StringDistance MY_LEVENSTEIN = new Levenstein();

	public Level2Levenstein() { super( SimpleTokenizer.DEFAULT_TOKENIZER, MY_LEVENSTEIN) ; }
	public String toString() { return "[Level2Levenstein]"; }
	
	static public void main(String[] argv) {
		doMain(new Level2Levenstein(), argv);
	}
}
