package com.wcohen.ss.tokens;

import com.wcohen.ss.api.*;


/**
 * An interned version of a string, with provinance information
 *
 */

public class BasicSourcedToken extends BasicToken implements SourcedToken
{
    private final String source;
	
    BasicSourcedToken(int index,String value,String source) {
        super(index,value);
        this.source = source;
    }
    public String getSource() { return source; }
    public int compareTo(Object o) {
        SourcedToken t = (SourcedToken)o;
        int d = t.getValue().compareTo(value);
        if (d!=0) return d;
        return t.getSource().compareTo(source);
    } 
    public boolean equals(Object o) {
        return compareTo(o)==0;
    }
    public int hashCode() { return (value+"@"+source).hashCode(); }
    public String toString() { return "[tok "+getIndex()+":"+getValue()+" src:"+source+"]"; }
}
