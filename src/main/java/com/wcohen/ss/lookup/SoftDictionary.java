package com.wcohen.ss.lookup;

import java.io.*;
import java.util.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Looks up nearly-matching strings in a dictionary, using a string distance.
 * 
 * A typical use:
 *<code><pre>
 * SoftDictionary softDict = new SoftDictionary(new SimpleTokenizer(true,true));
 * String alias[] = new String[]{"william cohen", "wwcohen", "einat minkov", "eminkov", .... };
 * for (int i=0; i<alias.length; i++) {
 *    softDict.put( alias[i], null );
 * }
 * String query = "w. cohen";
 * StringWrapper w = (StringWrapper)softDict.lookup( query );
 * String closestMatchToQuery = w.unwrap();
 *</pre></code>
 */

public class SoftDictionary 
{
    private static final boolean DEBUG=false;

    //
    // private data
    //

    // used to learn the distance measure to use in evaluation
    private StringDistanceLearner distanceLearner;
    // distance measure to use in evaluation
    private StringDistance distance;
    // tokenizes stored strings
    private Tokenizer tokenizer;
    // maps tokens to strings containing these tokens
    private Map index;
    // maps stored strings to their associated value
    private Map map;
    // maps stored strings to their associated 'id'
    private Map idMap;
    // count things in map
    private int totalEntries;

    //
    // config options
    //

    // max fraction of total entries an index entry can have to be useful
    private double maxFraction = 1.0;

    //
    // constructors
    //

    public SoftDictionary() 
    {
	this(new JaroWinklerTFIDF(), NGramTokenizer.DEFAULT_TOKENIZER);
    }
    public SoftDictionary(StringDistanceLearner distanceLearner)
    {
	this(distanceLearner, NGramTokenizer.DEFAULT_TOKENIZER);
    }
    public SoftDictionary(Tokenizer tokenizer)
    {
	this(new JaroWinklerTFIDF(), tokenizer);
    }
    public SoftDictionary(StringDistanceLearner distanceLearner,Tokenizer tokenizer) 
    {
	this.distanceLearner = distanceLearner;
	this.distance = null;
	this.tokenizer = tokenizer;
	this.index = new HashMap();
	this.map = new HashMap();
	this.idMap = new HashMap();
	this.totalEntries = 0;
    }
	
    /** Return the number of entries in the dictionary. */
    public int size() 
    { 
	return totalEntries; 
    }

    /** Prepare a string for quicker lookup.
     */
    public StringWrapper prepare(String s)
    {
	return new MyWrapper(s);
    }

    /** Insert all lines in a file as items mapping to themselves.
     */
    public void load(File file) throws IOException,FileNotFoundException
    {
	load(file,false);
    }

    /** Insert all lines in a file as items mapping to themselves.  If
     * 'ids' is true, then make the line number of an item its id.
     *
     *<p>This is mostly for testing the id feature.
     */
    public void load(File file,boolean ids) throws IOException,FileNotFoundException
    {
	LineNumberReader in = new LineNumberReader(new FileReader(file));
	String line;
	while ((line = in.readLine())!=null) {
	    if (ids) put(Integer.toString(in.getLineNumber()), line, line);
	    else put(line,line);
	}
	in.close();
    }

    /** Load a file of identifiers, each of which has multiple
     * aliases. Each line is a list of tab-separated strings, the
     * first of which is the identifier, the remainder of which
     * are aliases.
     */
    public void loadAliases(File file) throws IOException,FileNotFoundException
    {
	LineNumberReader in = new LineNumberReader(new FileReader(file));
	String line;
	while ((line = in.readLine())!=null) {
	    String[] parts = line.split("\\t");
	    for (int j=1; j<parts.length; j++) {
		put( parts[j], parts[j], parts[0] );
	    }
	}
	in.close();
    }

    /** Insert a string into the dictionary.
     *
     * <p>Id is a special tag used to handle 'leave one out'
     * lookups.  If you do a lookup on a string with a non-null
     * id, you get the closest matches that do not have the same
     * id.
     */
    public void put(String id,String string,Object value)
    {
	if (DEBUG && id!=null) System.out.println(id+":"+string+" => "+value);
	put(id, new MyWrapper(string), value);
    }

