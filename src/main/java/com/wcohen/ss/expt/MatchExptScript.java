package com.wcohen.ss.expt;

import com.wcohen.ss.api.*;
import com.wcohen.ss.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Perform a series of match experiments, specified by a script in an input file.
 *
 * <p>
 * The input file can contain these commands:
 * <ol>
 * <li>echo on, echo off
 * <p>
 * <li>blocker CLASS, distance CLASS, dataset FILE
 * <li>clear blockers, clear learners, clear datasets
 * <li>show blockers, show learners, show datasets
 * <p>
 * <li>compute: compute pairwise learners for using all declared blockers, learners, datasets
 * <li>table maxF1, table averagePrecision, table time, table blockerRecall: show summary performance tables
 * <li>precisionRecall: show precision-recall curves (11-pt interpolated, 
 * plus non-interpolated average precision.)
 * <p>
 * <li>save FILE, restore FILE: save previously 'compute'-d results
 * <li>runScript FILE: execute a sub-script
 * </ol>
 */

public class MatchExptScript
{
	public static final String BLOCKER_PACKAGE = MatchExpt.BLOCKER_PACKAGE;
	public static final String DISTANCE_PACKAGE = MatchExpt.DISTANCE_PACKAGE;

	private List blockers;
	private List datasets;
	private List learners;
	private List blockerNames;
	private List datasetNames;
	private List learnerNames;
	private MatchExpt[][][] expt;
	private boolean echoCommands;
	private boolean computable;

	public MatchExptScript() 
	{
		blockers = new ArrayList();
		datasets = new ArrayList();
		learners = new ArrayList();
		blockerNames = new ArrayList();
		datasetNames = new ArrayList();
		learnerNames = new ArrayList();
		expt = null;
		echoCommands = true;
		computable = true;
	}

	//
	// commands that can be called
	//

	/** Clear datasets, blockers, or learners. */
	public void clear(String what) 
	{ 
		if (what.equals("blockers")) blockers.clear(); 
		else if (what.equals("datasets")) datasets.clear(); 
		else if (what.equals("learners")) learners.clear();
		else if (what.equals("all")) {
			clear("blockers"); clear("datasets"); clear("learners");
		} else {
			System.out.println("usage: clear blockers|datasets|learners|all");
		}
	}

	/** Show datasets, blockers, or learners. */
	public void show(String what) 
	{ 
		if (what.equals("blockers")) showList("blockers",blockerNames); 
		else if (what.equals("datasets")) showList("datasets", datasetNames); 
		else if (what.equals("learners")) showList("learners", learnerNames); 
		else if (what.equals("all")) {
			show("blockers"); show("datasets"); show("learners"); 
		} else {
			System.out.println("usage: show blockers|datasets|learners|all");
		}
	}
	static private void showList(String s,List list) {
		System.out.println(list.size()+" "+s+":");
		for (int i=0; i<list.size(); i++) {
			System.out.println((i+1)+". "+list.get(i).toString());
		}
	}


	/** Turn echoing of commands on/off. */
	public void echo(String onOrOff) 
	{ 
		echoCommands = "on".equals(onOrOff);
	}

	/** Load a dataset. */
	public void dataset(String dataFile) throws MatchData.InputFormatException 
	{	
		datasets.add( new MatchData(dataFile) ); 
		datasetNames.add( dataFile );
		expt = null;
	}

	/** Load a learner. */
	public void learner(String learnerClass) 
	{
		learners.add( DistanceLearnerFactory.build( learnerClass ));
		learnerNames.add( learnerClass );
		expt = null;
	}

	/** Load a distance learner. Same as 'learner', provided for backward compatibility, 
	 * and because sometimes it's more reasonable to think of a distance functions
	 * rather than learners for them. */
	public void distance(String distanceClass) 
	{
		learner(distanceClass);
	}

