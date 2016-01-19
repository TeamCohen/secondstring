package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Monge & Elkan's "level 2" recursive field matching algorithm.
 */

public class Level2MongeElkan extends Level2
{
	private static final StringDistance MY_MONGE_ELKAN = new MongeElkan();

	public Level2MongeElkan() { super( SimpleTokenizer.DEFAULT_TOKENIZER, MY_MONGE_ELKAN) ; }
	public String toString() { return "[Level2MongeElkan]"; }
	
	static public void main(String[] argv) {
		doMain(new Level2MongeElkan(), argv);
	}
}