    /** Insert a string into the dictionary.
     */
    public void put(String string,Object value)
    {
	put((String)null, new MyWrapper(string), value);
    }

    /** Insert a prepared string into the dictionary.
     *
     * <p>Id is a special tag used to handle 'leave one out'
     * lookups.  If you do a lookup on a string with a non-null
     * id, you get the closest matches that do not have the same
     * id.
     */
    public void put(String id, StringWrapper toInsert,Object value)
    {
	MyWrapper wrapper = asMyWrapper(toInsert);
	Token[] tokens = wrapper.getTokens();
	for (int i=0; i<tokens.length; i++) {
	    ArrayList stringsWithToken = (ArrayList) index.get(tokens[i]);
	    if (stringsWithToken==null) index.put( tokens[i], (stringsWithToken=new ArrayList()) );
	    stringsWithToken.add( wrapper );
	}
	map.put( wrapper, value );
	if (id!=null) idMap.put( wrapper, id );
	distance = null; // mark distance as "out of date" 
	totalEntries++;
    }


    // caches result of last 'get'
    private HashSet closeMatches;
    private MyWrapper closestMatch;
    private double distanceToClosestMatch;
    private StringWrapper lastLookup;

    /** Lookup a string in the dictionary, cache result in closeMatches.
     *
     * <p>If id==null, consider any match. If id is non-null, consider
     * only matches to strings that don't have the same id, or that have
     * a null id.
     */
    private void doLookup(String id,StringWrapper toFind)
    {
	// retrain if necessary
	if (distance==null) {
	    distance = new MyTeacher().train( distanceLearner );
	}
	// used cached values if it's ok
	if (lastLookup==toFind) return;

	closeMatches = new HashSet();
	closestMatch = null;
	distanceToClosestMatch = -Double.MAX_VALUE;

	// lookup best match to wrapper
	MyWrapper wrapper = asMyWrapper(toFind);
	Token[] tokens = wrapper.getTokens();
	for (int i=0; i<tokens.length; i++) {
	    ArrayList stringsWithToken = (ArrayList) index.get(tokens[i]);
	    if (stringsWithToken!=null && ((double)stringsWithToken.size()/totalEntries) < maxFraction) {
		for (Iterator j=stringsWithToken.iterator(); j.hasNext(); ) {
		    MyWrapper wj = (MyWrapper)j.next(); 
		    String wjId = (String)idMap.get(wj);
		    //if (DEBUG) System.out.println("id:"+id+" wjId:"+wjId);
		    if (!closeMatches.contains(wj) && (wjId==null || !wjId.equals(id))) {
			double score = distance.score( wrapper.getDistanceWrapper(), wj.getDistanceWrapper() );
			if (DEBUG) System.out.println("score for "+wj+": "+score);
			//if (DEBUG) System.out.println(distance.explainScore(wrapper.getDistanceWrapper(),wj.getDistanceWrapper()));
			closeMatches.add( wj );
			if (score>=distanceToClosestMatch) {
			    //if (DEBUG) System.out.println("closest so far");
			    distanceToClosestMatch = score;
			    closestMatch = wj;
			}
		    }
		}
	    }
	}
	lastLookup = toFind;
    }
	
    /** Lookup a string in the dictionary.
     *
     * <p>If id is non-null, then consider only strings with different ids (or null ids).
     */
    public Object lookup(String id,String toFind)
    {
	return lookup(id,new MyWrapper(toFind));
    }

    /** Lookup a prepared string in the dictionary.
     *
     * <p>If id is non-null, then consider only strings with different ids (or null ids).
     */
    public Object lookup(String id,StringWrapper toFind)
    {
	doLookup(id,toFind);
	return closestMatch;
    }

    /** Return the distance to the best match.
     *
     * <p>If id is non-null, then consider only strings with different ids (or null ids).
     */
    public double lookupDistance(String id,String toFind)
    {
	return lookupDistance(id,new MyWrapper(toFind));
    }

