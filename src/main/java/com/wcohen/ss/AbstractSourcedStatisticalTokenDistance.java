package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


/**
 * Abstract token distance metric that uses frequency statistics.
 */

abstract public class AbstractSourcedStatisticalTokenDistance extends AbstractSourcedTokenizedStringDistance
{
	// to save space, allocate the small numbers only once in the documentFrequency map
	private static final Integer ONE = new Integer(1);
	private static final Integer TWO = new Integer(2);
	private static final Integer THREE = new Integer(3);

	// maps tokens to document frequency
	protected Map documentFrequency = new HashMap(); 
	// count number of documents
	protected int collectionSize = 0;
	// count number of tokens
	protected int totalTokenCount = 0;

	// count warnings
	private int warningCounter = 0;

	public AbstractSourcedStatisticalTokenDistance(SourcedTokenizer tokenizer) { super(tokenizer); }
	public AbstractSourcedStatisticalTokenDistance() { super(); }
	
	/** Accumulate statistics on how often each token value occurs 
	 */
	public void train(StringWrapperIterator i0) 
	{
            SourcedStringWrapperIterator i = (SourcedStringWrapperIterator)i0;
            Set seenTokens = new HashSet();
            while (i.hasNext()) {
                BagOfSourcedTokens bag = asBagOfSourcedTokens(i.nextSourcedStringWrapper());
                seenTokens.clear();
                for (Iterator j=bag.tokenIterator(); j.hasNext(); ) {
                    totalTokenCount++;
                    Token tokj = (Token)j.next();
                    if (!seenTokens.contains(tokj)) {
                        seenTokens.add(tokj);
                        // increment documentFrequency counts
                        Integer df = (Integer)documentFrequency.get(tokj);
                        if (df==null) documentFrequency.put(tokj,ONE); 
                        else if (df==ONE) documentFrequency.put(tokj,TWO);
                        else if (df==TWO) documentFrequency.put(tokj,THREE);
                        else documentFrequency.put(tokj, new Integer(df.intValue()+1));
                    }
                }
                collectionSize++;
            }
	}

	protected void checkTrainingHasHappened(StringWrapper s, StringWrapper t)
	{
            if (collectionSize==0 && ++warningCounter<=10) {
                System.out.println("Warning: "+this.getClass()+" not yet trained for sim('"+s+"','"+t+"')");
                if (warningCounter == 10) {
                    System.out.println("(By the way, that's the last warning you'll get about this.)");
                }
            }
	}

	public int getDocumentFrequency(Token tok) {
            Integer freqInteger = (Integer)documentFrequency.get(tok);
            if (freqInteger==null) return 0;
            else return freqInteger.intValue();
	}
}
