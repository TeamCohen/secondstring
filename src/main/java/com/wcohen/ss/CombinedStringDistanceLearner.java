package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.*;

/**
 * Abstract StringDistanceLearner class which combines results of a number of
 * inner distance metrics, learned by a number of inner distance learners.
 */

public abstract class CombinedStringDistanceLearner implements StringDistanceLearner
{
	protected StringDistanceLearner[] innerLearners;
	protected String delim;

	// use some reasonable defaults
	public CombinedStringDistanceLearner() {
		innerLearners = 
			new StringDistanceLearner[] { 
				new JaroWinkler()
				,new ScaledLevenstein()
				,new Jaccard()
				,new TFIDF()
				,new JaroWinklerTFIDF() 
			};
		delim = null;
	}

	public CombinedStringDistanceLearner(StringDistanceLearner[] innerLearners,String delim) { 
		this.innerLearners = innerLearners;
		this.delim = delim;
	}

	//
	// stuff for subclasses to implement
	//

	/** Pass an iterator over unlabeled string wrappers to the score-combination learner,
	 * just in case that's useful.
	 */
	abstract protected void comboSetStringWrapperPool(Iterator i); // on unlabeled StringWrapper's

	/** Set up a pool of (possibly unlabeled) instance distances, for the learner
	 * to make queries from.
	 */
	abstract protected void comboSetDistanceInstancePool(Iterator i); // on list of MyMultiDistanceInstance's

	/**
	 * Poll the routine that learns to combine inner distance scores to see if it
	 * wants to make more queries.
	 */
	abstract protected boolean comboHasNextQuery();  // active learning

	/** Get the next query from the score-combination learner.
	 */
	abstract protected DistanceInstance comboNextQuery(); // active learning

	/** Pass a labeled example to the score-combination learner.
	 */
	abstract protected void comboAddExample(DistanceInstance di); // ac

	/** Get the final string distance, which will be based on the distances learned by the
	 * inner learners, as well as the combination scheme learned by comboSetAnswer, comboTrain,
	 * and etc.
	 */
	abstract public StringDistance getDistance();

	//
	// routines for delegating
	//

	/** Pass the training data along to the inner learners. */
	public void setStringWrapperPool(StringWrapperIterator it) 
	{ 
		// train i-th learner on i-th field of string wrapper
		List buffer = asMultiStringWrapperList(it);
		if (buffer.size()==0) throw new IllegalStateException("need some unlabeled strings");
		MultiStringWrapper prototype = (MultiStringWrapper)buffer.get(0);
		for (int i=0; i<prototype.size(); i++) {
			int j = prototype.getDistanceLearnerIndex(i);
			innerLearners[j].setStringWrapperPool( new JthStringWrapperValueIterator( j, buffer.iterator() ));
		}
		comboSetStringWrapperPool( buffer.iterator() );
	}

	/** Pass the training data along to the inner learners. */
	public void setDistanceInstancePool(DistanceInstanceIterator it) 
	{
		// need to save out the i-th field, if it's been prepared.
		List buffer = asMultiDistanceInstanceList(it);
		if (buffer.size()==0) return;
		MyMultiDistanceInstance instance = (MyMultiDistanceInstance)buffer.get(0);
		MultiStringWrapper prototype = asMultiStringWrapper( instance.getA() );
		for (int i=0; i<prototype.size(); i++) {
			int j = prototype.getDistanceLearnerIndex(i);
			innerLearners[j].setDistanceInstancePool( new JthDistanceInstanceIterator( j, buffer.iterator() ) );
		}
		comboSetDistanceInstancePool( buffer.iterator() );
	}

	/** See if someone has a query */
	public boolean hasNextQuery() {
		for (int i=0; i<innerLearners.length; i++) {
			if (innerLearners[i].hasNextQuery()) return true;
		}
		return comboHasNextQuery();
	}

	/** Get a next query from one of the sublearners
	 */
	public DistanceInstance nextQuery() {
		// need to save out the i-th field, if it's been prepared.
		// poll sublearners in random order, to be fair
		// indices [0,innerLearners.length-1] are the inner learners,
		// index innerLearners.length is the comboLearner
		ArrayList indices = new ArrayList(innerLearners.length+1);
		for (int i=0; i<indices.size(); i++) indices.set(i, new Integer(i) );
		Collections.shuffle(indices);
		for (int i=0; i<indices.size(); i++) {
			int k = ((Integer)indices.get(i)).intValue();
			if ( k==innerLearners.length && comboHasNextQuery() ) {
				return comboNextQuery();
			} else if (innerLearners[k].hasNextQuery()) {
				return innerLearners[k].nextQuery();
			}
		}
		throw new IllegalStateException("someone seems to have forgotten they want a query");
	}

