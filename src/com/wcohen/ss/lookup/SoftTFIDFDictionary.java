package com.wcohen.ss.lookup;

import java.io.*;
import java.util.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Looks up nearly-matching strings in a dictionary, using SoftTFIDF
 * distance.  To use the dictionary, first load in string/value pairs
 * using 'put'.  Then 'freeze' the dictionary.  After the dictionary
 * is frozen, you can lookup values with lookup and getResult(i),
 * getValue(i), etc.
 *
 * <p>For example:
 * <code><pre>
 * SoftTFIDFDictionary dict = new SoftTFIDFDictionary();
 * dict.put("william cohen", "wcohen@cs.cmu.edu");
 * dict.put("vitor del rocha carvalho", "vitor@cs.cmu.edu");
 * ...
 * dict.freeze();
 * int n=dict.lookup("victor carvalho");
 * for (int i=0; i<n; i++) {
 *    System.out.println("value associated with '"+dict.getResult(i)+"' is "+dict.getValue(i));
 *    System.out.println("similarity of '"+dict.getResult(i)+"' to query string is "+dict.getScore(i));
 * }
 * </pre></code>
 * 
 */

public class SoftTFIDFDictionary implements FastLookup
{
    private static final boolean DEBUG=false;
    private static final int DEFAULT_WINDOW_SIZE=100;
    private static final double DEFAULT_MIN_TOKEN_SIMILARITY=0.9;
    private static final int DEFAULT_MAX_INVERTED_INDEX_SIZE=0;
    private static final Tokenizer DEFAULT_TOKENIZER=new SimpleTokenizer(false,true);
    private static final Comparator LEXICAL_ORDER_FOR_TOKENS = new Comparator() {
            public int compare(Object a,Object b) {
                return ((Token)a).getValue().compareTo(((Token)b).getValue());
            } 
        };
    private static final Comparator ID_ORDER_FOR_TOKENS = new Comparator() {
            public int compare(Object a,Object b) {
                return ((Token)a).getIndex() - ((Token)b).getIndex();
            } 
        };

    //
    // local information
    // 

    // minTokenSimilarity and tokenizer are used to define a softTFIDF string distance
    private double minTokenSimilarity;
    private Tokenizer tokenizer;
    private SoftTFIDF softTFIDFDistance;
    // the tfidfDistance is used to compute upper bounds on the score associated with
    // a particular token, for pruning
    private TFIDF tfidfDistance;
    // the jaroWinklerDistance is the inner similarity metric used in the softTFIDFDistance,
    // and is used for precomputing pairs of similar tokens
    private JaroWinkler jaroWinklerDistance;
    // windowSize is used for pruning the pairs of tokens for which jaroWinklerDistance
    // will be pre-computed.
    private int windowSize;
    // maxInvertedIndexSize limits size of an inverted index that is followed
    private int maxInvertedIndexSize;

    // the dictionary itself - map a dictionary string to a set of values
    private Map valueMap = new HashMap();
    // flag which indicates if this dictionary is 'frozen' 
    private boolean frozen = false;
    //
    // after freezing, these things are pre-computed to make lookup faster
    //
    // map a token to the highest TFIDF score it has for any string in the dictionary
    private double[] maxTFIDFScore;
    // map a token index to all highly-similar token id's
    Token[][] similarTokens;
    // map a token index to all strings that contain it
    Set[] invertedIndex;
    // all tokens, in alphabetic order
    Token[] allTokens;
    // number of tokens in allTokens
    int numTokens; 

    //
    // file i/o
    //

