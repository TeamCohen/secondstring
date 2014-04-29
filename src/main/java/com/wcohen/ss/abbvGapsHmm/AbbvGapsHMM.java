package com.wcohen.ss.abbvGapsHmm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dana Movshovitz-Attias
 */
public class AbbvGapsHMM {
	
	private String _tfIdfDataFile = null;
	
	private Double _dfWordThreshold = 0.2;
	
	private Map<String, Double> _commonWordDF = null;

	public enum States{
		S, // start
		DL,
		M,
		D,
		END
	}

	public enum Transitions{

		t_DL_in,
		t_DL_to_M,
		
		t_M_in,
		t_M_to_D,
		t_M_to_END,
		
		t_D_in,
		t_D_to_M,
		t_D_to_END,
		
		t_S_to_M,
		t_S_to_DL,
	}

	public enum Emissions{
		/** Deletions **/
		e_DL_alphaNumeric_to_none,
		e_DL_nonAlphaNumeric_to_none,
		e_DL_word_to_none,
		

		e_D_alphaNumeric_to_none,
		e_D_word_to_none,
		// This emission allows to remove a non alpha-numeric character from the short form
		e_D_none_to_nonAlphaNumeric,
		
		/** Insertions **/

		/** Substitutions **/
		e_M_partialWord_to_letter,
		e_M_word_to_firstLetter,
		e_M_letter_to_letter, 
		e_M_nonAlphaNumeric_to_none, // an inner space deletion is considered a match
		e_M_commonWordDeletion,
		
		/** Special substitutions **/
		e_M_AND_to_symbol, 
		e_M_one_to_1,
		e_M_two_to_2,
		e_M_three_to_3,
		e_M_four_to_4,
		e_M_five_to_5,
		e_M_six_to_6,
		e_M_seven_to_7,
		e_M_eight_to_8,
		e_M_nine_to_9,
		/** Chemical Elements Substitution (For chemical elements with a symbol that does not match their common name) **/
		e_M_Silver_Ag,
		e_M_Gold_Au,
		e_M_Copper_Cu,
		e_M_Iron_Fe,
		e_M_Mercury_Hg,
		e_M_Potassium_K,
		e_M_Sodium_Na,
		e_M_Lead_Pb,
		e_M_Antimony_Sb,
		e_M_Tin_Sn,
		e_M_Tungsten_W,

		/** End **/
		e_END_end
	}

	// Edit operation counters:
	// Expected number of times that edit operations were used to generate 
	// string pairs in the corpus.
	List<Double> _transitionCounters = new ArrayList<Double>();
	List<Double> _emissionCounters = new ArrayList<Double>();

	// Forward probabilities
	Matrix3D _alpha;
	// Backward probabilities
	Matrix3D _beta;

	// Model parameters for each edit operation.
	List<Double> _transitionParams = new ArrayList<Double>();
	List<Double> _emissionParams = new ArrayList<Double>();
	boolean _externalySet = false;

	// Starting probabilities for each state
	List<Double> _stateStartProb = null;

	// Threshold for change in parameter values - used for EM convergence criteria.
	private final static Double CHANGE_THRESHOLD = 0.01d;

	// Max number of EM iterations.
	private final static int MAX_ITERATIONS = 300;

	private String _modelParamsFile = null;
	
	/**
	 * 
	 */
	public AbbvGapsHMM() {
		_modelParamsFile = null;
	}
	
	/**
	 * @param modelParamFile After training, the model parameters will be saved to this file.
	 */
	public AbbvGapsHMM(String modelParamFile) {
		_modelParamsFile = modelParamFile;
	}

	/**
	 * @param modelParamFile After training, the model parameters will be saved to this file.
	 */
	public AbbvGapsHMM(String modelParamFile, boolean allowVowelsMatch) {
		_modelParamsFile = modelParamFile;
	}
	
	public List<Double> getEmmisionParams(){
		return _emissionParams;
	}
	
