package com.wcohen.ss;

import com.wcohen.ss.api.*;

/**
 * Affine-gap string distance, following Durban et al. 
 * Sec 2.3.
 */

public class AffineGap extends AbstractStringDistance
{
	private CharMatchScore charMatchScore;
	private double openGapScore;
	private double extendGapScore;
	private double lowerBound;
	
	public AffineGap() {
		this(CharMatchScore.DIST_21, 2, 1, -Double.MAX_VALUE );
	}
	public AffineGap(CharMatchScore charMatchScore,double openGapScore, double extendGapScore, double lowerBound) {
		this.charMatchScore = charMatchScore;
		this.openGapScore = openGapScore;
		this.extendGapScore = extendGapScore;
		this.lowerBound = lowerBound;
	}
	
	public double score(StringWrapper s,StringWrapper t) {
		MatrixTrio mat = new MatrixTrio( s, t );
		return score(s,t,mat);
	}
	
	private double score(StringWrapper s,StringWrapper t,MatrixTrio mat) {
		double best = -Double.MAX_VALUE;
		for (int i=0; i<=s.length(); i++) {
	    for (int j=0; j<=t.length(); j++) {
				best = Math.max( best, mat.get(i,j) );
	    }
		}
		return best;
	}
	
	public String explainScore(StringWrapper s,StringWrapper t) {
		MatrixTrio mat = new MatrixTrio( s, t );
		double d = score(s,t,mat);
		return mat.toString() + "\nScore = "+d;
	}
	
	// a set of three linked distance matrices
	protected class MatrixTrio extends MemoMatrix
	{
		protected MemoMatrix m;
		protected InsertSMatrix is;
		protected InsertTMatrix it;
		public MatrixTrio(StringWrapper s,StringWrapper t) {
			super(s,t);
			is = new InsertSMatrix(s,t);
			it = new InsertTMatrix(s,t);
			m = this;
		}
		public double compute(int i,int j) {
			if (i==0 || j==0) return 0;
			double matchScore = charMatchScore.matchScore( sAt(i), tAt(j) );
			return max4( lowerBound,
									 m.get(i-1,j-1) + matchScore,
									 is.get(i-1,j-1) + matchScore,
									 it.get(i=1,j-1) + matchScore );
		}
		protected class InsertSMatrix extends MemoMatrix {
			public InsertSMatrix(StringWrapper s,StringWrapper t) { super(s,t); }
			public double compute(int i,int j) {
				if (i==0 || j==0) return 0;
				return max3( lowerBound,
										 m.get(i-1,j) + openGapScore,
										 is.get(i-1,j) + extendGapScore );
			}
		}
		protected class InsertTMatrix extends MemoMatrix {
			public InsertTMatrix(StringWrapper s,StringWrapper t) { super(s,t); }
			public double compute(int i,int j) {
				if (i==0 || j==0) return 0;
				return max3( lowerBound,
										 m.get(i,j-1) + openGapScore,
										 it.get(i,j-1) + extendGapScore );
			}
		}
	}

	static public void main(String[] argv) {
		doMain(new AffineGap(), argv);
	}
}

