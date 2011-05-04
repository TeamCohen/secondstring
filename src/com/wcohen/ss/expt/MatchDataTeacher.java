package com.wcohen.ss.expt;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import java.util.Collections;

/**
 * Train a StringDistanceLearner using MatchData and a Blocker. 
 *
 */
public class MatchDataTeacher extends StringDistanceTeacher
{
	private Blocker blocker;
	private MatchData data;

	public MatchDataTeacher(MatchData data,Blocker blocker) {
		this.blocker = blocker;
		this.data = data;
	}

	public StringWrapperIterator stringWrapperIterator() 
	{
		return data.getIterator();
	}

	public DistanceInstanceIterator distanceInstancePool()
	{
		return new BasicDistanceInstanceIterator(Collections.EMPTY_SET.iterator() );
	}

	public DistanceInstanceIterator distanceExamplePool() 
	{
		blocker.block(data);
		return new DistanceInstanceIterator() {
				private int cursor=0;
				public boolean hasNext() { return cursor<blocker.size(); }
				public Object next() { return blocker.getPair( cursor++ ); }
				public void remove() { throw new UnsupportedOperationException(); }
				public DistanceInstance nextDistanceInstance() { return (DistanceInstance)next();}
			};
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
