package com.wcohen.ss.tokens;

import java.util.*;
import com.wcohen.ss.api.*;

/**
 * Simple implementation of a Tokenizer.  Tokens are sequences of
 * alphanumerics, optionally including single punctuation characters.
 */

public class SimpleTokenizer implements Tokenizer
{
    public static final SimpleTokenizer DEFAULT_TOKENIZER = new SimpleTokenizer(true,true);
	
    private boolean ignorePunctuation = true;
    private boolean ignoreCase = true;
	
    public SimpleTokenizer(boolean ignorePunctuation,boolean ignoreCase) {
        this.ignorePunctuation = ignorePunctuation;
        this.ignoreCase = ignoreCase;
    }

    // parameter setting
    public void setIgnorePunctuation(boolean flag)  { ignorePunctuation = flag; }
    public void setIgnoreCase(boolean flag)  { ignoreCase = flag; }
    public String toString() { return "[SimpleTokenizer "+ignorePunctuation+";"+ignoreCase+"]"; }
	
    /**  Return tokenized version of a string.  Tokens are sequences
     * of alphanumerics, or any single punctuation character. */
    public Token[] tokenize(String input) 
    {
        List tokens = new ArrayList();
        int cursor = 0;
        while (cursor<input.length()) {
	    char ch = input.charAt(cursor);
	    if (Character.isWhitespace(ch)) {
                cursor++;
	    } else if (Character.isLetter(ch)) {
                StringBuffer buf = new StringBuffer("");
                while (cursor<input.length() && Character.isLetter(input.charAt(cursor))) {
                    buf.append(input.charAt(cursor));
                    cursor++;
                }
                tokens.add(internSomething(buf.toString()));
	    } else if (Character.isDigit(ch)) {
                StringBuffer buf = new StringBuffer("");
                while (cursor<input.length() && Character.isDigit(input.charAt(cursor))) {
                    buf.append(input.charAt(cursor));
                    cursor++;
                }
                tokens.add(internSomething(buf.toString()));
	    } else {
                if (!ignorePunctuation) {
                    StringBuffer buf = new StringBuffer("");
                    buf.append(ch);
                    String str = buf.toString();
                    tokens.add(internSomething(str));
                }
                cursor++;
	    }
        }
        return (Token[]) tokens.toArray(new BasicToken[tokens.size()]);
    }
    private Token internSomething(String s) 
    {
        return intern( ignoreCase ? s.toLowerCase() : s );
    }
	
    //
    // 'interning' strings as tokens
    //
    private int nextId = 0;
    private Map tokMap = new TreeMap();

    public Token intern(String s) 
    {
        Token tok = (Token)tokMap.get(s);
        if (tok==null) {
	    tok = new BasicToken(++nextId,s);
	    tokMap.put(s,tok);
        }
        return tok;
    }

    public Iterator tokenIterator()
    {
        return tokMap.values().iterator();
    }

    public int maxTokenIndex()
    {
        return nextId;
    }

    /** Test routine */
    public static void main(String[] argv) 
    {
        SimpleTokenizer tokenizer = DEFAULT_TOKENIZER;
        int n = 0;
        for (int i=0; i<argv.length; i++) {
	    System.out.println("argument "+i+": '"+argv[i]+"'");
	    Token[] tokens = tokenizer.tokenize(argv[i]);
	    for (int j=0; j<tokens.length; j++) {
                System.out.println("token "+(++n)+":"
                                   +" id="+tokens[j].getIndex()
                                   +" value: '"+tokens[j].getValue()+"'");
	    }
        }
    }
}