	public List<Double> getTransitionParams(){
		return _transitionParams;
	}
	
	public boolean useTDIDF(){
		return _tfIdfDataFile != null;
	}
	
	public Double getDF(String word){
		if(_commonWordDF.containsKey(word)){
			return _commonWordDF.get(word);
		}
		return null;
	}
	
	public void setTfIdfData(String dataFile) throws IOException{
		_tfIdfDataFile = dataFile;
		_commonWordDF = new HashMap<String, Double>();
		
		BufferedReader fi = new BufferedReader(new FileReader(dataFile));
		String line;
		while( (line = fi.readLine()) != null){
			String parts[] = line.split(" ");
			String word = parts[0];
			Double df = Double.parseDouble(parts[1]);
			
			if(df.compareTo(_dfWordThreshold) >= 0){
				_commonWordDF.put(word, df);
			}
		}
		fi.close();
	}
	
	/**
	 * Initialize the starting probabilities for each state (hard coded).
	 */
	protected void initStartProbs(){
		if(_stateStartProb != null)
			return;

		States[] states = States.values();

		_stateStartProb = new ArrayList<Double>();
		for(int i = 0; i < states.length; ++i){
			if(states[i].name().equals("S"))
				_stateStartProb.add(1d);
			else
				_stateStartProb.add(0d);
		}
	}

	public void setParamFile(String paramFile){
		_modelParamsFile = paramFile;
	}

	public boolean train(List<List<Acronym>> corpus, List<Map<String, String>> trueLabels){
		if(!loadModelParams()){
			return trainCorpus(corpus, trueLabels);
		}
		return true;
	}

	public boolean train(List<List<Acronym>> corpus, List<Map<String, String>> trueLabels, boolean force){
		if(force)
			return trainCorpus(corpus, trueLabels);
		else
			return loadModelParams();
	}
	
	public void setStartingParams(List<Double> emmisions, List<Double> transitions){
		_emissionParams.clear();
		_emissionParams.addAll(emmisions);
		
		_transitionParams.clear();
		_transitionParams.addAll(transitions);
		
		_externalySet = true;
	}
	
	public void initModelParamsAndCounters(){
		// Init counters to 0
		// Init params to 0.5
		
		Emissions[] emissions = Emissions.values();
		_emissionCounters.clear();
		if(!_externalySet)
			_emissionParams.clear();
		for (int i = 0; i < emissions.length; i++) {
			_emissionCounters.add(0d);
			_emissionParams.add(0.5d);
		}
		
		Transitions[] transitions = Transitions.values();
		_transitionCounters.clear();
		if(!_externalySet)
			_transitionParams.clear();
		for (int i = 0; i < transitions.length; i++) {
			_transitionCounters.add(0d);
			_transitionParams.add(0.5d);
		}

		_emissionParams.set(Emissions.e_END_end.ordinal(), 1d);
		
	}

	// Gets training examples separated by documents
	protected boolean trainCorpus(List<List<Acronym>> corpus, List<Map<String, String>> trueLabels){
		boolean converge = false;
		
		// Unsupervised
		trueLabels = null;

		initModelParamsAndCounters();

		int n = corpus.size();

		int c = 1;
		Double change; 

		System.out.print("training:");
		while(!converge){

			for (int i = 0; i < n; i++) {
				List<Acronym> docAcronyms = corpus.get(i);
				Map<String, String> docTrueLabels = null;
				if(trueLabels != null)
					docTrueLabels = trueLabels.get(i);
				int m = docAcronyms.size();
				for (int j = 0; j < m; j++) {
					Acronym currAcronym = docAcronyms.get(j);
					if(trueLabels != null)
						expectationStep(currAcronym, docTrueLabels.get(currAcronym._shortForm));
					else
						expectationStep(currAcronym, null);
				}
			}

			change = maximizationStep();
			System.out.print(".");

			c++;
			if(c > MAX_ITERATIONS){
				System.out.println("\n\tTraining stopped after "+(c-1)+" iterations with final change: "+change);
				converge = true;
			}
			if(change.compareTo(CHANGE_THRESHOLD) < 0){
				System.out.println("\n\tTraining converged in "+(c-1)+" iterations.");
				converge = true;
			}
		}
		saveModelParams();
		return true;
	}

