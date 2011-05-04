package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * Needleman-Wunsch string distance, following Durban et al. 
 * Sec 2.3.
 */

public class NeedlemanWunsch extends AbstractStringDistance
{
    private CharMatchScore charMatchScore;
    private double gapCost;
	
    public NeedlemanWunsch() { this(CharMatchScore.DIST_01, 1.0 ); }

    public NeedlemanWunsch(CharMatchScore charMatchScore,double gapCost) {
        this.charMatchScore = charMatchScore;
        this.gapCost = gapCost;
    }
	
    public double score(StringWrapper s,StringWrapper t) {
        MyMatrix mat = new MyMatrix( s, t );
        return mat.get(s.length(), t.length() );
    }
	
    public String explainScore(StringWrapper s,StringWrapper t) {
        MyMatrix mat = new MyMatrix( s, t );
        double d = mat.get(s.length(), t.length() );
        mat.setPrintNegativeValues(true);
        return mat.toString() + "\nScore = "+d;
    }
	
    private class MyMatrix extends MemoMatrix {
        public MyMatrix(StringWrapper s,StringWrapper t) {
	    super(s,t);
        }
        public double compute(int i,int j) {
	    if (i==0) return -j*gapCost;
	    if (j==0) return -i*gapCost;
	    return max3( get(i-1,j-1) + charMatchScore.matchScore( sAt(i), tAt(j) ),
                         get(i-1, j) - gapCost,
                         get(i, j-1) - gapCost);
        }
    }
	
    static public void main(String[] argv) {
        doMain(new NeedlemanWunsch(), argv);
    }
}
