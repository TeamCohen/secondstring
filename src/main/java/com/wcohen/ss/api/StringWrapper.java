package com.wcohen.ss.api;

/**
 * Something that implements some of the functionality of Java's
 * string class, but which is a non-final class, and hence can also
 * cache additional information to facilitate later processing.
 */

public interface StringWrapper 
{
	/** Return the string that is wrapped. */
	public String unwrap();
	/** Return the i-th char of the wrapped string */
	public char charAt(int i);
	/** Return the length of the wrapped string */
	public int length();
}
