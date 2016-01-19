package com.wcohen.ss.api;

/**
 * An iterator over StringWrapper objects.
 */

public interface SourcedStringWrapperIterator extends StringWrapperIterator
{
    public SourcedStringWrapper nextSourcedStringWrapper();
}
