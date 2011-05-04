package com.wcohen.ss;

import com.wcohen.ss.api.*;

/** A matrix of doubles, defined recursively by the compute(i,j)
 * method, that will not be recomputed more than necessary.
 *
 */

public abstract class MemoMatrix 
{
    private double[][] value;
    private boolean[][] computed;
    protected StringWrapper s;
    protected StringWrapper t;
    protected String cellFormat = "%3g";
    protected boolean printNegativeValues = false;
	
    /** Create a MemoMatrix indexed from 0...|s| and 0...|t|. 
     * The strings s and t can be accessed later by sAt(i) and
     * tAt(j).
     */
    MemoMatrix(StringWrapper s,StringWrapper t) {
        this.s = s;
        this.t = t;
        value = new double[s.length()+1][t.length()+1];
        computed = new boolean[s.length()+1][t.length()+1];
    }
	
    /** Compute a new element of the matrix.  This should be defined in
     * terms of calls to get(i,j).
     */
    abstract double compute(int i,int j); 
	
    /** Get the value at i,j, computing it only if necessary. If it has
     * been computed before, the stored value will be re-used.
     */
    double get(int i,int j) {
        if (!computed[i][j]) {
	    value[i][j] = compute(i,j);
	    computed[i][j] = true;
        }
        return value[i][j];
    }
	
    /** Get i-th char of s, indexing s from 1..n */
    final protected char sAt(int i) { 
        return s.charAt(i-1);
    }
	
    /** Get i-th char of t, indexing s from 1..n */
    final protected char tAt(int i) { 
        return t.charAt(i-1);
    }
	
    /** Setting printNegativeValues to 'true' will invert the values
     * printed in the matrix by toString.  This is more readable
     * the values are always <=0.
     */
    final void setPrintNegativeValues(boolean flag) {
        printNegativeValues = flag;
    }
	
    /** Print the matrix, for debugging and/or explanation. */
    public String toString() 
    {
        StringBuffer buf = new StringBuffer();
        // line 1
        buf.append("   ");
        for (int i=1; i<=s.length(); i++) buf.append(" "+sAt(i)+" ");
        buf.append("\n");
        // line 2
        buf.append("   ");
        for (int i=1; i<=s.length(); i++) buf.append("---");
        buf.append("\n");
        // remaining lines
        PrintfFormat fmt = new PrintfFormat(cellFormat);
        for (int j=1; j<=t.length(); j++) {
	    buf.append(" "+tAt(j)+"|");
	    for (int i=1; i<=s.length(); i++) {
                double v = printNegativeValues ? -get(i,j) : get(i,j);
                buf.append(fmt.sprintf(v));
	    }
	    buf.append("\n");
        }
        return buf.toString();
    }

    //
    // useful subroutines	
    //

    /** Return max of three numbers. */
    final protected static double max3(double x,double y,double z) {
        return Math.max(x, Math.max(y,z) );
    }

    /** Return max of four numbers. */
    final protected static double max4(double w,double x,double y,double z) {
        return Math.max(Math.max(w,x), Math.max(y,z) );
    }
}


