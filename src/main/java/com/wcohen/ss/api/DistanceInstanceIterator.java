package com.wcohen.ss.api;

/**
 * An iterator over DistanceInstance objects.
 */

public interface DistanceInstanceIterator extends java.util.Iterator 
{
	public boolean hasNext();
	public Object next();
	public DistanceInstance nextDistanceInstance();
}
