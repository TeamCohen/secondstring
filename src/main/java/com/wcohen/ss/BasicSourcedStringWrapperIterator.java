package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.Iterator;

/** A simple StringWrapperIterator implementation. 
 */

public class BasicSourcedStringWrapperIterator implements SourcedStringWrapperIterator {
    private Iterator myIterator;
    public BasicSourcedStringWrapperIterator(Iterator i) { myIterator=i; }
    public boolean hasNext() { return myIterator.hasNext(); }
    public Object next() { return myIterator.next(); }
    public StringWrapper nextStringWrapper() { return (StringWrapper)next(); }
    public SourcedStringWrapper nextSourcedStringWrapper() { return (SourcedStringWrapper)next(); }
    public void remove() { myIterator.remove(); }
}
