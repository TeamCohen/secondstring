package com.wcohen.ss.api;

import java.util.*;

/**
 * An 'instance' for a StringDistance, analogous to an 'instance' for
 * a classification learner.  Consists of a pair of StringWrappers,
 * a distance, and some labeling information.
 */

public interface DistanceInstance 
{
	public StringWrapper getA();
	public StringWrapper getB();
	public boolean isCorrect();
	public double getDistance();
	public void setDistance(double distance);

	public static final Comparator INCREASING_DISTANCE = new Comparator() {
			public int compare(Object o1,Object o2) {
				DistanceInstance a = (DistanceInstance)o1;
				DistanceInstance b = (DistanceInstance)o2;
				if (a.getDistance() > b.getDistance()) return -1;
				else if (a.getDistance() < b.getDistance()) return +1;
				else return 0;
			}
		};
}
