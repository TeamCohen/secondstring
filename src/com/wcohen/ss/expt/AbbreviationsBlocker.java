package com.wcohen.ss.expt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.wcohen.ss.api.Token;
import com.wcohen.ss.api.Tokenizer;
import com.wcohen.ss.tokens.CharacterTokenizer;

/**
 * Produces candidate <short form, long form> pairs that share not-too-common character tokens.
 * Works with the AbbreviationAlignment distance class. 
 * <br>
 * Only works in clusterMode = false.
 * <br><br>
 * Sample command line: <br>
 * <code> java com.wcohen.ss.expt.MatchExpt AbbreviationsBlocker AbbreviationAlignment train/abbvAlign_strings.txt -summarize </code>
 * 
 * @author Dana Movshovitz-Attias
 */
public class AbbreviationsBlocker extends Blocker {
	
	private static double defaultMaxFraction = 1;
	static {
		try {
			String s = System.getProperty("blockerMaxFraction");
			if (s!=null) defaultMaxFraction = Double.parseDouble(s);
		} catch (NumberFormatException e) {
			;
		}
	}
	
	private static final Set<Integer> STOPWORD_TOKEN_MARKER = new HashSet<Integer>();
	
	private ArrayList<Blocker.Pair> pairList;
	private double maxFraction;
	private int numCorrectPairs;
	
	protected Tokenizer tokenizer;
	
	public AbbreviationsBlocker(Tokenizer tokenizer, double maxFraction) {
		this.maxFraction = maxFraction;
		this.clusterMode = false;
		this.tokenizer = tokenizer;
	}
	public AbbreviationsBlocker() {
		this(CharacterTokenizer.DEFAULT_TOKENIZER, defaultMaxFraction);
	}
	public double getMaxFraction() { return maxFraction; }
	public void setMaxFraction(double maxFraction) { this.maxFraction = maxFraction; }

	/* (non-Javadoc)
	 * @see com.wcohen.ss.expt.Blocker#block(com.wcohen.ss.expt.MatchData)
	 */
	@Override
	public void block(MatchData data) {
		numCorrectPairs = countCorrectPairs(data);
		pairList = new ArrayList<Blocker.Pair>();
		if (clusterMode)
			throw new IllegalArgumentException("clusterMode=true is not valid for this blocker");
		
		String sfSource = data.getSource(0);
		String lfSource = data.getSource(1);
		if(!sfSource.equals("short")){
			String tmp = sfSource;
			sfSource = lfSource;
			lfSource = tmp;
		}
		
		// index the smaller source
		double maxSetSize = data.numInstances(sfSource)*maxFraction;
		Map<Token, Set<Integer>> index = new TreeMap<Token, Set<Integer>>();
		for (int i=0; i<data.numInstances(sfSource); i++) {
			Token[] tokens = tokenizer.tokenize( data.getInstance(sfSource,i).unwrap() );
			for (int j=0; j<tokens.length; j++) {
				Set<Integer> containers = index.get(tokens[j]);
				if (containers==STOPWORD_TOKEN_MARKER) {
					/* do nothing */;
				} else if (containers==null) {
					containers = new TreeSet<Integer>();
					index.put(tokens[j], containers);
				} 
				containers.add( new Integer(i) );
				// mark this if it's too full
				if (containers.size() > maxSetSize) {  
					index.put(tokens[j], STOPWORD_TOKEN_MARKER);						
				} 
			}
		}
		//System.out.println("data:\n"+data); showIndex(index);
		// find pairs
		Set<Integer> pairedUpInstances = new TreeSet<Integer>();
		for (int i=0; i<data.numInstances(lfSource); i++) {
			MatchData.Instance lfInst = data.getInstance(lfSource,i);
			pairedUpInstances.clear();
			Token[] tokens = tokenizer.tokenize( lfInst.unwrap() );			
			for (int j=0; j<tokens.length; j++) {			
				Set<Integer> containers = index.get( tokens[j] );
				if (containers!=null && containers!=STOPWORD_TOKEN_MARKER) {
					for (Iterator<Integer> k=containers.iterator(); k.hasNext(); ) {
						Integer smallIndexInteger = (Integer)k.next();
						int smallIndex = smallIndexInteger.intValue();
						if (!pairedUpInstances.contains(smallIndexInteger) && 
								(sfSource!=lfSource || smallIndex>i))
						{
							MatchData.Instance sfInst = data.getInstance(sfSource, smallIndex);
							// Matching of short-form --> long-form is directional. 
							pairList.add( new Blocker.Pair( sfInst, lfInst, sfInst.sameId(lfInst) ));
							pairedUpInstances.add( smallIndexInteger );
						}
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.wcohen.ss.expt.Blocker#size()
	 */
	@Override
	public int size() { return pairList.size();  }
	
	/* (non-Javadoc)
	 * @see com.wcohen.ss.expt.Blocker#getPair(int)
	 */
	@Override
	public Pair getPair(int i) { return (Pair)pairList.get(i); }
	
	public String toString() { return "[AbbreviationsBlocker:maxFraction="+maxFraction+"]"; }
	
	/* (non-Javadoc)
	 * @see com.wcohen.ss.expt.Blocker#numCorrectPairs()
	 */
	@Override
	public int numCorrectPairs() { return numCorrectPairs; }

}