    public void saveAs(File file) throws IOException,FileNotFoundException
    {
        freeze();

	ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

        // save the parameters of the SoftTFIDFDictionary
        if (tokenizer!=DEFAULT_TOKENIZER) throw new IllegalStateException("can't save a non-default tokenizer");
        out.writeDouble(minTokenSimilarity);
        out.writeInt(windowSize);
        out.writeInt(maxInvertedIndexSize);

        // save the valueMap
        if (DEBUG) System.out.println("saving valueMap...");
        //if (DEBUG) showValueMap();
        out.writeInt(valueMap.entrySet().size());
        for (Iterator i=valueMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            out.writeObject( e.getKey() );
            out.writeObject( e.getValue() );
        }

        // save numTokens, allTokens 
        if (DEBUG) System.out.println("saving allTokens...");
        //if (DEBUG) showAllTokens();
        out.writeInt( numTokens );
        Arrays.sort( allTokens, ID_ORDER_FOR_TOKENS );
        for (int i=0; i<numTokens; i++) {
            out.writeObject( allTokens[i].getValue() );
        }
        Arrays.sort( allTokens, LEXICAL_ORDER_FOR_TOKENS );

        if (DEBUG) System.out.println("saving df's...");
        for (int i=0; i<numTokens; i++) {
            out.writeInt( tfidfDistance.getDocumentFrequency(allTokens[i]) );
        }
        out.writeInt( tfidfDistance.getCollectionSize() );

        if (DEBUG) System.out.println("saving maxTFIDFScore...");
        // save maxTFIDFScore
        for (int i=0; i<numTokens; i++) {
            Token toki = allTokens[i];
            out.writeDouble( maxTFIDFScore[toki.getIndex()] );
        }
        //if (DEBUG) showAllMaxScores();

        // save similarTokens
        if (DEBUG) System.out.println("saving similarTokens...");
        for (int i=0; i<numTokens; i++) {
            Token toki = allTokens[i];
            int n = similarTokens[ toki.getIndex() ].length;
            out.writeInt( n );
            for (int j=0; j<n; j++) {
                out.writeObject( similarTokens[toki.getIndex()][j].getValue() );
            }
        }

        // save invertedIndex
        if (DEBUG) System.out.println("saving invertedIndex...");
        for (int i=0; i<numTokens; i++) {        
            Token toki = allTokens[i];
            Set ii = invertedIndex[toki.getIndex()];
            out.writeInt( ii.size() );
            for (Iterator j=ii.iterator(); j.hasNext(); ) {
                String s = (String)j.next();
                out.writeObject( s );
            }
        }
        
        out.close();
    }

    static public SoftTFIDFDictionary restore(File file) throws IOException,FileNotFoundException
    {
        try {
            return doRestore(file);
        } catch (ClassNotFoundException ex) {
            throw new IOException("improperly format SoftTFIDFDictionary file:"+ex);
        }
    }