	/** Pass new labels to the sublearners. 
	 */
	public void addExample(DistanceInstance answeredQuery) {
		MyMultiDistanceInstance di = asMultiDistanceInstance( answeredQuery );
		for (int i=0; i<innerLearners.length; i++) {
			innerLearners[i].addExample( di.get(i) );
		}
		comboAddExample( di );
	}

	/** Prepare data for the sublearners.
	 */
	public StringWrapperIterator prepare(StringWrapperIterator it) 
	{
		List multiWrappers = asMultiStringWrapperList(it);
		if (multiWrappers.size()==0) return new BasicStringWrapperIterator( Collections.EMPTY_SET.iterator() );
		MultiStringWrapper prototype = (MultiStringWrapper)multiWrappers.get(0);
		for (int i=0; i<prototype.size(); i++) {
			int j = prototype.getDistanceLearnerIndex(i);
			StringDistanceLearner learner = innerLearners[j];
			StringWrapperIterator prepped = learner.prepare( new JthStringWrapperValueIterator( j, multiWrappers.iterator() ) );
			for (int k=0; k<multiWrappers.size(); k++) {
				MultiStringWrapper msw = (MultiStringWrapper)multiWrappers.get(k);
				StringWrapper w = prepped.nextStringWrapper();
				msw.set( i, w );
			}
		}
		return new BasicStringWrapperIterator( multiWrappers.iterator() );
	}

	/** Prepare data for the learners.
	 */
	public DistanceInstanceIterator prepare(DistanceInstanceIterator it) 
	{
		List multiDistances = asMultiDistanceInstanceList(it);
		return new BasicDistanceInstanceIterator( multiDistances.iterator() );
	}

	//
	// support routines for splitting and merging multiple StringWrapper's and DistanceInstance's
	//

	/* lazily convert to a List of MultiStringWrapper list */
	protected List asMultiStringWrapperList(StringWrapperIterator i) {
		List buffer = new ArrayList();
		while (i.hasNext()) {
			StringWrapper w = i.nextStringWrapper();
			MultiStringWrapper mw = asMultiStringWrapper(w);
			buffer.add(mw);
		}
		return buffer;
	}

	/* lazily convert to a MultiStringWrapper */
	protected MultiStringWrapper asMultiStringWrapper(StringWrapper w) 
	{
		if (w instanceof MultiStringWrapper) return (MultiStringWrapper)w;
		else 	{
			MultiStringWrapper mw = new MultiStringWrapper( w.unwrap(), innerLearners.length, delim);
			for (int i=0; i<mw.size(); i++) {
				mw.set(i, prepareForLearner( mw.get(i), innerLearners[mw.getDistanceLearnerIndex(i)] ) );
			}
			return mw;
		}
	}

	/** Prepare a single StringWrapper for a learner */
	private StringWrapper prepareForLearner(StringWrapper w,StringDistanceLearner learner) {
		StringWrapperIterator it = new BasicStringWrapperIterator( Collections.singleton(w).iterator() ); 
		return learner.prepare(it).nextStringWrapper();
	}


	/** Iterate over the j-th field of MultiStringWrapper */
	protected class JthStringWrapperValueIterator implements StringWrapperIterator {
		private Iterator i;
		private int j;
		public JthStringWrapperValueIterator(int j,Iterator i) { this.j=j; this.i=i; }
		public boolean hasNext() { return i.hasNext(); }
		public Object next() { return ((MultiStringWrapper)i.next()).get(j); }
		public StringWrapper nextStringWrapper() { return (StringWrapper) next(); }
		public void remove() { throw new UnsupportedOperationException("can't remove"); }
	}

	/* lazily convert to a List of DistanceInstance's with MultiStringWrapper's in each place */
	protected List asMultiDistanceInstanceList(DistanceInstanceIterator i) {
		List buffer = new ArrayList();
		while (i.hasNext()) {
			DistanceInstance di = i.nextDistanceInstance();
			buffer.add( asMultiDistanceInstance( i.nextDistanceInstance() ) );
		}
		return buffer;
	}

	protected MyMultiDistanceInstance asMultiDistanceInstance(DistanceInstance di) {
		if (di instanceof MyMultiDistanceInstance) return (MyMultiDistanceInstance)di;
		else return new MyMultiDistanceInstance( di.getA(), di.getB(), di.isCorrect(),  di.getDistance());
	}

	protected class MyDistanceInstance implements DistanceInstance {
		protected StringWrapper a,b;
		protected boolean correct;
		protected double distance;
		public MyDistanceInstance(StringWrapper a,StringWrapper b,boolean correct,double distance) {
			this.a = a;
			this.b = b;
			this.correct = correct;
			this.distance = distance;
		}
		public StringWrapper getA() { return a; }
		public StringWrapper getB() { return b; }
		public boolean isCorrect() { return correct; }
		public double getDistance() { return distance; }
		public void setDistance(double distance) { this.distance = distance; }
	}

