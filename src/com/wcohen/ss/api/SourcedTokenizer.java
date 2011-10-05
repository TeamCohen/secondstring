package com.wcohen.ss.api;

import java.util.Iterator;

/**
 * Split a string into tokens, retaining provinance.
 */

public interface SourcedTokenizer extends Tokenizer
{
    /**  Return tokenized version of a string, as an array of
     *  SourcedToken objects.
     */
    public SourcedToken[] sourcedTokenize(String input,String source);
}