    static private SoftTFIDFDictionary doRestore(File file) throws IOException,FileNotFoundException,ClassNotFoundException
    {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));

        // read the parameters of the SoftTFIDFDictionary
        double mts = in.readDouble();
        int ws = in.readInt();
        int miis = in.readInt();
        SoftTFIDFDictionary dict = new SoftTFIDFDictionary(DEFAULT_TOKENIZER,mts,ws,miis);

        // read the valueMap
        if (DEBUG) System.out.println("restoring valueMap...");
        dict.valueMap = new HashMap();
        int v = in.readInt();
        for (int i=0; i<v; i++) {
            String key = (String)in.readObject();
            Object value = in.readObject();
            dict.valueMap.put( key, value );
        }

        // read numTokens, allTokens
        if (DEBUG) System.out.println("restoring allTokens...");
        dict.numTokens = in.readInt();
        dict.allTokens = new Token[dict.numTokens];
        for (int i=0; i<dict.numTokens; i++) {
            String tokenValue = (String)in.readObject();
            dict.allTokens[i] = dict.tokenizer.intern( tokenValue );
        }
        Arrays.sort( dict.allTokens, LEXICAL_ORDER_FOR_TOKENS );
        //if (DEBUG) dict.showAllTokens();
        
        if (DEBUG) System.out.println("restoring df's...");
        for (int i=0; i<dict.numTokens; i++) {
            int df = in.readInt();
            dict.tfidfDistance.setDocumentFrequency( dict.allTokens[i], df );
            dict.softTFIDFDistance.setDocumentFrequency( dict.allTokens[i], df );
        }
        int cs = in.readInt();
        dict.tfidfDistance.setCollectionSize( cs );
        dict.softTFIDFDistance.setCollectionSize( cs );

        // read maxTFIDFScore
        if (DEBUG) System.out.println("restoring maxTFIDFScore...");
        dict.maxTFIDFScore = new double[ dict.tokenizer.maxTokenIndex()+1 ];
        for (int i=0; i<dict.numTokens; i++) {
            Token toki = dict.allTokens[i];            
            dict.maxTFIDFScore[ toki.getIndex() ] = in.readDouble();
        }
        //if (DEBUG) dict.showAllMaxScores();

        // read similarTokens
        if (DEBUG) System.out.println("restoring similarTokens...");
        dict.similarTokens = new Token[dict.tokenizer.maxTokenIndex()+1][];
        for (int i=0; i<dict.numTokens; i++) {
            Token toki = dict.allTokens[i];
            int n = in.readInt();
            dict.similarTokens[ toki.getIndex() ] = new Token[n];
            for (int j=0; j<n; j++) {
                String tokenValue = (String)in.readObject();
                dict.similarTokens[toki.getIndex()][j] = dict.tokenizer.intern( tokenValue );
            }
        }

        // read the invertedIndex
        if (DEBUG) System.out.println("restoring invertedIndex...");        
        dict.invertedIndex = new Set[dict.tokenizer.maxTokenIndex()+1];
        for (int i=0; i<dict.numTokens; i++) {        
            Token toki = dict.allTokens[i];
            dict.invertedIndex[toki.getIndex()] = new HashSet();
            int n = in.readInt();
            for (int j=0; j<n; j++) {
                String s = (String)in.readObject();
                dict.invertedIndex[toki.getIndex()].add( s );
            }
        }
        in.close();

        dict.frozen = true;
        return dict;
    }

    private void showValueMap() 
    { 
        System.out.println("valueMap: "+valueMap); 
    }
    private void showAllTokens() 
    { 
        for (int i=0; i<numTokens; i++) {
            System.out.println("allTokens["+i+"] = "+allTokens[i]);
        }
    }
    private void showAllMaxScores() 
    { 
        for (int i=0; i<numTokens; i++) {
            System.out.println("allTokens["+i+"] = "+allTokens[i]+" maxscore = "+maxTFIDFScore[i]);
        }
    }

    //
    // constructors
    //

    public SoftTFIDFDictionary() 
    { 
        this(DEFAULT_TOKENIZER,DEFAULT_MIN_TOKEN_SIMILARITY,DEFAULT_WINDOW_SIZE,DEFAULT_MAX_INVERTED_INDEX_SIZE); 
    }
    public SoftTFIDFDictionary(Tokenizer tokenizer) 
    { 
        this(tokenizer,DEFAULT_MIN_TOKEN_SIMILARITY,DEFAULT_WINDOW_SIZE,DEFAULT_MAX_INVERTED_INDEX_SIZE); 
    }
    public SoftTFIDFDictionary(Tokenizer tokenizer,double minTokenSimilarity) 
    { 
        this(tokenizer,minTokenSimilarity,DEFAULT_WINDOW_SIZE,DEFAULT_MAX_INVERTED_INDEX_SIZE); 
    }

    /**
     * Create a new SoftTFIDFDictionary. The distance is defined by a
     * SoftTFIDF distance function where minTokenSimilarity is the minimum
     * Jaro-Winkler distance between similar tokens, and the tokenizer
     * defines the tokens considered.
     */
    public SoftTFIDFDictionary(Tokenizer tokenizer,double minTokenSimilarity,int windowSize,int maxInvertedIndexSize)
    {
        this.tokenizer = tokenizer;
        this.minTokenSimilarity = minTokenSimilarity;
        this.windowSize = windowSize;
        this.maxInvertedIndexSize = maxInvertedIndexSize;
        tfidfDistance = new TFIDF(tokenizer);
        jaroWinklerDistance = new JaroWinkler();
        softTFIDFDistance = new SoftTFIDF(tokenizer,jaroWinklerDistance,minTokenSimilarity);
    }

    /** Set the 'windowSize' used for finding similar tokens.  When
     * finding tokens t2 that are similar to a given t1, the
     * dictionary limits itself to tokens t3 that are within
     * distance 'windowSize' of t1 on a sorted list of all tokens
     * in the dictionary
     */
    public void setWindowSize(int w) { this.windowSize=w; }
    public int getWindowSize(int w) { return windowSize; }

    /** Set the maximum size of an inverted index that will be
     * followed.  If this is zero (the default) then any inverted
     * index will be followed, even for very frequent tokens, if
     * following it is justified by the upper bound algorithms.
     */
    public void setMaxInvertedIndexSize(int m) { maxInvertedIndexSize=m; }
    public int getMaxInvertedIndexSize() { return maxInvertedIndexSize; }


    /** Load a file of identifiers, each of which has multiple
     * aliases. The dictionary constructed will map aliases to
     * identifiers.  Each line in the file is a list of tab-separated
     * strings, the first of which is the identifier, the remainder of
     * which are aliases.
     */
    public void loadAliases(File file) throws IOException,FileNotFoundException
    {
	LineNumberReader in = new LineNumberReader(new FileReader(file));
	String line;
	while ((line = in.readLine())!=null) {
	    String[] parts = line.split("\\t");
	    for (int j=1; j<parts.length; j++) {
		put( parts[j], parts[0] );
	    }
	}
	in.close();
    }

    /** Insert a string into the dictionary, and associate it with the
     * given value.
     *
     */
    public void put(String string,Object value)
    {
        if (frozen) throw new IllegalStateException("can't add new values to a frozen dictionary");
        Set valset = (Set)valueMap.get(string);
        if (valset==null) valueMap.put(string, (valset=new HashSet()));
        valset.add( value );
    }

    public void refreeze()
    {
        frozen = false;
        freeze();
    }

    /** Make it impossible to add new values, but possible to perform lookups. 
     */
    public void freeze()
    {
        if (frozen) return;

        // train the TFIDF distance on all strings seen
        trainDistances();

        // now, compute the maxScore of each token, and create an inverted index
        if (DEBUG) System.out.println("computing maxScore of "+tokenizer.maxTokenIndex()+" tokens");
        invertedIndex = new Set[ tokenizer.maxTokenIndex()+1 ];
        maxTFIDFScore = new double[ tokenizer.maxTokenIndex()+1 ];
        for (Iterator i=valueMap.keySet().iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            tfidfDistance.prepare( s );
            Token[] tokens = tfidfDistance.getTokens();
            for (int j=0; j<tokens.length; j++) {
                Token tok = tokens[j];
                double w = tfidfDistance.getWeight( tok );
                maxTFIDFScore[ tok.getIndex() ] = Math.max( maxTFIDFScore[tok.getIndex()], w );
                Set ii = invertedIndex[ tok.getIndex() ];
                if (ii==null) ii = invertedIndex[ tok.getIndex() ] = new HashSet();
                ii.add( s );
                //if (DEBUG) System.out.println("adjust maxscore, invertedIndex for "+tok);
            }
        }

        // find out which tokens are similar to which other tokens
        if (DEBUG) System.out.println("computing similar-tokens for "+tokenizer.maxTokenIndex()+" tokens, window="+windowSize);
        allTokens = new Token[tokenizer.maxTokenIndex()] ;
        numTokens=0;
        for (Iterator i=tokenizer.tokenIterator(); i.hasNext(); ) {        
            Token toki = (Token)i.next();
            allTokens[numTokens++] = toki;
        }
        Arrays.sort(allTokens,LEXICAL_ORDER_FOR_TOKENS);

        similarTokens = new Token[tokenizer.maxTokenIndex()+1][];

        for (int i=0; i<numTokens; i++) {
            Token toki = allTokens[i];
            Set likeTokI = findSimilarTokens( toki.getValue(), i );
            similarTokens[ toki.getIndex() ] = new Token[ likeTokI.size() ];
            int k = 0;
            for (Iterator j=likeTokI.iterator(); j.hasNext(); ) {
                Token tokj = (Token)j.next();
                similarTokens[toki.getIndex()][ k++ ] = tokj;
            }
        }
        
        frozen = true;
    }

    private void trainDistances()
    {
        long start = System.currentTimeMillis();
        List accum = new ArrayList( valueMap.keySet().size() );
        for (Iterator i=valueMap.keySet().iterator(); i.hasNext(); ) {
            String s = (String)i.next();
            accum.add( tfidfDistance.prepare( s ) );
        }
        double elapsedSec1 = (System.currentTimeMillis()-start) / 1000.0;        
        tfidfDistance.train( new BasicStringWrapperIterator(accum.iterator()) );
        softTFIDFDistance.train( new BasicStringWrapperIterator(accum.iterator()) );
        double elapsedSec2 = (System.currentTimeMillis()-start) / 1000.0;        
        if (DEBUG) System.out.println("training took: "+elapsedSec2+" (i.e. "+elapsedSec1+" then "+(elapsedSec2-elapsedSec1)+") sec");
    }

    // find all tokens similar to the given string s, where i is the
    // position of s in the list 'allTokens'
    private Set findSimilarTokens(String s,int i)
    {
        Set likeTokI = new HashSet();
        for (int j=Math.max(0,i-windowSize); j<Math.min(i+windowSize,numTokens); j++) {
            if (i!=j) {
                Token tokj = allTokens[j];
                double d = jaroWinklerDistance.score( s, tokj.getValue() );
                if (d>=minTokenSimilarity) likeTokI.add( tokj );
            }
        }
        return likeTokI;
    }

    // stores items returned from 'lookup'
    private List result;
    // saves lookup time
    protected double lookupTime;

    /** Exactly like lookup, but works by exhaustively checking every stored string.
     */
    public int slowLookup(double minScore,String toFind)
    {
        if (!frozen) freeze();
        long start = System.currentTimeMillis();
        StringWrapper wa = softTFIDFDistance.prepare( toFind );
        result = new ArrayList();
        for (Iterator i=valueMap.keySet().iterator(); i.hasNext(); ) {
            String found = (String)i.next();
            StringWrapper wb = softTFIDFDistance.prepare( found );
            double d = softTFIDFDistance.score( wa, wb );
            if (d>=minScore) {
                Set valset = (Set)valueMap.get(found);
                for (Iterator j=valset.iterator(); j.hasNext(); ) {
                    String valj=(String)j.next();
                    result.add(new LookupResult(found,valj,d));
                }
            }
        }
        Collections.sort( result );
        lookupTime = (System.currentTimeMillis()-start) / 1000.0;        
        return result.size();
    }

    /** Lookup items SoftTFIDF-similar to the 'toFind' argument, and
     * return the number of items found.  The looked-up items must
     * have a similarity score greater than minScore.
     */
    public int lookup(double minScore,String toFind)
    {
        if (!frozen) freeze();
        long start = System.currentTimeMillis();

        final Map upperBoundOnWeight = new HashMap();
        // find all tokens that could be potentially useful for
        // retrieving similar strings
        tfidfDistance.prepare( toFind );
        Token[] tokens = tfidfDistance.getTokens();
        List usefulTokens = new ArrayList(tokens.length);
        for (int i=0; i<tokens.length; i++) {
            Token tok = tokens[i];
            if (DEBUG) System.out.println("upper-bounding token "+i+"="+tok);
            // getIndex()<maxTFIDFScore then tok is somewhere in the dictionary
            if (tok.getIndex() < maxTFIDFScore.length) { 
                // token should be in allTokens and similar tokens should be pre-computed
                storeUpperBound( tok, tok, usefulTokens, upperBoundOnWeight, 1.0 );
                for (int j=0; j<similarTokens[tok.getIndex()].length; j++) {
                    Token simTok = similarTokens[tok.getIndex()][j];
                    double sim = jaroWinklerDistance.score(tok.getValue(), simTok.getValue());
                    storeUpperBound( tok, simTok, usefulTokens, upperBoundOnWeight, sim );
                }
            } else {
                // token should NOT be in allTokens, so we need to computed similarTokens on-the-fly
                int indexInAllTokens = Arrays.binarySearch( allTokens, tok, LEXICAL_ORDER_FOR_TOKENS );
                if (indexInAllTokens<0) indexInAllTokens = -(indexInAllTokens+1);
                Set likeTokI = findSimilarTokens( tok.getValue(), indexInAllTokens );   
                if (DEBUG) System.out.println("just found "+likeTokI.size()+" tokens similar to the novel token "+tok);
                for (Iterator j=likeTokI.iterator(); j.hasNext(); ) {
                    Token simTok = (Token)j.next();
                    double sim = jaroWinklerDistance.score(tok.getValue(), simTok.getValue());
                    storeUpperBound( tok, simTok, usefulTokens, upperBoundOnWeight, sim );
                }
            }
        }
        if (DEBUG) System.out.println("tokens and upper bounds: "+upperBoundOnWeight);

        // collect all candidates, but skip the lowest-scoring
        // "usefulTokens" - scores that add up to 1-minScore
        Set candidates = new HashSet();
        Collections.sort(usefulTokens, new Comparator() {
                public int compare(Object a,Object b) {
                    Double da = (Double)upperBoundOnWeight.get(a);
                    Double db = (Double)upperBoundOnWeight.get(b);
                    //if (da==null) da = new Double(0); if (db==null) db = new Double(0);
                    double diff = da.doubleValue()-db.doubleValue();
                    return diff>0 ? +1 : (diff<0? -1 : 0);
                }
            });
        double totScore = 0;
        for (Iterator i=usefulTokens.iterator(); i.hasNext(); ) {
            Token tok = (Token)i.next();
            Double ub = (Double)upperBoundOnWeight.get(tok); 
            if (ub!=null) totScore += ub.doubleValue();
            if (totScore >= minScore) {
                Set ii = invertedIndex[tok.getIndex()];
                if (maxInvertedIndexSize<=0 || ii.size()<maxInvertedIndexSize) {
                    candidates.addAll( ii  );
                }
            } else {
                if (DEBUG) System.out.println("skip tok "+tok+" upper bound "+ub+" totScore = "+totScore);
            }
            if (DEBUG) System.out.println("after "+tok+" with upper bound "+ub+": "+candidates.size()+" candidates");
        }
        
        // finally collect and score the candidates        
        result = new ArrayList( candidates.size() );
        StringWrapper wa = softTFIDFDistance.prepare( toFind );
        for (Iterator i=candidates.iterator(); i.hasNext(); ) {
            String found = (String)i.next();
            StringWrapper wb = softTFIDFDistance.prepare( found );
            double d = softTFIDFDistance.score( wa, wb );
            //if (DEBUG) System.out.println("candidate "+found+" score "+d+" minscore "+minScore);
            if (d>=minScore) {
                Set valset = (Set)valueMap.get(found);
                for (Iterator j=valset.iterator(); j.hasNext(); ) {
                    String valj=(String)j.next();
                    result.add(new LookupResult(found,valj,d));
                }
            }
        }
        if (DEBUG) System.out.println("result="+result);
        Collections.sort( result );
        lookupTime = (System.currentTimeMillis()-start) / 1000.0;        
        return result.size();
    }
    // subroutine of lookup
    private void storeUpperBound(Token tok, Token simTok, List usefulTokens, Map upperBoundOnWeight, double sim)
    {
        double upperBound = tfidfDistance.getWeight(tok)*maxTFIDFScore[simTok.getIndex()]*sim;
        if (DEBUG) System.out.println("upper-bounding tok "+simTok+" sim="+sim+" to "+tok+" upperBound "+upperBound);
        if (DEBUG) System.out.println("upperBound = "+tfidfDistance.getWeight(tok)+"*"+maxTFIDFScore[simTok.getIndex()]+"*"+sim);
        usefulTokens.add( simTok );
        upperBoundOnWeight.put( simTok, new Double(upperBound) );
    }

    /** Get the i'th string found by the last lookup */
    public String getResult(int i) { return ((LookupResult)result.get(i)).found; }

    /** Get the value of the i'th string found by the last lookup */
    public Object getValue(int i) { return ((LookupResult)result.get(i)).value; }

    /** Get the score of the i'th string found by the last lookup */
    public double getScore(int i) { return ((LookupResult)result.get(i)).score; }

    /** Get the time used in performing the lookup */
    public double getLookupTime() { return lookupTime; }

    // for debug
    private void showLookup(int n)
    {
        for (int i=0; i<n; i++) {
            System.out.println( result.get(i) );
        }
    }
    private void showSimilarTokens()
    {
        for (int i=0; i<numTokens; i++) {
            Token toki = allTokens[i];
            System.out.print(toki+"\t~");
            if (similarTokens[toki.getIndex()]==null) {
                System.out.print(" NULL");
            } else {
                for (int j=0; j<similarTokens[toki.getIndex()].length; j++) {
                    Token tokj = similarTokens[toki.getIndex()][j];
                    System.out.print(" "+tokj.getValue());
                }
            }
            System.out.println();
        }
    }

    private double getNumberOfSimilarTokenPairs()
    {
        double tot = 0;
        for (int i=0; i<numTokens; i++) {
            Token toki = allTokens[i];
            tot += similarTokens[toki.getIndex()].length; 
        }
        return tot;
    }

    /** Simple main for testing and experimentation
     */
    static public void main(String[] argv) throws IOException,FileNotFoundException,NumberFormatException,ClassNotFoundException
    {
        if (argv.length==0) {
            System.out.println("usage 1: aliasfile threshold query1 query2 ... - run queries");
            System.out.println("usage 2: aliasfile threshold queryfile - run queries from a file");
            System.out.println("usage 3: aliasfile window1 window2 .... - explore different window sizes");
            System.out.println("usage 4: aliasfile savefile - convert to fast-loading savefile");
            System.out.println("usage 4: aliasfile - print some stats");
            System.exit(0);
        }

        SoftTFIDFDictionary dict = loadSomehow(argv[0]);
        if (argv.length==1) {
            System.out.println("inverted index sizes:");
            for (int i=0; i<dict.numTokens; i++) {        
                Token toki = dict.allTokens[i];
                Set ii = dict.invertedIndex[toki.getIndex()];
                System.out.println(ii.size()+" "+toki.getValue());
            }
        }  else if (argv.length==2) {
            // aliasfile savefile
            System.out.println("saving...");
            dict.saveAs(new File(argv[1]));
        } else {
            double d = Double.parseDouble(argv[1]);
            if (d<=1) {
                // aliasfile threshold ....
                if (argv.length==3 && new File(argv[2]).exists()) {
                    // aliasfile threshold queryfile
                    LineNumberReader in = new LineNumberReader(new FileReader(new File(argv[2])));
                    String line;
                    // store fast time, slow time, fast values, slow values, #times[fastValues=slowValues]
                    double[] stats = new double[5];
                    int numQueries = 0;
                    while ((line = in.readLine())!=null) {
                        doLookup(dict,d,line,true,stats);
                        numQueries++;
                    }
                    System.out.println("optimized time:   "+stats[0]);
                    System.out.println("baseline time:    "+stats[1]);
                    System.out.println("speedup:          "+stats[1]/stats[0]);
                    System.out.println("optimized values: "+stats[2]);
                    System.out.println("baseline values:  "+stats[3]);
                    System.out.println("percent complete: "+stats[4]/numQueries);
                } else {
                    // aliasfile threshold query1 query2 ....
                    for (int i=2; i<argv.length; i++) {
                        doLookup(dict,d,argv[i],false,null);
                    }
                }
            } else {
                // aliasfile window1 window2....
                dict.setWindowSize(2); // for a quick load
                System.out.println("loading...");
                dict.loadAliases(new File(argv[0]));
                System.out.println("loaded "+dict.numTokens+" tokens");
                System.out.println( "window" +"\t"+ "time" +"\t"+ "#pairs" +"\t"+ "pairs/token");
                java.text.DecimalFormat fmt = new java.text.DecimalFormat("0.000"); 
                for (int i=1; i<argv.length; i++) {
                    int w = Integer.parseInt(argv[i]);
                    dict.setWindowSize( w );
                    long start = System.currentTimeMillis();
                    dict.refreeze();
                    double elapsedSec = (System.currentTimeMillis()-start) / 1000.0;
                    double tot = dict.getNumberOfSimilarTokenPairs();
                    System.out.println( w +"\t"+ fmt.format(elapsedSec) +"\t"+ tot +"\t"+ fmt.format(tot/dict.numTokens)  );
                }
            }
        }
    }
    // for testing
    static private SoftTFIDFDictionary loadSomehow(String fileName) throws IOException,ClassNotFoundException
    {
        SoftTFIDFDictionary dict = null;
        long start0 = System.currentTimeMillis();
        if (fileName.endsWith(".list")) {
            System.out.println("loading aliases...");
            dict = new SoftTFIDFDictionary();
            dict.loadAliases(new File(fileName));
        } else {
            System.out.println("restoring...");
            dict = restore(new File(fileName));
        }
        double elapsedSec0 = (System.currentTimeMillis()-start0) / 1000.0;
        System.out.println("loaded in "+elapsedSec0+" sec");
        long start1 = System.currentTimeMillis();
        System.out.println("freezing...");
        dict.freeze();
        double elapsedSec1 = (System.currentTimeMillis()-start1) / 1000.0;
        System.out.println("frozen in "+elapsedSec1+" sec");
        System.out.println("total i/o "+(elapsedSec1+elapsedSec0)+" sec");
        return dict;
    }

    // for testing
    static private void doLookup(SoftTFIDFDictionary dict,double d,String s,boolean compare,double[] stats)
    {
        System.out.println("lookup: "+s);
        long start1 = System.currentTimeMillis();
        int n1 = dict.lookup(d,s);
        double elapsedSec1 = (System.currentTimeMillis()-start1) / 1000.0;
        dict.showLookup( n1 );
        List saved = new ArrayList(dict.result);
        if (compare) {
            long start2 = System.currentTimeMillis();
            int n2 = dict.slowLookup(d,s);
            double elapsedSec2 = (System.currentTimeMillis()-start2) / 1000.0;
            collectStats(elapsedSec1,elapsedSec2,saved,dict.result,stats);
            boolean differentFromBaseline = false;
            if (n1!=n2) {
                differentFromBaseline = true;
            } else {
                for (int j=0; j<n1; j++) {
                    LookupResult savedj = (LookupResult)saved.get(j);
                    if (!dict.getResult(j).equals(savedj.found) || dict.getScore(j)!=savedj.score) {
                        differentFromBaseline = true;
                    }
                }
            }
            if (differentFromBaseline) {
                System.out.println("baseline:");
                dict.showLookup(n2);
            }
        }
    }
    // for testing
    static private void collectStats(double elapsedSec1,double elapsedSec2,List saved,List result,double[] stats)
    {
        stats[0] += elapsedSec1;
        stats[1] += elapsedSec2;
        Set fastVals = new HashSet();
        Set slowVals = new HashSet();
        for (int i=0; i<saved.size(); i++) {
            fastVals.add( ((LookupResult)saved.get(i)).value );
        }
        stats[2] += fastVals.size();
        for (int i=0; i<result.size(); i++) {
            slowVals.add( ((LookupResult)result.get(i)).value );
        }
        stats[3] += slowVals.size();
        if (fastVals.size()==slowVals.size()) stats[4]++;
    }

}
