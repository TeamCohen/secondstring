package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/**
 * A string, with an associated bag of tokens.  Each token has an
 * associated weight.
 * 
 */

class BagOfSourcedTokens extends BasicStringWrapper implements SourcedStringWrapper
{
    private Map weightMap = new TreeMap();
    private Set unsourcedTokens = new TreeSet();
    private double totalWeight = 0;
    private SourcedToken[] tokens;
	
    BagOfSourcedTokens(String s,SourcedToken[] tokens) 
    {
        super(s);
        this.tokens = tokens;
        for (int i=0; i<tokens.length; i++) {
            weightMap.put(tokens[i], new Double(getWeight(tokens[i])+1) );
            unsourcedTokens.add(tokens[i].getValue());
        }
        totalWeight = tokens.length;
    }

    public String getSource() {
        return tokens[0].getSource();
    }

    /** Iterates over all tokens in the bag. */
    Iterator tokenIterator() {
        return weightMap.keySet().iterator();
    }
	
    /** Test if this token appears at least once. */
    SourcedToken getEquivalentToken(Token tok) {
        if (unsourcedTokens.contains(tok.getValue())) {
            for (int i=0; i<tokens.length; i++) {
                if (tokens[i].getValue().equals(tok.getValue())) {
                    return tokens[i];
                }
            }
            System.out.println("This is a problem");
            return null;
        } else {
            return null;
        }
    }
	
    /** Weight associated with a token: by default, the number of times
     * the token appears in the bag. */
    double getWeight(Token tok) {
        Double f = (Double)weightMap.get(tok);
        return f==null ? 0 : f.doubleValue();
    }
	
    /** Change the weight of a token in the bag */
    void setWeight(Token tok, double d) {
        Double oldWeight = (Double)weightMap.get(tok);
        totalWeight += oldWeight==null ? d : (d - oldWeight.doubleValue());
        weightMap.put(tok,new Double(d));
    }
	
    /** Number of distinct tokens in the bag. */
    int size() {
        return weightMap.keySet().size();
    }

    /** Total weight of all tokens in bag */
    double getTotalWeight() {
        return totalWeight;
    }

    /** Return array of tokens */
    Token[] getTokens() {
        return tokens;
    }

    /** Return array of tokens */
    SourcedToken[] getSourcedTokens() {
        return tokens;
    }
}
