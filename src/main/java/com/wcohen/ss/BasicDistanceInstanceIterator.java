package com.wcohen.ss;

import java.util.Iterator;

import com.wcohen.ss.api.DistanceInstance;
import com.wcohen.ss.api.DistanceInstanceIterator;

/** A simple DistanceInstanceIterator implementation. 
 */

public class BasicDistanceInstanceIterator implements DistanceInstanceIterator {
	private Iterator myIterator;
	public BasicDistanceInstanceIterator(Iterator i) { myIterator=i; }
	public boolean hasNext() { return myIterator.hasNext(); }
	public Object next() { return myIterator.next(); }
	public DistanceInstance nextDistanceInstance() { return (DistanceInstance)next(); }
	public void remove() { myIterator.remove(); }
}
