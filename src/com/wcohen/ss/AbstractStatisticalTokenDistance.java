package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

import org.apache.log4j.Logger;

/**
 * Abstract token distance metric that uses frequency statistics.
 */

abstract public class AbstractStatisticalTokenDistance extends AbstractTokenizedStringDistance
{
	private static Logger log=Logger.getLogger(AbstractTokenizedStringDistance.class);

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

	public AbstractStatisticalTokenDistance(Tokenizer tokenizer) { super(tokenizer); }
	public AbstractStatisticalTokenDistance() { super(); }
	
	/** Accumulate statistics on how often each token value occurs 
	 */
	public void train(StringWrapperIterator i) 
	{
		Set seenTokens = new HashSet();
		while (i.hasNext()) {
			BagOfTokens bag = asBagOfTokens(i.nextStringWrapper());
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
		    log.warn(this.getClass()+" not yet trained for sim('"+s+"','"+t+"')");
		    if (warningCounter == 10) {
			log.warn("(By the way, that's the last warning you'll get about this.)");
		    }
		}
	}

	public int getDocumentFrequency(Token tok) {
		Integer freqInteger = (Integer)documentFrequency.get(tok);
		if (freqInteger==null) return 0;
		else return freqInteger.intValue();
	}
}