	protected class MyMultiDistanceInstance extends MyDistanceInstance {
		MultiStringWrapper ma,mb;
		public MyMultiDistanceInstance(StringWrapper a,StringWrapper b,boolean correct,double distance) {
			super(a,b,correct,distance);
			ma = asMultiStringWrapper(a);
			mb = asMultiStringWrapper(b);
		}
		public StringWrapper getA(int j) { return ma.get(j); }
		public StringWrapper getB(int j) { return mb.get(j); }
		public DistanceInstance get(int j) { return new MyDistanceInstance( ma.get(j), mb.get(j), correct, distance); }
		public String toString() { return "["+ma+";"+mb+"]"; }
	}

	/** Iterate over the j-th field of MultiStringWrapper's in a DistanceInstance of MultiStringWrapper's */
	protected class JthDistanceInstanceIterator implements DistanceInstanceIterator {
		private Iterator i;
		private int j;
		public JthDistanceInstanceIterator(int j, Iterator i) { this.j=j; this.i=i; }
		public boolean hasNext() { return i.hasNext(); }
		public DistanceInstance nextDistanceInstance() { return (DistanceInstance)next(); }
		public Object next() { return ((MyMultiDistanceInstance) i.next()).get(j); }
		public void remove() { throw new UnsupportedOperationException("can't remove"); }
	}

	/** Get an array of trained inner distances. */
	protected StringDistance[] getInnerDistances() 
	{
		StringDistance[] innerDistances = new StringDistance[ innerLearners.length ];
		for (int j=0; j<innerLearners.length; j++) {
			innerDistances[j] = innerLearners[j].getDistance();
		}
		return innerDistances;
	}

	/**
	 * Abstract class for combining innerDistances's
	 */
	protected abstract class CombinedStringDistance implements StringDistance
	{
		protected StringDistance[] innerDistances;
		protected MultiStringWrapper prototype;

		public CombinedStringDistance(StringDistance[] innerDistances, MultiStringWrapper prototype) 
		{
			this.innerDistances = innerDistances;
			this.prototype = prototype;
		}

		final public double score(String s, String t) 
		{	
			return score(prepare(s), prepare(t));	
		}

		final public String explainScore(String s, String t) 
		{ 
			return explainScore(prepare(s), prepare(t));	
		}

		final public StringWrapper prepare(String s) 
		{ 
			MultiStringWrapper ms = asMultiStringWrapper(new BasicStringWrapper(s));
			ms.prepare( innerDistances );
			return ms;
		}

		final public double score(StringWrapper s,StringWrapper t) 
		{
			MultiStringWrapper ms = asMultiStringWrapper(s);
			MultiStringWrapper mt = asMultiStringWrapper(t);
			ms.prepare(innerDistances);
			mt.prepare(innerDistances);
			if (ms.size() != mt.size() || ms.size()!=prototype.size()) {
				throw new IllegalStateException("ms,mt="+ms+","+mt+" expected MultiStringWrapper's of size "+prototype.size()); 
			}
			return doScore(ms,mt);
		}
		
		final public String explainScore(StringWrapper s, StringWrapper t) { 
			StringBuffer buf = new StringBuffer();
			MultiStringWrapper ms = asMultiStringWrapper(s);
			MultiStringWrapper mt = asMultiStringWrapper(t);
			if (ms.size() != mt.size() || ms.size()!=prototype.size()) {
				throw new IllegalStateException("expected MultiStringWrapper's of size "+prototype.size()); 
			}
			for (int i=0; i<ms.size(); i++) {
				StringDistance d = innerDistances[ ms.getDistanceLearnerIndex(i) ];
				buf.append("score of "+d+" on '"+ms.get(i)+"' and '"+mt.get(i)+"' = "+d.score( ms.get(i), mt.get(i) )+"\n");
			}
			buf.append( explainCombination(ms, mt) );
			return buf.toString(); 
		}

		/** Produce a score, assuming ms and mt are the correct sizes, and fully prepared. */
		abstract protected double doScore(MultiStringWrapper ms,MultiStringWrapper mt);

		/** Explain how the primitive scores were combined. */
		abstract protected String explainCombination(MultiStringWrapper ms,MultiStringWrapper mt);

		/** Help class for 'toString()' which produces a description of the distances being combined. */
		protected String innerDistanceString() 
		{
			StringBuffer buf = new StringBuffer("");
			for (int i=0; i<innerDistances.length; i++) 
			{
				buf.append(" "+innerDistances[i]);
			}
			return buf.toString();
		}
	}

}
