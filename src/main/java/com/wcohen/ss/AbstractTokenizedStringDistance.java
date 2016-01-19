package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


/**
 * Abstract distance metric for tokenized strings.
 */

abstract public class AbstractTokenizedStringDistance extends AbstractStringDistance
{
	protected Tokenizer tokenizer;
	// cached, tokenized version of wrappers
	private List tokenizedWrappers; 

	public AbstractTokenizedStringDistance(Tokenizer tokenizer) { this.tokenizer = tokenizer; }
	public AbstractTokenizedStringDistance() { this(SimpleTokenizer.DEFAULT_TOKENIZER); }
	
	final public void setStringWrapperPool(StringWrapperIterator i) { 
		train(i);
	}

	abstract public void train(StringWrapperIterator i); 

	final public StringWrapperIterator prepare(StringWrapperIterator i) {
		tokenizedWrappers = new ArrayList();
		while (i.hasNext()) {
			tokenizedWrappers.add( asBagOfTokens(i.nextStringWrapper()) );
		}
		return new BasicStringWrapperIterator(tokenizedWrappers.iterator());
	}

	// convert to a bag of tokens
	final protected BagOfTokens asBagOfTokens(StringWrapper w) 
	{
		if (w instanceof BagOfTokens) return (BagOfTokens)w;
		else {
			String s = w.unwrap();
			Token[] toks = tokenizer.tokenize(s);
			return new BagOfTokens(s,toks);
		}
	}
}