    /** Return the distance to the best match.
     *
     * <p>If id is non-null, then consider only strings with different ids (or null ids).
     */
    public double lookupDistance(String id,StringWrapper toFind)
    {
	doLookup(id,toFind);
	return distanceToClosestMatch;
    }


    /** Lookup a string in the dictionary.
     */
    public Object lookup(String toFind)
    {
	return lookup(null,new MyWrapper(toFind));
    }

    /** Lookup a prepared string in the dictionary.
     */
    public Object lookup(StringWrapper toFind)
    {
	doLookup(null,toFind);
	return closestMatch;
    }

    /** Return the distance to the best match.
     */
    public double lookupDistance(String toFind)
    {
	return lookupDistance(null,new MyWrapper(toFind));
    }

    /** Return the distance to the best match.
     */
    public double lookupDistance(StringWrapper toFind)
    {
	doLookup(null,toFind);
	return distanceToClosestMatch;
    }
	
    /** Return a teacher that can 'train' a distance metric
     * from the information in the dictionary.  Since there are
     * no known distances, this means unsupervised training,
     * e.g. accumulating TFIDF weights, etc.
     */
    public StringDistanceTeacher getTeacher() { return new MyTeacher(); }

    //
    // a tokenized version of the string, plus one prepared for the distance metric
    //
    private class MyWrapper implements StringWrapper {
	private StringWrapper w;
	private Token[] tokens;
	public MyWrapper(String s) 
	{
	    this.w = prepare(s);
	    this.tokens = tokenizer.tokenize(s);
	}
	public String unwrap() { return w.unwrap(); }
	public char charAt(int i)	{	return w.charAt(i);	}
	public int length()	{	return w.length(); }
	public Token[] getTokens() { return tokens; }
	public StringWrapper getDistanceWrapper() { return w; }
	public int hashCode() { return unwrap().hashCode(); }
	public boolean equals(Object o) {
	    if (!(o instanceof MyWrapper)) return false;
	    return ((MyWrapper)o).unwrap().equals( this.unwrap() );
	}
	private StringWrapper prepare(String s) {
	    StringWrapperIterator i = distanceLearner.prepare( 
							      new BasicStringWrapperIterator( Collections.singleton(new BasicStringWrapper(s)).iterator()) );
	    return i.nextStringWrapper();
	}
	public String toString() { return "[SoftDictionaryWrapper '"+unwrap()+"']"; }
    }

    // lazily convert to a MyWrapper
    private MyWrapper asMyWrapper(StringWrapper w)
    {
	if (w instanceof MyWrapper) return (MyWrapper)w;
	else return new MyWrapper(w.unwrap());
    }

    // simple teacher that only supports unsupervised training
    private class MyTeacher extends StringDistanceTeacher {
	protected StringWrapperIterator stringWrapperIterator() {
	    return new BasicStringWrapperIterator(map.keySet().iterator());
	}
	protected DistanceInstanceIterator distanceInstancePool() {
	    return new BasicDistanceInstanceIterator( Collections.EMPTY_SET.iterator() );
	}
	protected DistanceInstanceIterator distanceExamplePool() {
	    return new BasicDistanceInstanceIterator( Collections.EMPTY_SET.iterator() );			
	}
	protected DistanceInstance labelInstance(DistanceInstance distanceInstance) {
	    return null;
	}
	protected boolean hasAnswers() {
	    return false;
	}
    }

    /** Simple main for testing.
     */
    static public void main(String[] argv) throws IOException,FileNotFoundException
    {
	SoftDictionary m = new SoftDictionary();
	System.out.println("loading...");
	m.loadAliases(new File(argv[0]));
	System.out.println("loaded...");
	for (int i=1; i<argv.length; i++) {
	    System.out.println("lookup: "+argv[i]);
	    String[] f = argv[i].split(":");
	    if (f.length==1) {
		System.out.println(argv[i] +" => "+m.lookup(argv[i])+" at "+m.lookupDistance(argv[i]));
	    } else {
		System.out.println(f[1] +" => "+m.lookup(f[0],f[1])+" at "+m.lookupDistance(f[0],f[1]));
	    }
	}
    }
}
