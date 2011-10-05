package com.wcohen.ss.expt;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import java.util.*;
import java.io.*;

/**
 * Holds data for evaluating a distance metric.
 */

public class MatchData 
{
    private Map sourceLists;
    private ArrayList sourceNames;
    private String filename;
    private static final boolean KEEP_OLD_ITERATION_BUG = System.getProperty("ss.keepOldIterationBug")!=null;
	
    /**
     * Read match data from a file.  Format should be:
     * sourceRelation TAB instanceID TAB field1 TAB ... fieldn LF
     */

    public MatchData(String filename) throws InputFormatException
    {
        this.filename = filename;
        sourceNames = new ArrayList();
        sourceLists = new HashMap();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            int lineNum = 0;
            while ((line = in.readLine())!=null) {
                lineNum++;
                String tok[] = line.split("\t",-1);
                int toklen = tok.length;
                if(toklen < 1)
                    throw new
                        InputFormatException(filename,lineNum,"no source");
                String src = tok[0];
                if (toklen < 2)
                    throw new
                        InputFormatException(filename,lineNum,"no id");
                String id = tok[1];
                if (toklen < 3)
                    throw new
                        InputFormatException(filename,lineNum,"no text fields");
                String text = tok[2];
                for(int i = 3;i < toklen;i++){
                    text += "\t" + tok[i];
                }
                addInstance(src,id,text);
            }
            in.close();
        } catch (IOException e) {
            throw new InputFormatException(filename,0,e.toString());
        }
    }


    public MatchData() 
    {
        this.filename = "none";
        sourceNames = new ArrayList();
        sourceLists = new HashMap();
    }

    /** Add a single instance, with given src and id, to the datafile */
    public void addInstance(String src,String id,String text) 
    {
        Instance inst = new Instance(src,id,text);
        ArrayList list = (ArrayList)sourceLists.get(src);
        if (list==null) {
            list = new ArrayList();
            sourceLists.put(src,list);
            sourceNames.add(src);
        }
        list.add(inst);
    }

    /** Number of sources in data set */
    public int numSources() { 
        return sourceNames.size(); 
    }

    /** Get string identifier for i-th source */
    public String getSource(int i) { 
        return (String)sourceNames.get(i); 
    }

    /** Number of records for source with given string id */
    public int numInstances(String src) { 
        return ((ArrayList)sourceLists.get(src)).size();
    }

    /** Get the j-th record for the named source. */
    public Instance getInstance(String src, int j) { 
        return (Instance)((ArrayList)sourceLists.get(src)).get(j); 
    }

    public StringWrapperIterator getIterator() {
        return new MatchIterator(this);
    }

    public String getFilename()
    {
        return filename;
    }

    public String toString() 
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<numSources(); i++) {
            String src = getSource(i);
            for (int j=0; j<numInstances(src); j++) {
                Instance inst = getInstance(src,j);
                buf.append(inst.toString()+"\n");
            }
        }
        return buf.toString();
    }

	
    /** A single item (aka record, string, etc) to match against others.
     * An item has an id (for evaluating correctness of a match), a
     * source (which relation its from), and a text field.  Text is
     * stored as a StringWrapper so that it can be preprocessed, if
     * necessary.
     */
    public static class Instance extends BasicStringWrapper implements SourcedStringWrapper
    {
        private final String source;
        private final String id;
        public Instance(String source, String id, String text) {
            super(text);
	    this.source = source.trim();
	    this.id = id.trim();
        }
        public String getSource() { return source; }
        public String getId() { return id; }
        public boolean sameId(Instance b) {
            return id!=null && b.id!=null && id.equals(b.id);
        }
        public String toString() { return "[src: '"+source+"' id: '"+id+"' unwrapped: '"+unwrap()+"']"; }
    }
	
    /** Iterates over all stored StringWrappers */
    static public class MatchIterator implements SourcedStringWrapperIterator 
    {
        private int sourceCursor,instanceCursor;
        private String src;  // caches getSource(sourceCursor)
        private MatchData data;

        public MatchIterator(MatchData data) { 
            this.data = data;
            sourceCursor = 0; 
            instanceCursor = 0; 
            src = data.getSource(sourceCursor); 
        }

        /** Not implemented. */
        public void remove() { throw new IllegalStateException("remove not implemented"); }

        /** Return the next StringWrapper. */
        public SourcedStringWrapper nextSourcedStringWrapper() { 
            return (SourcedStringWrapper)next(); 
        }

        /** Return the next StringWrapper. */
        public StringWrapper nextStringWrapper() { 
            return (StringWrapper)next(); 
        }

        public boolean hasNext() { 
            return sourceCursor<data.numSources() && instanceCursor<data.numInstances(src); 
        }

        /** Returns the next StringWrapper as an object. */
        public Object next() {
            Instance inst = data.getInstance( src, instanceCursor++ );
            if (KEEP_OLD_ITERATION_BUG && instanceCursor>data.numInstances(src)) {
                sourceCursor++; instanceCursor=0;
                if (sourceCursor<data.numSources()) 
                    src = data.getSource(sourceCursor);
            }
            if (!KEEP_OLD_ITERATION_BUG && instanceCursor>=data.numInstances(src)) {
                sourceCursor++; instanceCursor=0;
                if (sourceCursor<data.numSources()) 
                    src = data.getSource(sourceCursor);
            }
            return inst;
        }
    }
	

    /** Signals an incorrectly formatted MatchData file.
     */
    public static class InputFormatException extends RuntimeException {
        public InputFormatException(String file, int line, String msg) {
	    super("line "+line+" of file "+file+": "+msg);
        }
    }
	
    public static void main(String[] argv) 
    {
        try {
            MatchData md = new MatchData(argv[0]);
            System.out.println("Dump:");
	    System.out.println(md.toString());
            System.out.println();
            System.out.println("Iteration:");
            for (Iterator i = md.getIterator(); i.hasNext(); ) {
                System.out.println(i.next().toString());                
            }
        } catch (Exception e) {
	    e.printStackTrace();
        }
    }
}
