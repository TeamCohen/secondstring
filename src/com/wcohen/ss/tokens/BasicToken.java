package com.wcohen.ss.tokens;

import java.io.Serializable;

import com.wcohen.ss.api.*;


/**
 * An interned version of a string.    
 *
 */

public class BasicToken implements Token, Comparable, Serializable
{
	protected final int index;
	protected final String value;
	
	BasicToken(int index,String value) {
		this.index = index;
		this.value = value;
	}
	public String getValue() { return value; }
	public int getIndex() { return index; }
	public int compareTo(Object o) {
		Token t = (Token)o;
		return index - t.getIndex();
	} 
	public int hashCode() { return value.hashCode(); }
	public String toString() { return "[tok "+getIndex()+":"+getValue()+"]"; }
	public boolean equals(Object t) { 
		if (t instanceof BasicToken) return this.hashCode() == (t.hashCode());
		return super.equals(t);
	}
}
