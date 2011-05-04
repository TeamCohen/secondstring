package com.wcohen.ss.api;

import java.util.Iterator;

/**
 * Split a string into tokens.
 */

public interface Tokenizer 
{
    /**  Return tokenized version of a string */
    public Token[] tokenize(String input);

    /** Convert a given string into a token.  The intern function
     * should have these properties: (1) If s1.equals(s2), then
     * intern(s1)==intern(s2). (2) If no string equal to s1 has ever
     * been interned before, then intern(s1).getIndex() will be larger
     * than every previously-assigned index--i.e, token 'indexes' are
     * assigned in increasing order.
     */
    public Token intern(String s);

    /** Return an iterator over interned tokens */
    public Iterator tokenIterator();

    /** Return the higest index of any interned token */
    public int maxTokenIndex();
}


