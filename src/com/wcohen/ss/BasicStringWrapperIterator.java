package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.Iterator;

/** A simple StringWrapperIterator implementation. 
 */

public class BasicStringWrapperIterator implements StringWrapperIterator {
	private Iterator myIterator;
	public BasicStringWrapperIterator(Iterator i) { myIterator=i; }
	public boolean hasNext() { return myIterator.hasNext(); }
	public Object next() { return myIterator.next(); }
	public StringWrapper nextStringWrapper() { return (StringWrapper)next(); }
	public void remove() { myIterator.remove(); }
}