	protected void expectationStep(Acronym acronym, String trueLongForm){
		AbbvGapsHmmBackwardsEvaluator backEval = new AbbvGapsHmmBackwardsEvaluator(this);
		backEval.backwardEvaluate(acronym, _transitionParams, _emissionParams);
		_beta = backEval.getEvalMatrix();
		
		if(_beta.at(0, 0, States.S.ordinal()) == 0)
			return;
		
		AbbvGapsHmmForwardEvaluator forEval = new AbbvGapsHmmForwardEvaluator(this);
		forEval.forwardEvaluate(acronym, _transitionParams, _emissionParams);
		_alpha = forEval.getEvalMatrix();
		
		AbbvGapsHmmExpectationEvaluator expectationEval = new AbbvGapsHmmExpectationEvaluator(this);
		expectationEval.expectationEvaluate(acronym, _transitionCounters, _emissionCounters, _transitionParams, _emissionParams, _alpha, _beta);
		
		_transitionCounters = expectationEval.getTransitionCounters();
		_emissionCounters = expectationEval.getEmissionCounters();
	}


	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> viterbi(Acronym acronym){
		AbbvGapsHmmBackwardsViterbiEvaluator viterbi = new AbbvGapsHmmBackwardsViterbiEvaluator(this);
		return viterbi.backwardViterbiEvaluate(acronym, _transitionParams, _emissionParams);
	}

	public void saveModelParams(){
		if(_modelParamsFile == null)
			return;

		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(_modelParamsFile));

