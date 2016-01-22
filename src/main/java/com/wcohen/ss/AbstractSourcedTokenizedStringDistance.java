package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


/**
 * Abstract distance metric for tokenized strings.
 */

abstract public class AbstractSourcedTokenizedStringDistance extends AbstractStringDistance
{
    protected SourcedTokenizer tokenizer;
    // cached, tokenized version of wrappers
    private List tokenizedWrappers; 

    public AbstractSourcedTokenizedStringDistance(Tokenizer tokenizer) { this.tokenizer = (SourcedTokenizer)tokenizer; }
    public AbstractSourcedTokenizedStringDistance() { this(SimpleSourcedTokenizer.DEFAULT_SOURCED_TOKENIZER); }
	
    final public void setStringWrapperPool(StringWrapperIterator i) { 
        train(i);
    }

    abstract public void train(StringWrapperIterator i); 

    final public StringWrapperIterator prepare(StringWrapperIterator i0) {
        SourcedStringWrapperIterator i = (SourcedStringWrapperIterator)i0;
        tokenizedWrappers = new ArrayList();
        while (i.hasNext()) {
            tokenizedWrappers.add( asBagOfSourcedTokens(i.nextSourcedStringWrapper()) );
        }
        return new BasicSourcedStringWrapperIterator(tokenizedWrappers.iterator());
    }

    // convert to a bag of tokens
    final protected BagOfSourcedTokens asBagOfSourcedTokens(SourcedStringWrapper w) 
    {
        if (w instanceof BagOfSourcedTokens) return (BagOfSourcedTokens)w;
        else {
            SourcedToken[] toks = tokenizer.sourcedTokenize(w.unwrap(), w.getSource());
            return new BagOfSourcedTokens(w.unwrap(), toks);
        }
    }
}
