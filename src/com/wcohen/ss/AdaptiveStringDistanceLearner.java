package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.util.*;
import com.wcohen.cls.*;
import com.wcohen.cls.linear.*;

/**
 * Abstract StringDistanceLearner class which averages results of a number of
 * inner distance metrics, learned by a number of inner distance learners.
 */

public class AdaptiveStringDistanceLearner extends CombinedStringDistanceLearner
{
	private MultiStringWrapper prototype = null;
	private BinaryClassifierLearner comboLearner = null;
	private Iterator distanceInstanceIterator = null;
	
	public AdaptiveStringDistanceLearner() 
	{ 
		super(); 
		//comboLearner = new VotedPerceptron();
		comboLearner = new NaiveBayes();
	}

	public AdaptiveStringDistanceLearner(BinaryClassifierLearner comboLearner) 
	{ 
		super(); 
		this.comboLearner = comboLearner;
	}

	public 
	AdaptiveStringDistanceLearner(StringDistanceLearner[] innerLearners, String delim,BinaryClassifierLearner comboLearner) 
	{	
		super(innerLearners,delim); 
		this.comboLearner = comboLearner;
	}

	protected void comboSetStringWrapperPool(Iterator it) 
	{
		// save a prototypical example, so we know how many fields there are
		if (it.hasNext()) prototype = asMultiStringWrapper( (StringWrapper) it.next() );
	}

	protected void comboSetDistanceInstancePool(Iterator it)
	{
		distanceInstanceIterator = it;
		//System.out.println("distanceInstanceIterator = "+distanceInstanceIterator);
	}

	protected boolean comboHasNextQuery() 
	{ 
		return false;
	}

	protected DistanceInstance comboNextQuery() 
	{ 
		return null;
	}

	protected void comboAddExample(DistanceInstance distanceInstance) 
	{ 
		MyMultiDistanceInstance di = (MyMultiDistanceInstance)distanceInstance;
		//System.out.println("adding example "+di);
		MutableInstance instance = new MutableInstance(di); 
		StringDistance[] innerDistances = getInnerDistances();
		for (int i=0; i<prototype.size(); i++) {
			StringWrapper a = di.getA(i);
			StringWrapper b = di.getB(i);
			int j = prototype.getDistanceLearnerIndex(i); 
			Feature ithFeature = 
				Feature.Factory.getFeature(new String[] { "dist#"+j, "field#"+prototype.getFieldIndex(i) });
			double ithScore = innerDistances[j].score(a,b);
			//System.out.println("slot "+i+" a,b='"+a+"','"+b+" distance="+innerDistances[j]+" score="+ithScore);
			instance.addNumeric( ithFeature, ithScore );
		}
		double label = di.isCorrect() ? +1 : -1;
		//System.out.println("instance is: "+instance);
		comboLearner.addExample( new BinaryExample(instance, label) );
	}

	public StringDistance getDistance() 
	{
		if (prototype==null) throw new IllegalStateException("need to be trained first");
		return new ClassifiedStringDistance(getInnerDistances(), prototype, comboLearner.getBinaryClassifier());
	}

	//
	// average of some string distances
	//

	private class ClassifiedStringDistance extends CombinedStringDistance
	{
		private BinaryClassifier classifier;

		public ClassifiedStringDistance(
			StringDistance[] innerDistances, 
			MultiStringWrapper prototype,
			BinaryClassifier classifier) 
		{
			super(innerDistances,prototype);
			this.classifier = classifier;
			//System.out.println("classifier is "+classifier);
		}

		protected double doScore(MultiStringWrapper ms,MultiStringWrapper mt)
		{
			return classifier.score( asComboInstance(ms,mt) );
		}
	
		public String explainCombination(MultiStringWrapper ms,MultiStringWrapper mt) 
		{ 
			StringBuffer buf = new StringBuffer();
			buf.append("Classifier: "+classifier);
			buf.append("Classifier score: "+classifier.explain(asComboInstance(ms,mt)));
			return buf.toString();
		}

		private Instance asComboInstance(MultiStringWrapper ms,MultiStringWrapper mt)
		{	
			MutableInstance instance = new MutableInstance(ms.unwrap()+" ~ "+mt.unwrap()); 
			for (int i=0; i<ms.size(); i++) {
				StringWrapper a = ms.get(i);
				StringWrapper b = mt.get(i);
				int j = prototype.getDistanceLearnerIndex(i); 
				Feature ithFeature = 
					Feature.Factory.getFeature(new String[] { "dist#"+j, "field#"+prototype.getFieldIndex(i) });
				double ithScore = innerDistances[j].score(a,b);
				instance.addNumeric( ithFeature, ithScore );
			}
			return instance;
		}

		public String toString() 
		{
			return "[ClassifiedStringDistance: "+classifier+";"+innerDistanceString()+"]";
		}
	}

}