			// Emmisions
			bw.write("# Emmisions\n");
			Emissions emissions[] = Emissions.values();
			for (int i = 0; i < emissions.length; i++) {
				bw.write(emissions[i].toString() + "\t" + _emissionParams.get(i) + "\n");
			}
			// Transitions
			bw.write("# Transitions\n");
			Transitions transitions[] = Transitions.values();
			for (int i = 0; i < transitions.length; i++) {
				bw.write(transitions[i].toString() + "\t" + _transitionParams.get(i) + "\n");
			}
			bw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean loadModelParams(){
		try{
			if(_modelParamsFile == null)
				return false;
			
			File f = new File(_modelParamsFile);
			if(!f.exists()){
				return false;
			}
			
			BufferedReader fi = new BufferedReader(new FileReader(_modelParamsFile));

			_emissionParams.clear();
			_transitionParams.clear();
			
			Emissions emissions[] = Emissions.values();
			
			int i = 0;
			String line;
			
			while( (line = fi.readLine()) != null){
				if(line.startsWith("#"))
					continue;
				
				String[] parts = line.split("\t");
				if(i < emissions.length){
					_emissionParams.add(Double.parseDouble(parts[1]));
					i ++;
					continue;
				}
				_transitionParams.add(Double.parseDouble(parts[1]));
			}
			fi.close();
			return (_transitionParams.size() == Transitions.values().length);
		}
		catch (IOException e) {
			_emissionParams.clear();
			_transitionParams.clear();
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns the total change in model parameter values.
	 */
	protected Double maximizationStep(){
		
		Double valChange = 0d;
		
		valChange += maximizationStepForEmissions();
		valChange += maximizationStepForTransitions();
		
		return valChange;
	}
	
	protected Double maximizationStepForTransitions(){
		// Normalization factor
		double total_M = 0;
		double total_D = 0;
		double total_DL = 0;
		double total_S = 0;
		double total_I = 0;
		Transitions[] transitions = Transitions.values();
		for (int i = 0; i < transitions.length; i++) {
			String currTransition = transitions[i].name();
			if(currTransition.startsWith("t_DL_"))
				total_DL += smoothCounter(i, _transitionCounters, _transitionParams);
			else if(currTransition.startsWith("t_M_"))
				total_M += smoothCounter(i, _transitionCounters, _transitionParams);
			else if(currTransition.startsWith("t_D_"))
				total_D += smoothCounter(i, _transitionCounters, _transitionParams);
			else if(currTransition.startsWith("t_S_"))
				total_S += smoothCounter(i, _transitionCounters, _transitionParams);
			else if(currTransition.startsWith("t_I_"))
				total_I += smoothCounter(i, _transitionCounters, _transitionParams);
		}
		
		Double valChange = 0d;
		Double newVal;
		for (int i = 0; i < transitions.length; i++) {
			String currTransition = transitions[i].name();
			if(currTransition.startsWith("t_DL_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _transitionCounters, _transitionParams), total_DL));
			else if(currTransition.startsWith("t_M_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _transitionCounters, _transitionParams), total_M));
			else if(currTransition.startsWith("t_D_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _transitionCounters, _transitionParams), total_D));
			else if(currTransition.startsWith("t_S_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _transitionCounters, _transitionParams), total_S));
			else if(currTransition.startsWith("t_I_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _transitionCounters, _transitionParams), total_I));
			else
				newVal = new Double(1);
			
			valChange += Math.abs(_transitionParams.get(i) - newVal);

			_transitionParams.set(i, newVal);
		}
		
		return valChange;
	}
	
	/**
	 * Dirichlet smoothing
	 * -------------------
	 * 
	 * Without a prior: 
	 * 		P(data | theta) = theta(i)^beta(i) = counters(i)
	 * 
	 * 
	 * With a dirichlet prior:
	 * 		P(data | theta)*p(theta) = theta(i)^(beta(i) + alpha(i)) =
	 * 			theta(i)^beta(i) + theta(i)^alpha(i) 
	 * 			counters(i) + params(i)^alpha(i)
	 */
	protected double smoothCounter(int i, List<Double> counters, List<Double> params){
		double alpha = 1;
		return counters.get(i) + Math.pow(params.get(i), alpha);
	}
	
	protected double getNewStateVal(double current, double total){
		if(total == 0)
			return 0d;
		return new Double( (current/*+1*/) /total);
	}
	
	protected Double maximizationStepForEmissions(){
		// Normalization factor
		double total_M = 0;
		double total_D = 0;
		double total_DL = 0;
		double total_I = 0;
		Emissions[] emissions = Emissions.values();
		for (int i = 0; i < emissions.length; i++) {
			String currEmission = emissions[i].name();
			if(currEmission.startsWith("e_DL_"))
				total_DL += smoothCounter(i, _emissionCounters, _emissionParams);
			else if(currEmission.startsWith("e_M_"))
				total_M += smoothCounter(i, _emissionCounters, _emissionParams);
			else if(currEmission.startsWith("e_D_"))
				total_D += smoothCounter(i, _emissionCounters, _emissionParams);
			else if(currEmission.startsWith("e_I_"))
				total_I += smoothCounter(i, _emissionCounters, _emissionParams);
		}
		
		Double valChange = 0d;
		Double newVal;
		for (int i = 0; i < emissions.length; i++) {
			String currEmission = emissions[i].name();
			if(currEmission.startsWith("e_DL_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _emissionCounters, _emissionParams), total_DL));
			else if(currEmission.startsWith("e_M_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _emissionCounters, _emissionParams), total_M));
			else if(currEmission.startsWith("e_D_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _emissionCounters, _emissionParams), total_D));
			else if(currEmission.startsWith("e_I_"))
				newVal = new Double(getNewStateVal(smoothCounter(i, _emissionCounters, _emissionParams), total_I));
			else
				newVal = new Double(1);
			
			valChange += Math.abs(_emissionParams.get(i) - newVal);

			_emissionParams.set(i, newVal);
		}
		
		return valChange;
	}
	

}