	/** Load a blocker. */
	public void blocker(String blockerClass) 
		throws ClassNotFoundException,InstantiationException,IllegalAccessException  
	{
		blockers.add( Class.forName(BLOCKER_PACKAGE+blockerClass).newInstance() );
		blockerNames.add( blockerClass );
		expt = null;
	}

	/** Load a blocker, with optional boolean value */
	public void blocker(String blockerClass, String param, String value) 
		throws ClassNotFoundException,InstantiationException,IllegalAccessException,
					 InvocationTargetException,NoSuchMethodException
	{
		Blocker blocker = (Blocker)Class.forName(BLOCKER_PACKAGE+blockerClass).newInstance();
		// upperCase the first letter of param
		param = param.substring(0,1).toUpperCase() + param.substring(1,param.length());
		Method m = blocker.getClass().getMethod("set"+param, new Class[] { Boolean.class });
		m.invoke( blocker, new Object[]{ Boolean.valueOf(value) } );
		blockers.add( blocker );
		blockerNames.add( blockerClass );
		expt = null;
	}

	/** Compute learners. */
	public void compute() 
	{
		if (!computable) {
			throw new RuntimeException("can't re-'compute' experiment results after a 'restore'");
		}
		expt = new MatchExpt[blockers.size()][learners.size()][datasets.size()];
		for (int i=0; i<blockers.size(); i++) {
			Blocker blocker = (Blocker)blockers.get(i);
			for (int j=0; j<learners.size(); j++) {
				StringDistanceLearner distance = (StringDistanceLearner)learners.get(j);
				for (int k=0; k<datasets.size(); k++) {
					MatchData dataset = (MatchData)datasets.get(k);
					expt[i][j][k] = new MatchExpt(dataset,distance,blocker);
				}
			}
		}
	}

	/** Show a table of some expt-wide numeric measurement. */
	public void table(String what) throws NoSuchMethodException,IllegalAccessException,InvocationTargetException
	{
		PrintfFormat dfmt = new PrintfFormat("Dataset %2d:");
		PrintfFormat fmt = new PrintfFormat(" %9.5f  ");
		PrintfFormat nfmt = new PrintfFormat(" %11s");
		if (expt==null) compute();
		for (int i=0; i<blockerNames.size(); i++) {
			System.out.println("\nblocker: "+blockerNames.get(i)+"\n");
			System.out.print("           ");
			for (int j=0;  j<learnerNames.size(); j++) {
				String s = (String)learnerNames.get(j);
				if (s.length()>11) s = s.substring(0,11);
				System.out.print( nfmt.sprintf(s) );
			}
			System.out.println();
			double[] average = new double[learnerNames.size()];
			for (int k=0; k<datasetNames.size(); k++) {
				System.out.print( dfmt.sprintf(k+1));
				for (int j=0; j<learnerNames.size(); j++) {
					Method m = MatchExpt.class.getMethod(what, new Class[] {});
					Double d = (Double)m.invoke(expt[i][j][k], new Object[]{});
					System.out.print( fmt.sprintf(d) );
					average[j] += d.doubleValue();
				}
				System.out.print("\n");
			}
			System.out.print( "   Average:");
			for (int j=0; j<learnerNames.size(); j++) {
				System.out.print( fmt.sprintf( average[j]/datasetNames.size() ) );
			}
			System.out.print("\n\n");
		}
	}

