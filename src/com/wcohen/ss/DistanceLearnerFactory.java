package com.wcohen.ss;

import com.wcohen.ss.api.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;

/**
 * Creates distance metric learners from string descriptions.
 *
 * String descriptions are of the form D1/D2/..Dk where D is either
 * the class name of a StringDistanceLearner or a class name plus [param=xxx]
 * where xxx is a double. For example, 
 * <p>
 * <code> 
 * SoftTFIDF[tokenMatchThreshold=0.9]/WinklerVariant/Jaro
 * </code>
 * <p>
 * builds the JaroWinklerSoftTFIDF class.
 */

public class DistanceLearnerFactory
{
	/** Generate a StringDistance from a class name, or a sequence of classnames
	 * separated by slashes.
	 */
	public static StringDistanceLearner build(String classNames) 
	{
		return build(0,split(classNames));
	}

	/** Generate a StringDistanceArray given a sequence of classnames
	 * separated by slashes.
	 */
    public static StringDistance[] buildArray(String classNames) 
	{
	    String classNamesArray[] = split(classNames);
	    StringDistance learners[] = new StringDistance[classNamesArray.length];
	    for (int i = 0; i < classNamesArray.length; i++) {
		learners[i] = build(classNamesArray[i]).getDistance();
	    }
	    return learners;
	}

	/** Generate a StringDistance from a sequence of classnames.
	 */
	public static StringDistanceLearner build(String[] classNames) 
	{
		return build(0,classNames);
	}

	private static StringDistanceLearner build(int lo,String[] classNames) 
	{
		try {
			Class c = findClassFor( classNames[lo] );
			StringDistanceLearner result = null;
			if (lo == classNames.length-1) {
				// last one in the list
				result = (StringDistanceLearner) c.newInstance();
			} else {
				StringDistanceLearner innerDist = build(lo+1,classNames);
				Constructor constr = c.getConstructor( new Class[] { StringDistance.class } );
				result = 
				    (StringDistanceLearner) constr.newInstance( new Object[] { innerDist } );
			}
			// set a parameter value, if it exists
			String p = findParamFor( classNames[lo] );
			if (p!=null) {
			    String v = findParamValueFor( classNames[lo] );
			    Double d = new Double(Double.parseDouble(v));
			    String setMethodName = "set" + p.substring(0,1).toUpperCase() + p.substring(1);
			    Method m = result.getClass().getMethod(setMethodName, new Class[] { Double.class } );
			    m.invoke(result, new Object[] { d });
			}
			return result;
			
		} catch (Exception e) {
			//e.printStackTrace();
			StringBuffer buf = new StringBuffer(classNames[0]);
			if (lo==0) buf.append("<<<");
			for (int i=1; i<classNames.length; i++) {
				buf.append("/" + classNames[i]);
				if (lo==i) buf.append("<<<");
			}
			throw new IllegalStateException("error building '"+buf+"': "+e);
		}
	}
	// find yyy in a string of the form xxx[yyy=zzz]
	static private String findParamFor(String s)
	{
		int endClassIndex = s.indexOf('[');
		if (endClassIndex<0) return null;
		else {
			int endParamIndex = s.indexOf('=',endClassIndex+1);
			if (endParamIndex<0) throw new IllegalStateException("illegal class description '"+s+"'");
			return s.substring(endClassIndex+1,endParamIndex);
		}
	}
	// find zzz in a string of the form xxx[yyy=zzz]
	static private String findParamValueFor(String s)
	{
		int endParamIndex = s.indexOf('=');
		int endValueIndex = s.indexOf(']',endParamIndex);
		if (endValueIndex<0) throw new IllegalStateException("illegal class description '"+s+"'");
		return s.substring(endParamIndex+1,endValueIndex);
	}
	// find xxx in a string of the form xxx[yyy=zzz] or xxxx
	static private Class findClassFor(String s) throws ClassNotFoundException
	{ 
		int endClassIndex = s.indexOf('[');
		if (endClassIndex>=0) s = s.substring(0,endClassIndex);
		try {
			return Class.forName(s);
		} catch (ClassNotFoundException e) {
			return Class.forName("com.wcohen.ss."+s);
		}
	}
	// split a string of the form a/b/c/d/ into a string array {a,b,c,...}
	// 
	static private String[] split(String s) 
	{
		ArrayList list = new ArrayList();
    // begin = start of next class description
		int begin = 0; 
		for (int end=s.indexOf('/'); end>=0; end=s.indexOf('/',end+1)) {
			list.add( s.substring(begin,end) );
			begin = end+1; 
		}
		list.add( s.substring(begin) );
		return (String[]) list.toArray( new String[list.size()] );
	}

	/** Test routine.
	 */
	static public void main(String[] args) 
	{
		try {
			if (args[0].indexOf('/')>0) {
				System.out.println(build(args[0]));				
			} else {
				System.out.println(build(args));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
