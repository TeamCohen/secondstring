package com.wcohen.ss.tokens;

import java.util.*;
import com.wcohen.ss.api.*;

/**
 * Character tokenizer implementation.  Tokens are single characters of the source string.
 */

public class CharacterTokenizer implements Tokenizer
{
    public static final CharacterTokenizer DEFAULT_TOKENIZER = new CharacterTokenizer(true,true);
	
    private boolean ignorePunctuation = true;
    private boolean ignoreCase = true;
	
    public CharacterTokenizer(boolean ignorePunctuation,boolean ignoreCase) {
        this.ignorePunctuation = ignorePunctuation;
        this.ignoreCase = ignoreCase;
    }

    // parameter setting
    public void setIgnorePunctuation(boolean flag)  { ignorePunctuation = flag; }
    public void setIgnoreCase(boolean flag)  { ignoreCase = flag; }
    public String toString() { return "[CharacterTokenizer "+ignorePunctuation+";"+ignoreCase+"]"; }
	
    /**  Return tokenized version of a string.  Tokens are sequences
     * of alphanumerics, or any single punctuation character. */
    public Token[] tokenize(String input) 
    {
    	char[] stringChars =  input.toCharArray();
        List<Token> tokens = new ArrayList<Token>();
        for (char c : stringChars) {
			if(Character.isLetterOrDigit(c)){
				tokens.add(internSomething(Character.toString(c)));
			}
			else if (!ignorePunctuation && !Character.isWhitespace(c)) {
				tokens.add(internSomething(Character.toString(c)));
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
    private Map<String, Token> tokMap = new TreeMap<String, Token>();

    public Token intern(String s) 
    {
        Token tok = (Token)tokMap.get(s);
        if (tok==null) {
	    tok = new BasicToken(++nextId,s);
	    tokMap.put(s,tok);
        }
        return tok;
    }

    public Iterator<Token> tokenIterator()
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
        CharacterTokenizer tokenizer = DEFAULT_TOKENIZER;
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
