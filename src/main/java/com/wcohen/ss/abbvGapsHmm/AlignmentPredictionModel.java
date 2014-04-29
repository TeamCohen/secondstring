package com.wcohen.ss.abbvGapsHmm;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Dana Movshovitz-Attias 
 */
public class AlignmentPredictionModel {
	
	public static final String SEPARATOR = "#_#";
	
	public static String _trainingDataDir;
	public static String _trueLabelsFile;
	public static String _trainingCorpusFile;

	private AbbvGapsHMM _abbvHmm = null;

	public AlignmentPredictionModel() throws IOException{
		_abbvHmm = new AbbvGapsHMM();
		setTrainingDataDir("train/");
	}
	
	public void setTrainingDataDir(String trainDir) {
		_trainingDataDir = trainDir;
		_trueLabelsFile = _trainingDataDir+"abbvAlign_pairs.txt";
		_trainingCorpusFile = _trainingDataDir+"abbvAlign_corpus.txt";
	}
	
	public void setTfIdfData(String dataFile) throws IOException{
		_abbvHmm.setTfIdfData(dataFile);
	}
	
	public void setModelParamsFile(String paramFilename){
		_abbvHmm.setParamFile(paramFilename);
	}
	public void setModelParamsFile(){
		setModelParamsFile("hmmModelParams.txt");
	}
	
