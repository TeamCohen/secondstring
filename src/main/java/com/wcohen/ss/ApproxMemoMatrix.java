package com.wcohen.ss;

import com.wcohen.ss.api.*;

import org.apache.log4j.Logger;

/** 
 * Variant of MemoMatrix that only stores values near the diagonal,
 * for better efficiency.
 */

// to do: export scale, loJNearDiag(i), hiJNearDiag(i)
//

public abstract class ApproxMemoMatrix extends MemoMatrix
{
    private static Logger log=Logger.getLogger(ApproxMemoMatrix.class);

    // explicitly stored values
    private double[][] valuesNearDiagonal;
    private boolean[][] wasComputed;
    // value used for entries not explicitly stored
    private double defaultValue;   

    //
    // these define which values are explicitly stored
    //

    // since the matrix is not necessarily square, the 'diagonal'
    // entry for i is not i, but i*scale.
    private double scale; 

    // store m[i,i*scale-width] thru store m[i,i*scale+width] 
    private int width;  

    ApproxMemoMatrix(StringWrapper s,StringWrapper t,int width,double defaultValue) 
    {
        super(new BasicStringWrapper(""),new BasicStringWrapper(""));
        this.s = s;
        this.t = t;
        this.valuesNearDiagonal = new double[s.length()+1][2*width];
        this.wasComputed = new boolean[s.length()+1][2*width];
        this.defaultValue = defaultValue;
        this.width = width;
        this.scale = (t.length()+1.0)/(s.length()+1.0);
    }
	
    private int offsetFromDiagonal(int i,int j)
    {
        int diagForI = (int)Math.round( i*scale );
        int k = diagForI-j + width;
        return k;
    }
    private boolean nearDiagonal(int k)
    {
        return k>=1 && k<2*width;
    }

    double get(int i,int j) 
    {
        // value for i,j is stored in valuesNearDiagonal[i][k]
        int k = offsetFromDiagonal(i,j);
        if (!nearDiagonal(k)) {
            return defaultValue;
        } else if (wasComputed[i][k]) {
            return valuesNearDiagonal[i][k];
        } else {
            valuesNearDiagonal[i][k] = compute(i,j);
            wasComputed[i][k] = true;
            return valuesNearDiagonal[i][k];
        }
    }

    public boolean outOfRange(int i, int j)
    {
        boolean out = i<1 || i>s.length() || j<1 || j>t.length();
        if (out) log.error("out of range: |s|="+s.length()+" |t|="+t.length()+" s = '"+s+"' t="+t+"'");
        return out;
    }


    //
    // low-level access to the stored part of the matrix
    //

    int getWidth() { return width; }

    double getScale() { return scale; }

    int getFirstStoredEntryInRow(int i) 
    { 
        int diagForI = (int)Math.round( i*scale );
        return Math.max(1, (diagForI - width));
    }

    int getLastStoredEntryInRow(int i) 
    { 
        int diagForI = (int)Math.round( i*scale );
        return Math.min(t.length(), (diagForI + width));
    }


    /** Print the matrix, for debugging and/or explanation. */
    public String toString() 
    {
        PrintfFormat fmt = new PrintfFormat(cellFormat);
        StringBuffer buf = new StringBuffer();
        // line 1 - a ruler
        buf.append("   ");  buf.append("   ");
        for (int i=1; i<=s.length(); i++) buf.append(fmt.sprintf((double)i));
        buf.append("\n");
        // line 2 - the string
        buf.append("   "); buf.append("   ");
        for (int i=1; i<=s.length(); i++) buf.append(" "+trapNewline(sAt(i))+" ");
        buf.append("\n");
        // line 2 - an hrule
        buf.append("   "); buf.append("   ");
        for (int i=1; i<=s.length(); i++) buf.append("---");
        buf.append("\n");
        // remaining lines
        for (int j=1; j<=t.length(); j++) {
            buf.append(fmt.sprintf((double)j));
	    buf.append(" "+trapNewline(tAt(j))+"|");
	    for (int i=1; i<=s.length(); i++) {
                if (!nearDiagonal(offsetFromDiagonal(i,j)) || defaultValue==get(i,j)) {
                    buf.append(" * ");
                } else {
                    double v = printNegativeValues ? -get(i,j) : get(i,j);
                    buf.append(fmt.sprintf(v));
                }
	    }
	    buf.append("\n");
        }
        return buf.toString();
    }
    private char trapNewline(char ch)
    {
        return Character.isWhitespace(ch) ? ' ' : ch;
    }
}


