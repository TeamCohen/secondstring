package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/**
 * Sourced-based distance metric.
 */

public class SourcedTFIDF extends AbstractSourcedStatisticalTokenDistance
{
    private UnitVector lastVector = null;

    public SourcedTFIDF(SourcedTokenizer tokenizer) { super(tokenizer);	}
    public SourcedTFIDF() { super(); }

    public double score(StringWrapper s0,StringWrapper t0) {
        SourcedStringWrapper s = (SourcedStringWrapper)s0;
        SourcedStringWrapper t = (SourcedStringWrapper)t0;
        checkTrainingHasHappened(s,t);
        UnitVector sBag = asUnitVector(s);
        UnitVector tBag = asUnitVector(t);
        double sim = 0.0;
        int numCommon = 0;
        for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    Token sTok = (Token)i.next();
            Token tTok = null;
	    if ((tTok = tBag.getEquivalentToken(sTok))!=null) {
                sim += sBag.getWeight(sTok) * tBag.getWeight(tTok);
                numCommon++;
            }
        }
        //System.out.println("sim = "+sim+" common="+numCommon+" |s| = "+sBag.size()+" |t| = "+tBag.size()+ " for "+s0+" ~ "+t0);
        return sim;
    }
	
    protected UnitVector asUnitVector(SourcedStringWrapper w) {
        if (w instanceof UnitVector) return (UnitVector)w;
        else if (w instanceof BagOfSourcedTokens) return new UnitVector((BagOfSourcedTokens)w);
        else return new UnitVector(w.unwrap(),tokenizer.sourcedTokenize(w.unwrap(),w.getSource()));
    }

    /** Preprocess a string by finding tokens and giving them TFIDF weights */ 
    public StringWrapper prepare(String s) {
        System.out.println("unknown source for "+s);
        lastVector = new UnitVector(s, tokenizer.sourcedTokenize(s, "*UNKNOWN SOURCE*"));
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
    protected class UnitVector extends BagOfSourcedTokens 
    {
        public UnitVector(String s,SourcedToken[] tokens) {
            super(s,tokens);
            termFreq2TFIDF();
        }
        public UnitVector(BagOfSourcedTokens bag) {
            this(bag.unwrap(), bag.getSourcedTokens());
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
                //System.out.println("final weight: "+tok+" => "+getWeight(tok));
            }
        }
    }
	
    /** Explain how the distance was computed. 
     * In the output, the tokens in S and T are listed, and the
     * common tokens are marked with an asterisk.
     */
    public String explainScore(StringWrapper s, StringWrapper t) 
    {
        BagOfSourcedTokens sBag = (BagOfSourcedTokens)s;
        BagOfSourcedTokens tBag = (BagOfSourcedTokens)t;
        StringBuffer buf = new StringBuffer("");
        PrintfFormat fmt = new PrintfFormat("%.3f");
        buf.append("Common tokens: ");
        for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
	    SourcedToken sTok = (SourcedToken)i.next();
            SourcedToken tTok = null;
            if ((tTok = tBag.getEquivalentToken(sTok))!=null) {
                buf.append(" "+sTok.getValue()+": ");
                buf.append(fmt.sprintf(sBag.getWeight(sTok)));
                buf.append("*"); 
                buf.append(fmt.sprintf(tBag.getWeight(tTok)));
            }
        }
        buf.append("\nscore = "+score(s,t));
        return buf.toString(); 
    }
    public String toString() { return "[SourcedTFIDF]"; }
	
    static public void main(String[] argv) {
        doMain(new SourcedTFIDF(), argv);
    }
}