	public static ArrayList<Map<String, String>> loadLabels(String labelsFile) {
		if(labelsFile == null)
			return null;
		
		ArrayList<Map<String, String>> labels = null; 
		
		try{
			BufferedReader fi = new BufferedReader(new FileReader(labelsFile));
			
			labels = new ArrayList<Map<String, String>>(); 

			String docLine;
			while( (docLine = fi.readLine()) != null){

				Map<String, String> docAcronymMap = new HashMap<String, String>();

				String acronyms[] = docLine.split(SEPARATOR);

				for (int i = 0; i < acronyms.length; i++) {
					String singleAcronym = acronyms[i];

					if(singleAcronym.isEmpty())
						continue;

					String parts[] = singleAcronym.split("\t");
					if(parts.length != 2){
						System.out.println("BAD FORMAT in "+labelsFile+": "+singleAcronym);
					}
					else{
						docAcronymMap.put(parts[0].trim(), parts[1].trim());
					}
				}

				labels.add(docAcronymMap);
			}
			fi.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		return labels;
	}
	
	public static List<String> loadTrainingCorpus(String corpusFile){
		
		List<String> trainingCorpus = null;
		try{
			trainingCorpus = new ArrayList<String>();

			BufferedReader fi = new BufferedReader(new FileReader(corpusFile));
			String line;
			while( (line = fi.readLine()) != null){
				trainingCorpus.add(line);
			}
			fi.close();
		}catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return trainingCorpus;
	}
	
	// Trains on full corpus
	public boolean trainOnAll(){
		
		List<Map<String, String>> trueLabels = loadLabels(_trueLabelsFile);
		List<String> corpus = loadTrainingCorpus(_trainingCorpusFile);
		
		List<List<Acronym>> trainingExtractedCandidates = new ArrayList<List<Acronym>>();
		List<Map<String, String>> trueLabelsForTraining = new ArrayList<Map<String,String>>();

		for(Integer docID = 0; docID < corpus.size(); ++docID){
			// Adds to training examples, all the extracted pairs from the current document.
			trainingExtractedCandidates.add( extractCandidatePairs(corpus.get(docID)) );
			trueLabelsForTraining.add( trueLabels.get(docID) );
		}

		return _abbvHmm.train(trainingExtractedCandidates, trueLabelsForTraining, true);
	}
	
	// Trains on candidate pairs extracted from the corpus
	public boolean trainOnCandidates(){
		
		List<String> corpus = loadTrainingCorpus(_trainingCorpusFile);
		
		List<List<Acronym>> trainingExtractedCandidates = new ArrayList<List<Acronym>>();

		for(Integer docID = 0; docID < corpus.size(); ++docID){
			// Adds to training examples, all the extracted pairs from the current document.
			trainingExtractedCandidates.add( extractCandidatePairs(corpus.get(docID)) );
		}

		return _abbvHmm.train(trainingExtractedCandidates, null, true);
	}


	public boolean train(List<String> corpus, List<Integer> trainingSet, List<Map<String, String>> trueLabels) {
		List<List<Acronym>> trainingExtractedCandidates = new ArrayList<List<Acronym>>();
		List<Map<String, String>> trueLabelsForTraining = new ArrayList<Map<String,String>>();

		if(trainingSet != null){
			for (Integer docID : trainingSet) {
				// Adds to training examples, all the extracted pairs from the current document.
				trainingExtractedCandidates.add( extractCandidatePairs(corpus.get(docID)) );
				trueLabelsForTraining.add( trueLabels.get(docID) );
			}
		}
		else{
			// Iterates over all documents in the corpus
			for(int docID = 0; docID < corpus.size(); ++docID){
				// Adds to training examples, all the extracted pairs from the current document.
				trainingExtractedCandidates.add( extractCandidatePairs(corpus.get(docID)) );
				trueLabelsForTraining.add( trueLabels.get(docID) );
			}
		}

		return _abbvHmm.train(trainingExtractedCandidates, trueLabelsForTraining, true);
	}
	
	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> predict(String sf, String lf) {
		return predictAlignment(new Acronym(sf, lf));
	}

	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> predictAlignment(Acronym candidatePair) {
		return _abbvHmm.viterbi(candidatePair);
	}
	
	public Acronym predict(Acronym candidatePair) {

		AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> alignment = predictAlignment(candidatePair);
		Acronym currAcronym = null;

		try {
			if(alignment == null){
				// No good alignment found
				return null;
			}
			
			currAcronym = alignment.getAcronym();
			if(currAcronym != null){
				AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> acronymAlignment = predictAlignment(currAcronym);
				currAcronym._probability = acronymAlignment.getProbability();
				currAcronym._alignment = alignment;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currAcronym;
	}
	
	public Map<String, Acronym> acronymsArrayToMap(Collection<Acronym> pairs){
		Map<String, Acronym> out = new HashMap<String, Acronym>();

		for (Acronym acronymPair : pairs) {
			if(out.containsKey(acronymPair._shortForm)){
				Acronym prevAcronym = out.get(acronymPair._shortForm);
				if(acronymPair._probability != null && prevAcronym._probability != null){
					if(acronymPair._probability.compareTo(prevAcronym._probability) > 0){
						out.put(acronymPair._shortForm, acronymPair);
					}
				}
			}
			else{
				out.put(acronymPair._shortForm, acronymPair);
			}
		}

		return out;
	}
	
	public Collection<Acronym> predict(String text) {
		List<Acronym> candidates = extractCandidatePairs(text);

		List<Acronym> predictions = new ArrayList<Acronym>();
		Acronym currPrediction;

		for (Acronym candidateAcronym : candidates) {
			currPrediction = predict(candidateAcronym);
			if(currPrediction != null){
				predictions.add(currPrediction);
			}
		}
		
		return predictions;
	}
	
	public boolean trainIfNeeded() {
		if(!_abbvHmm.loadModelParams()){
			return trainOnCandidates();
		}
		return true;
	}
	
	/**** Candidates Extraction ****/
	public List<Acronym> extractCandidatePairs(String text) {
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		extractedPairs.addAll(extractSingleAcronyms(text));
		extractedPairs.addAll(extractPatternAcronyms(text));
		
		return extractedPairs;
	}
	
	protected List<Acronym> extractPatternAcronyms(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		extractedPairs.addAll(extractHeadNounPattern_2Parts(text));
		extractedPairs.addAll(extractHeadNounPattern_3Parts(text));
		extractedPairs.addAll(extractTrailingNounPattern_2Parts(text));
		extractedPairs.addAll(extractTrailingNounPattern_3Parts(text));
		
		return extractedPairs;
	}
	
	protected void addCandidatePair(List<Acronym> allPairs, String longFormCandidate, String shortFormCandidate){
		Acronym pair = parseCandidate(longFormCandidate, shortFormCandidate);
		if(pair != null && !pair._shortForm.isEmpty()){
			allPairs.add(pair);
		}
	}
	
	protected List<Acronym> extractHeadNounPattern_3Parts(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		String nounExp = "([a-zA-Z0-9\\-]{1,20})";
		String shortFormExp = "\\(([^\\(]*?)\\)";
		
		Matcher matcher = Pattern.compile (nounExp+" "+nounExp+" "+shortFormExp+",? "+nounExp+" "+shortFormExp+",? and "+nounExp+" "+shortFormExp).matcher(text);
		int startPos = 0;
		while (startPos < text.length() && matcher.find(startPos))
		{
			String mainNoun = matcher.group(1);
			
			String part1 = matcher.group(2);
			String part1_short = matcher.group(3);
			String part2 = matcher.group(4);
			String part2_short = matcher.group(5);
			String part3 = matcher.group(6);
			String part3_short = matcher.group(7);
			
			startPos = matcher.regionEnd() + 1;
			
			addCandidatePair(extractedPairs, mainNoun+" "+part1, part1_short);
			addCandidatePair(extractedPairs, mainNoun+" "+part2, part2_short);
			addCandidatePair(extractedPairs, mainNoun+" "+part3, part3_short);
		}
		
		return extractedPairs;
	}
	
	protected List<Acronym> extractHeadNounPattern_2Parts(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		String nounExp = "([a-zA-Z0-9\\-]{1,20})";
		String shortFormExp = "\\(([^\\(]*?)\\)";
		
		Matcher matcher = Pattern.compile (nounExp+" "+nounExp+" "+shortFormExp+",? and "+nounExp+" "+shortFormExp).matcher(text);
		int startPos = 0;
		while (startPos < text.length() && matcher.find(startPos))
		{
			String mainNoun = matcher.group(1);
			
			String part1 = matcher.group(2);
			String part1_short = matcher.group(3);
			String part2 = matcher.group(4);
			String part2_short = matcher.group(5);
			
			startPos = matcher.regionEnd() + 1;
			
			addCandidatePair(extractedPairs, mainNoun+" "+part1, part1_short);
			addCandidatePair(extractedPairs, mainNoun+" "+part2, part2_short);
		}
		
		return extractedPairs;
	}
	
	protected List<Acronym> extractTrailingNounPattern_3Parts(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		String finalNounExp = "([a-zA-Z0-9\\-]{1,20})";
		String nounExp = "(.{1,20}?)";
		String shortFormExp = "\\(([^\\(]*?)\\)";
		
		Matcher matcher = Pattern.compile (nounExp+" "+shortFormExp+",? "+nounExp+" "+shortFormExp+",? and "+nounExp+" "+shortFormExp+" "+finalNounExp).matcher(text);
		int startPos = 0;
		while (startPos < text.length() && matcher.find(startPos))
		{
			String part1 = matcher.group(1);
			String part1_short = matcher.group(2);
			String part2 = matcher.group(3);
			String part2_short = matcher.group(4);
			String part3 = matcher.group(5);
			String part3_short = matcher.group(6);
			String mainNoun = matcher.group(7);
			
			startPos = matcher.regionEnd() + 1;
			
			addCandidatePair(extractedPairs, part1 + " " + mainNoun, part1_short);
			addCandidatePair(extractedPairs, part2 + " " + mainNoun, part2_short);
			addCandidatePair(extractedPairs, part3 + " " + mainNoun, part3_short);
		}
		
		return extractedPairs;
	}
	
	protected List<Acronym> extractTrailingNounPattern_2Parts(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();
		
		String finalNounExp = "([a-zA-Z0-9\\-]{1,20})";
		String nounExp = "(.{1,20}?)";
		String shortFormExp = "\\(([^\\(]*?)\\)";
		
		Matcher matcher = Pattern.compile (nounExp+" "+shortFormExp+",? and "+nounExp+" "+shortFormExp+" "+finalNounExp).matcher(text);
		int startPos = 0;
		while (startPos < text.length() && matcher.find(startPos))
		{
			String part1 = matcher.group(1);
			String part1_short = matcher.group(2);
			String part2 = matcher.group(3);
			String part2_short = matcher.group(4);
			String mainNoun = matcher.group(5);
			
			startPos = matcher.regionEnd() + 1;
			
			addCandidatePair(extractedPairs, part1 + " " + mainNoun, part1_short);
			addCandidatePair(extractedPairs, part2 + " " + mainNoun, part2_short);
		}
		
		return extractedPairs;
	}

	protected List<Acronym> extractSingleAcronyms(String text){
		ArrayList<Acronym> extractedPairs = new ArrayList<Acronym>();

		int iOpen = text.indexOf("(");
		int iClose = -1;
		String mOutOfPar = "";
		String mInPar = "";

		while(iOpen != -1){
			iClose = -1;
			int numPar = 0;
			for (int p = iOpen+1; p < text.length(); p++) {
				if(text.charAt(p) == '('){
					numPar++;
				}
				if(text.charAt(p) == ')'){
					if(numPar > 0)
						numPar--;
					else{
						iClose = p;
						break;
					}
				}
			}
			if(iClose != -1){
				mInPar = text.substring(iOpen+1, iClose);
				mOutOfPar = text.substring(0, iOpen).trim();
				
				addCandidatePair(extractedPairs, mOutOfPar, mInPar);
			}

			iOpen = text.indexOf("(", iOpen+1);
		}

		return extractedPairs;
	}

	protected Acronym parseCandidate(String outOfParenthesis, String inParenthesis) {

		if(inParenthesis.indexOf(";") != -1){
			int i = inParenthesis.indexOf(";");
			inParenthesis = inParenthesis.substring(0, i);
		}

		if(outOfParenthesis.indexOf(";") != -1){
			int i = outOfParenthesis.indexOf(";");
			outOfParenthesis = outOfParenthesis.substring(i+1);
		}

		// Default assumption: long form is outside the parenthesis 
		String shortForm = inParenthesis.trim();
		String longForm = outOfParenthesis.trim();

		// Unless default was found not to be true
		if(!isShortForm(shortForm)){
			longForm = inParenthesis.trim();

			String parts[] = outOfParenthesis.trim().split(" ");
			shortForm = parts[parts.length-1];

		}
		// Is the short form valid?
		if(!isValidShortForm(shortForm)){
			return null;
		}

		if(!isValidExpression(shortForm) || ! isValidExpression(longForm)){
			return null;
		}

		// Chunk long form to correct size
		String parts[] = longForm.split(" ");
		int sfSize = shortForm.length();
		int maxLongFormLength = Math.min( sfSize+5, sfSize*2 );
		int finalLfSize = Math.min( maxLongFormLength, parts.length );


		String finalLongForm = "";
		for (int i = parts.length-1; i > parts.length-finalLfSize-1; i--) {
			finalLongForm = parts[i] + " " + finalLongForm;
		}
		finalLongForm = finalLongForm.trim();

		if(shortForm.equalsIgnoreCase(finalLongForm)){
			return null;
		}

		return new Acronym(shortForm, finalLongForm);
	}
	
	protected String chunkLongForm(String longForm, int size){
		int foundWords = 0;
		int i = longForm.length()-1;
		for (; i >= 0 && foundWords < size ; i--) {
			if(i == 0 || !Character.isLetterOrDigit(longForm.charAt(i-1))){
				foundWords++;
			}
		}
		return longForm.substring(i+1, longForm.length());
	}

	protected boolean isValidExpression(String exp){
		if(		exp == null 
				||
				exp.isEmpty() 
		){
			return false;
		}
		return true;
	}

	protected boolean isShortForm(String candidate) {
		String parts[] = candidate.split(" ");
		return parts.length <= 3;
	}

	protected boolean isValidShortForm(String candidate) {
		// length restriction
		if(candidate.length() > 15)
			return false;
		// length restriction
		if(candidate.length() < 1)
			return false;
		// first char is alpha-numeric
		if(!Pattern.matches("^[a-zA-Z0-9].*", candidate))
			return false;
		// at least one of these characters is a letter
		if(!Pattern.matches(".*[a-zA-Z].*", candidate))
			return false;

		return true;
	}
	
	public List<Double> getEmmisions(){
		return _abbvHmm.getEmmisionParams();
	}
	public List<Double> getTransitions(){
		return _abbvHmm.getTransitionParams();
	}
	
	public void setStartingParams(List<Double> emmisions, List<Double> transitions){
		_abbvHmm.setStartingParams(emmisions, transitions);
	}

}
