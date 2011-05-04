package com.wcohen.ss.expt;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import java.util.Collections;

/**
 * Train a StringDistanceLearner.
 */
public class BasicTeacher extends StringDistanceTeacher
{
	private DistanceInstanceIterator distanceExamplePool;
	private DistanceInstanceIterator distanceInstancePool;
	private StringWrapperIterator wrapperIterator;

	/** Create a teacher from a blocker and a dataset.
	 * Will train from all blocked pairs.
	 */
	public BasicTeacher(final Blocker blocker,final MatchData data)
	{
		blocker.block(data);
		wrapperIterator = data.getIterator();
		distanceInstancePool = new BasicDistanceInstanceIterator(Collections.EMPTY_SET.iterator());
		distanceExamplePool = 
			new DistanceInstanceIterator() {
				private int cursor=0;
				public boolean hasNext() { return cursor<blocker.size(); }
				public Object next() { return blocker.getPair( cursor++ ); }
				public void remove() { throw new UnsupportedOperationException(); }
				public DistanceInstance nextDistanceInstance() { return (DistanceInstance)next();}
			};
	}

	/**
	 * Create a teacher using specific values for the various iterators. 
	 */
	public BasicTeacher(
		StringWrapperIterator wrapperIterator,
		DistanceInstanceIterator distanceInstancePool,
		DistanceInstanceIterator distanceExamplePool
		)
	{
		this.wrapperIterator = wrapperIterator;
		this.distanceInstancePool = distanceInstancePool;
		this.distanceExamplePool = distanceExamplePool;
	}

	public StringWrapperIterator stringWrapperIterator() 
	{
		return wrapperIterator;
	}

	public DistanceInstanceIterator distanceInstancePool()
	{
		return distanceInstancePool;
	}

	public DistanceInstanceIterator distanceExamplePool() 
	{
		return distanceExamplePool;
	}

	public DistanceInstance labelInstance(DistanceInstance distanceInstance) 
	{	
		return distanceInstance;
	}

	public boolean hasAnswers() 
	{ 
		return true; 
	}
}
