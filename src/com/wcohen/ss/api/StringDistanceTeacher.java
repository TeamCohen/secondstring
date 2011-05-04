package com.wcohen.ss.api;

/**
 * Train a StringDistanceLearner and return the learned
 * StringDistance, using some unspecified source of information to
 * train the learner.
 *
 */
public abstract class StringDistanceTeacher
{
	final public StringDistance train(StringDistanceLearner learner) 
	{
		// TFIDF-style 'training' based on observing corpus statistics
		learner.setStringWrapperPool( learner.prepare(stringWrapperIterator()) );

		// provide examples for unsupervised/semi-supervised training
		learner.setDistanceInstancePool( learner.prepare(distanceInstancePool() ));

		// supervised training
		for (DistanceInstanceIterator i=distanceExamplePool(); i.hasNext(); ) {
			learner.addExample( i.nextDistanceInstance() );
		}

		// active or passive learning from labeled data
		while (learner.hasNextQuery() && hasAnswers()) {
			DistanceInstance query = learner.nextQuery();
			DistanceInstance answeredQuery = labelInstance(query);
			if (answeredQuery!=null) {
				learner.addExample( answeredQuery );
			}
		}

		// final result
		return learner.getDistance();
	}

	/** Strings over which distances will be computed. */
	abstract protected StringWrapperIterator stringWrapperIterator();

	/** A pool of unlabeled pairs of strings over which distances will be computed, 
	 * to be used for active or semi-supervised learning. */
	abstract protected DistanceInstanceIterator distanceInstancePool();

	/** A pool of unlabeled pairs of strings over which distances will be computed, 
	 * to be used for supervised learning. */
	abstract protected DistanceInstanceIterator distanceExamplePool();

	/** Label an instance queried by the learner.  Return null if the query
	 * can't be answered. */
	abstract protected DistanceInstance labelInstance(DistanceInstance distanceInstance);

	/** Return true if this teacher can answer more queries. */
	abstract protected boolean hasAnswers();
}
