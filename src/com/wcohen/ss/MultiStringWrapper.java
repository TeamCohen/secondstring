package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.api.*;

/**
 * A StringWrapper that stores a version of the string
 * that has been either (a) split into a number of distinct fields,
 * or (b) duplicated k times, so that k different StringDistance's
 * can preprocess it, of (b) both of the above.
 */

public class MultiStringWrapper extends BasicStringWrapper
{
	private String s;
	private StringWrapper[] f;
	private int[] learnerIndex, fieldIndex;
	private boolean fieldsPrepared;

	/** Create a MultiStringWrapper by splitting s into
	 * fields based on the given delimiter. 
	 */
	public MultiStringWrapper(String s, String delim) { this(s,1,delim); }

	/** Create a MultiStringWrapper by making k copies of s. */
	public MultiStringWrapper(String s, int numCopies) { this(s,numCopies,null); }

	/** Create a MultiStringWrapper by making k copies of each field of s. */
	public MultiStringWrapper(String s, int numCopies, String delim) { 
		super(s);
		this.s = s;
		String[] fields;
		if (delim!=null) fields = s.split(delim,-1);
		else fields = new String[]{s};
		f = new StringWrapper[ fields.length * numCopies ];
		learnerIndex = new int[ f.length ];
		fieldIndex = new int[ f.length ];
		for (int i=0; i<fields.length; i++) {
			for (int j=0; j<numCopies; j++) {
				int k = i*numCopies+j;
				f[k] = new BasicStringWrapper(fields[i]);
				learnerIndex[k] = j;
				fieldIndex[k] = i;
			}
		}
		fieldsPrepared = false;
	}

	/** Return number of fields. */
	public int size() { return f.length; }

	/** Return the index of the learner that will process internal field i */
	public int getDistanceLearnerIndex(int i) { return learnerIndex[i]; }

	/** Return the field i of the original string associated with internal field i */
	public int getFieldIndex(int i) { return fieldIndex[i]; }

	/** Return the i-th field. */
	public StringWrapper get(int i) { return f[i]; }
	
	/** Set the i-th field. */
	public void set(int i, StringWrapper w) { f[i] = w; }

	/** Prepare each field with the appropriate distance */
	public void prepare(StringDistance[] innerDistances) {
		if (!fieldsPrepared) {
			for (int i=0; i<size(); i++) {
				StringDistance d = innerDistances[ getDistanceLearnerIndex(i) ];
				set( i, d.prepare( get(i).unwrap() ) );
			}
		}
	}

	public String toString() { 
		StringBuffer buf =  new StringBuffer("[multiwrap '"+s+"':");
		for (int i=0; i<size(); i++) {
			buf.append(" '"+get(i)+"'");
		}
		buf.append("]");
		return buf.toString();
	}

	public static void main(String[] args) {
		try {
			if (args.length==3) {
				int numCopies = Integer.parseInt(args[0]);
				String delim = args[1];
				String s = args[2];
				System.out.println(new MultiStringWrapper(s,numCopies,delim));
			} else if (args.length==2) {
				int numCopies = Integer.parseInt(args[0]);
				String s = args[1];
				System.out.println(new MultiStringWrapper(s,numCopies,null));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("usage: numCopies delim string");
		}
	}
}
