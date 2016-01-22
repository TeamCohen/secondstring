package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * An extendible (non-final) class that implements some of the
 * functionality of a string.
 */

public class BasicStringWrapper implements StringWrapper
{
	private String s;

	public BasicStringWrapper(String s) { this.s = s; } 
	public char charAt(int i) { return s.charAt(i); }
	public int length() { return s.length(); }
	public String unwrap() { return s; }
	public String toString() { return "[wrap '"+s+"']"; }
	public int hashCode() { return s.hashCode(); }

}
