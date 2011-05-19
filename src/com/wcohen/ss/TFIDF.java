package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/**
 * TFIDF-based distance metric.
 */

public class TFIDF extends AbstractStatisticalTokenDistance
{
    private UnitVector lastVector = null;

    public TFIDF(Tokenizer tokenizer) { super(tokenizer);	}
    public TFIDF() { super(); }

    public double score(StringWrapper s,StringWrapper t) {
        checkTrainingHasHappened(s,t);
        UnitVector sBag = asUnitVector(s);
        UnitVector tBag = asUnitVector(t);
        double sim = 0.0;
        for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token tok = (Token)i.next();
	    if (tBag.contains(tok)) {
                sim += sBag.getWeight(tok) * tBag.getWeight(tok);
            }
        }
        return sim;
    }
	
    protected UnitVector asUnitVector(StringWrapper w) {
        if (w instanceof UnitVector) return (UnitVector)w;
        else if (w instanceof BagOfTokens) return new UnitVector((BagOfTokens)w);
        else return new UnitVector(w.unwrap(),tokenizer.tokenize(w.unwrap()));
    }

    /** Preprocess a string by finding tokens and giving them TFIDF weights */ 
    public StringWrapper prepare(String s) {
        lastVector = new UnitVector(s, tokenizer.tokenize(s));
        return lastVector;
    }
	
    //
    // some special methods added mostly for SoftTFIDFDictionary
    //

    /** Access the tokens of the last prepare()-ed string. */
    public Token[] getTokens() { return lastVector.getTokens(); }

    /** Access the weight of a token in the vector created for the last prepare()-ed string. */
    public double getWeight(Token token) { return lastVector.getWeight(token); }

    /** Get the document frequency of the token. */
    public int getDocumentFrequency(Token token) 
    { 
        Integer df = (Integer)documentFrequency.get(token);
        if (df == null) return 0;
        else return df.intValue();
    }

    /** Set the document frequency of the token to some value.
     * Setting the collectionSize and also setting the document
     * frequency of every token is an alternative to explicit
     * training.
     */
    public void setDocumentFrequency(Token token, int df) 
    { 
        documentFrequency.put(token,new Integer(df));
    }

    /* Return the size of the collection that this TFIDF measure was
     * trained on to some value. */
    public int getCollectionSize() 
    {
        return collectionSize;
    }

    /**   Setting the collectionSize and alsoSet the size of the collection that this TFIDF measure was
     * trained on to some value.
     * setting the document frequency of every token is an alternative
     * to explicit training.
     */
    public void setCollectionSize(int n) 
    {
        collectionSize=n;
    }

    /** Marker class extending BagOfTokens */
    protected class UnitVector extends BagOfTokens 
    {
        public UnitVector(String s,Token[] tokens) {
            super(s,tokens);
            termFreq2TFIDF();
        }
        public UnitVector(BagOfTokens bag) {
            this(bag.unwrap(), bag.getTokens());
            termFreq2TFIDF();
        }
        /** convert term frequency weights to unit-length TFIDF weights */
        private void termFreq2TFIDF() {
            double normalizer = 0.0;
            for (Iterator i=tokenIterator(); i.hasNext(); ) {
                Token tok = (Token)i.next();
                if (collectionSize>0) {
                    Integer dfInteger = (Integer)documentFrequency.get(tok);
                    // set previously unknown words to df==1, which gives them a high value
                    double df = dfInteger==null ? 1.0 : dfInteger.intValue();
                    double w = Math.log( getWeight(tok) + 1) * Math.log( collectionSize/df );
                    setWeight( tok, w );
                    normalizer += w*w;
                } else {
                    setWeight( tok, 1.0 );
                    normalizer += 1.0;
                }
            }
            normalizer = Math.sqrt(normalizer);
            for (Iterator i=tokenIterator(); i.hasNext(); ) {
                Token tok = (Token)i.next();
                setWeight( tok, getWeight(tok)/normalizer );
            }
        }
    }
	
    /** Explain how the distance was computed. 
     * In the output, the tokens in S and T are listed, and the
     * common tokens are marked with an asterisk.
     */
    public String explainScore(StringWrapper s, StringWrapper t) 
    {
        BagOfTokens sBag = (BagOfTokens)s;
        BagOfTokens tBag = (BagOfTokens)t;
        StringBuffer buf = new StringBuffer("");
        PrintfFormat fmt = new PrintfFormat("%.3f");
        buf.append("Common tokens: ");
        for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token tok = (Token)i.next();
            if (tBag.contains(tok)) {
                buf.append(" "+tok.getValue()+": ");
                buf.append(fmt.sprintf(sBag.getWeight(tok)));
                buf.append("*"); 
                buf.append(fmt.sprintf(tBag.getWeight(tok)));
            }
        }
        buf.append("\nscore = "+score(s,t));
        return buf.toString(); 
    }
    public String toString() { return "[TFIDF]"; }
	
    static public void main(String[] argv) {
        doMain(new TFIDF(), argv);
    }
}
