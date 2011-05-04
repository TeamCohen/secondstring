package com.wcohen.ss.api;

/**
 * An iterator over StringWrapper objects.
 */

public interface StringWrapperIterator extends java.util.Iterator 
{
	public boolean hasNext();
	public Object next();
	public StringWrapper nextStringWrapper();
}
