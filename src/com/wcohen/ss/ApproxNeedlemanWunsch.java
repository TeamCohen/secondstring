package com.wcohen.ss;

import com.wcohen.ss.api.*;
import java.io.*;

/**
 * Needleman-Wunsch string distance, following Durban et al. 
 * Sec 2.3, but using an approximate string distance.
 */

public class ApproxNeedlemanWunsch extends AbstractStringDistance
{
    private static final int DEFAULT_WIDTH = 40;
    private CharMatchScore charMatchScore;
    private double gapCost;
    private MyMatrix mat;
    private int width = DEFAULT_WIDTH;
	
    public ApproxNeedlemanWunsch() { this(CharMatchScore.DIST_01, 1.0 ); }

    public ApproxNeedlemanWunsch(CharMatchScore charMatchScore,double gapCost) {
        this.charMatchScore = charMatchScore;
        this.gapCost = gapCost;
    }
	
    public void setWidth(int w) { this.width=w; }

    public double score(StringWrapper s,StringWrapper t) {
        mat = new MyMatrix( s, t );
        // fill matrix forward to prevent deep recursion
        for (int i=1; i<=s.length(); i++) {
            int j = (int)Math.round(i * mat.getScale());
            if (j>=1 && j<=t.length()) {
                double forceComputatationHere = mat.get( i, j);                
            }
        }
        return mat.get(s.length(), t.length() );
    }
	
    public String explainScore(StringWrapper s,StringWrapper t) {
        mat = new MyMatrix( s, t );
        double d = mat.get(s.length(), t.length() );
        mat.setPrintNegativeValues(true);
        return mat.toString() + "\nScore = "+d;
    }
	

    /** Find a character in the first string, s, that can be aligned
     * with the i-th character in the second string, t. */
    public int getAlignedChar(int iMinusOne,boolean preferHigherIndices)
    {
        // internally to this package, strings are indexed 1...N, so
        // we need to convert from the usual 0...N-1 Java convention
        int i = iMinusOne+1;

        int bestJ = -1;
        double bestScore = -Double.MAX_VALUE;
        //System.out.print("align to: "+i);
        for (int j=mat.getFirstStoredEntryInRow(i); j<=mat.getLastStoredEntryInRow(i); j++) {
            if (mat.outOfRange(i,j)) System.out.println("out of range: "+i+","+j);
            double score = mat.get(i,j);
            //System.out.print(" at"+j+"="+(-score));
            if ((score>bestScore) || (score==bestScore && preferHigherIndices)) {
                bestScore = score; bestJ = j;
                //System.out.print("!");
            }
        }
        //System.out.println("i="+i+" bestJ="+bestJ+ " score="+bestScore+" alignment("+iMinusOne+")="+(bestJ-1));
        // convert back to the usual 0...N-1 Java convention
        return bestJ-1;
    }
    
    private class MyMatrix extends ApproxMemoMatrix 
    {
        public MyMatrix(StringWrapper s,StringWrapper t) {
	    super(s,t,width,-Double.MAX_VALUE);
        }
        public double compute(int i,int j) {
	    if (i==0) return -j*gapCost;
	    if (j==0) return -i*gapCost;
	    return max3( get(i-1,j-1) + charMatchScore.matchScore( sAt(i), tAt(j) ),
                         get(i-1, j) - gapCost,
                         get(i, j-1) - gapCost);
        }
    }
	

    static public void main(String[] argv) throws Exception
    {
        if (argv.length==3) {
            // -f file1 file2
            String s = readFile(new File(argv[1]));
            String t = readFile(new File(argv[2]));
            long t0 = System.currentTimeMillis();
            double score = new ApproxNeedlemanWunsch().score(s,t);
            long tf = System.currentTimeMillis();
            System.out.println("score = "+score+" runtime = "+((tf-t0)/1000.0)+" sec"); 
        } else {
            doMain(new ApproxNeedlemanWunsch(), argv);
        }
    }

    private static String readFile(File in) throws IOException
    {
        InputStream inputStream = new FileInputStream(in);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();
        return new String(bytes);
    }
}
