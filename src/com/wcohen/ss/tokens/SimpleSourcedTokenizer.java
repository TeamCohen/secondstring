package com.wcohen.ss.tokens;

import java.util.*;
import com.wcohen.ss.api.*;

/**
 * Simple implementation of a Tokenizer.  Tokens are sequences of
 * alphanumerics, optionally including single punctuation characters.
 */

public class SimpleSourcedTokenizer extends SimpleTokenizer implements SourcedTokenizer
{
    private int nextId = 0;
    private Map tokMap = new TreeMap();

    public static final SimpleSourcedTokenizer DEFAULT_SOURCED_TOKENIZER = new SimpleSourcedTokenizer(true,true);
	
    public SimpleSourcedTokenizer(boolean ignorePunctuation,boolean ignoreCase) {
        super(ignorePunctuation,ignoreCase);
    }

    /**  Return tokenized version of a string. */
    public SourcedToken[] sourcedTokenize(String input,String source) 
    {
        Token[] tokens = tokenize(input);
        SourcedToken[] sourcedTokens = new SourcedToken[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            String key = tokens[i].getValue()+"@"+source;
            if (tokMap.get(key)==null) {
                tokMap.put(key,new Integer(++nextId));
            }
            int id = ((Integer)tokMap.get(key)).intValue();
            sourcedTokens[i] = new BasicSourcedToken(id, tokens[i].getValue(), source);
        }
        return sourcedTokens;
    }
    /** Test routine */
    public static void main(String[] argv) 
    {
        SimpleSourcedTokenizer tokenizer = DEFAULT_SOURCED_TOKENIZER;
        int n = 0;
        for (int i=0; i<argv.length; i++) {
	    System.out.println("argument "+i+": '"+argv[i]+"'");
	    SourcedToken[] tokens = tokenizer.sourcedTokenize(argv[i],Integer.toString(i));
	    for (int j=0; j<tokens.length; j++) {
                System.out.println("token "+(++n)+":"
                                   +" id="+tokens[j].getIndex()
                                   +" value: '"+tokens[j].getValue()
                                   +"' source: '"+tokens[j].getSource()+"'");
	    }
        }
    }
}