	/** Show interpolated 11-pt precision curves for each blocker/distance/dataset */
	public void precisionRecall()
	{
		PrintfFormat fmt1 = new PrintfFormat("%9s %9s %9s");
		PrintfFormat fmt2 = new PrintfFormat(" %5.2f");
		PrintfFormat hfmt2 = new PrintfFormat(" r%4.2f");
		// print header
		System.out.print(fmt1.sprintf( new Object[] { "block","dist","data"} ));
		double[] rec = MatchExpt.interpolated11PointRecallLevels();
		for (int m=0; m<rec.length; m++) System.out.print(hfmt2.sprintf(rec[m])); 
		System.out.println("   avgPrec\n");
		// print values
		for (int i=0; i<blockerNames.size(); i++) {
			String blocker = (String)blockerNames.get(i);
			if (blocker.length()>7) blocker = blocker.substring(0,7);
			for (int j=0; j<learnerNames.size(); j++) {
				String distance = (String)learnerNames.get(j);
				if (distance.length()>7) distance = distance.substring(0,7);
				double[] average = new double[11+1]; 
				for (int k=0; k<datasetNames.size(); k++) {
					String dataset = (String)datasetNames.get(k);
					// shorten this name
					dataset = new File(dataset).getName();
					int dotPos = dataset.indexOf('.');
					if (dotPos>=0) dataset = dataset.substring(0,dotPos);
					if (dataset.length()>9) dataset = dataset.substring(0,9);
					System.out.print(fmt1.sprintf( new Object[] { blocker, distance, dataset } ));
					double[] prec = expt[i][j][k].interpolated11PointPrecision();
					for (int m=0; m<prec.length; m++) {
						System.out.print(fmt2.sprintf( prec[m] ));
						average[m] += prec[m];
					}
					double a = ((Double)expt[i][j][k].averagePrecision()).doubleValue();
					System.out.print("   "+fmt2.sprintf( a ));
					average[11] += a;
					System.out.print("\n");
				}
				// show average over all datasets
				System.out.print(fmt1.sprintf( new Object[] { blocker, distance, "avg" }));
				for (int m=0; m<average.length-1; m++) {
					System.out.print(fmt2.sprintf( average[m]/datasetNames.size() ));
				}
				System.out.println("   "+fmt2.sprintf( average[11]/datasetNames.size()) + "\n");
			}
		}
	}

	/** Save current experimental data to a file */
	public void save(String file) throws IOException,FileNotFoundException
	{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
		oos.writeObject(blockerNames);
		oos.writeObject(datasetNames);
		oos.writeObject(learnerNames);
		oos.writeObject(expt);
		oos.close();
	}

	/** Restore experimental data previously 'saved' toa file.  It will
	 * be possible to analyze this data with 'table' commands and etc,
	 * but not to perform additional experiments.
	 */
	public void restore(String file) throws IOException,FileNotFoundException,ClassNotFoundException
	{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		blockerNames = (List)ois.readObject();
		datasetNames = (List)ois.readObject();
		learnerNames = (List)ois.readObject();
		expt = (MatchExpt[][][]) ois.readObject();
		computable = false;
		ois.close();
	}

	/**
	 * Load commands from a file and execute them. 
	 */
	public void runScript(String configFileName) 
	{
		int lineNum = 0;
		try {
	    BufferedReader in = new BufferedReader(new FileReader(configFileName));
	    String line;
	    while ((line = in.readLine())!=null) {
				lineNum++;
				if (!line.startsWith("#")) {
					String command = null;
					List args = new ArrayList();
					StringTokenizer tok = new StringTokenizer(line);
					if (tok.hasMoreTokens()) {
						command = tok.nextToken();
					}
					while (tok.hasMoreTokens()) {
						args.add( tok.nextToken() );
					}
					if (command!=null) {
						if (echoCommands) System.out.println("exec: "+line);
						execCommand(command,args);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error: "+configFileName+" line "+lineNum+": "+e.toString());
			e.printStackTrace();
			return;
		}
	}
	// execute a single command
	private void execCommand(String command,List args) 
		throws NoSuchMethodException,IllegalAccessException,InvocationTargetException
	{
		Class[] template = new Class[ args.size() ];
		for (int i=0; i<args.size(); i++) {
			template[i] = String.class;
		}
		Method m = MatchExptScript.class.getMethod(command, template );
		m.invoke(this, args.toArray(new String[0]));
	}

	static public void main(String[] argv) {
		try {
			MatchExptScript interp = new MatchExptScript();
			if (argv.length==0) {
				System.out.println("usage: file1 file2 ...");
			} else {
				for (int i=0; i<argv.length; i++) {
					interp.runScript(argv[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
