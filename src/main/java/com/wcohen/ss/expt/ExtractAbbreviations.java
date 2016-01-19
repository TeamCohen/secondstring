package com.wcohen.ss.expt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wcohen.ss.abbvGapsHmm.Acronym;
import com.wcohen.ss.abbvGapsHmm.AlignmentPredictionModel;

/**
 * Extracts abbreviation pairs (<<i>short-form</i>, <i>long-form</i>>) from text using an 'abbreviation distance metric' which evaluates 
 * the probability of a short-form string being an abbreviation/acronym of another long-form string. 
 * The probability is given by an HMM-based alignment between the two strings.
 * <br><br>
 * Sample command line:<br>
 * <code> java com.wcohen.ss.expt.ExtractAbbreviations ./train/abbvAlign_corpus.txt experiment_name </code>
 * <br><br>
 * Citation: Dana Movshovitz-Attias and William Cohen, Alignment-HMM-based Extraction of Abbreviations from Biomedical Text, 2012, BioNLP in NAACL
 *
 * @see com.wcohen.ss.AbbreviationAlignment
 * @author Dana Movshovitz-Attias
 *
 */
public class ExtractAbbreviations {
	public class Stats {
		public int FN, FP, TP, TN;
		public float precision, recall, F1;
		public Stats(){
			FN = 0;
			FP = 0;
			TN = 0;
			FP = 0;
			precision = 0f;
			recall = 0f;
			F1 = 0f;
		}
	}
	
	public static String SEPARATOR = "#_#";
	
	private String _input;
	private String _output;
	private String _gold;
	private String _train = "./train";
	
	private AlignmentPredictionModel _alignPredictor = null;
	
	private Map<String, Integer> _strToID = null;
	private Map<Integer, Set<String>> _idToStr = null;
	private Map<String, String> _strToSrc = null;
	
	public ExtractAbbreviations(String input, String output, String train, String gold) {
		_input = input;
		_output = output;
		_train = train;
		_gold = gold;
	}
	
	public void run() throws IOException {
		loadPredictor();
		setTrainDir(_train);
		
		predictAndTest(AlignmentPredictionModel.loadTrainingCorpus(_input), AlignmentPredictionModel.loadLabels(_gold));
	}
	
	protected void mkdir(String dir) {
		File f = new File(dir);
		f.mkdirs();
	}
	
	protected void setTrainDir(String trainDir) {
		_alignPredictor.setTrainingDataDir(trainDir+"/");
		_alignPredictor.setModelParamsFile(trainDir+"/hmmModelParams.txt");
		_alignPredictor.trainIfNeeded();
	}
	
	protected AlignmentPredictionModel loadPredictor(){
		if(_alignPredictor == null){
			try {
				_alignPredictor = new AlignmentPredictionModel();
			} catch (IOException e) {
				System.err.println("Unable to load AlignmentPredictionModel");
				e.printStackTrace();
				System.exit(1);
			}
		}
		return _alignPredictor;
	}
	
	protected void predictAndTest(List<String> corpus, List<Map<String, String>> trueLabels) throws IOException{
		Stats totalStats = new Stats();
		
		String output_abbvs = "./"+_output+"_abbvs";
		String output_strings = "./"+_output+"_strings";
		BufferedWriter bw_abbvs = new BufferedWriter(new FileWriter(output_abbvs));
		BufferedWriter bw_strings = new BufferedWriter(new FileWriter(output_strings));
		
		_strToID = new HashMap<String, Integer>();
		_idToStr = new HashMap<Integer, Set<String>>();
		_strToSrc = new HashMap<String, String>();
		
		// iterate over all documents in the corpus
		for(int docID = 0; docID < corpus.size(); ++docID){
			Stats currStats = predictAndTest(docID, corpus, trueLabels, bw_abbvs);
			if(trueLabels!= null){
				totalStats.TP += currStats.TP;
				totalStats.FP += currStats.FP;
				totalStats.FN += currStats.FN;
				totalStats.precision += currStats.precision;
				totalStats.recall += currStats.recall;
				totalStats.F1 += currStats.F1;
			}
		}
		
		outputPairs(bw_strings);
		
		bw_abbvs.close();
		bw_strings.close();
		
		if(trueLabels!= null){
			System.out.println("Avg TP: "+(totalStats.TP / (double)corpus.size()));
			System.out.println("Avg FP: "+(totalStats.FP / (double)corpus.size()));
			System.out.println("Avg Precision: "+(totalStats.precision / (double)corpus.size()));
			System.out.println("Avg Recall: "+(totalStats.recall / (double)corpus.size()));
			System.out.println("Avg F1: "+(totalStats.F1 / (double)corpus.size()));
			
			float tot_precision, tot_recall, tot_F1;
			if(totalStats.TP+totalStats.FP == 0){
				tot_precision = 1f;
			}
			else{
				tot_precision = new Float(totalStats.TP) / new Float(totalStats.TP+totalStats.FP);
			}
			tot_recall = totalStats.TP / new Float(totalStats.TP+totalStats.FN);
			tot_F1 = 2* ((tot_precision*tot_recall) / (tot_precision+tot_recall));
			System.out.println("Total Precision: "+(tot_precision / (double)corpus.size()));
			System.out.println("Total Recall: "+(tot_recall / (double)corpus.size()));
			System.out.println("Total F1: "+(tot_F1 / (double)corpus.size()));
		}
	}
		
	protected String outputAbbvs(Map<String, Acronym> predictions) {
		String out = "";
		for (String sf : predictions.keySet()) {
			String lf = predictions.get(sf)._longForm;
			out += sf + "\t" + lf + "#_#";
		}
		return out;
	}
	
	protected void addAbbreviationPairs(Map<String, Acronym> predictions) {
		for (String sf : predictions.keySet()) {
			String lf = predictions.get(sf)._longForm;
			Integer sf_id = _strToID.get(sf);
			Integer lf_id = _strToID.get(lf);
			
			if (sf_id == null && lf_id == null){
				Integer id = _strToID.size();
				_strToID.put(sf, id);
				_strToID.put(lf, id);
				_idToStr.put(id, new HashSet<String>());
				_idToStr.get(id).add(sf);
				_idToStr.get(id).add(lf);
			}
			else if (sf_id == null && lf_id != null) {
				_strToID.put(sf, lf_id);
				_idToStr.get(lf_id).add(sf);
			}
			else if (lf_id == null && sf_id != null) {
				_strToID.put(lf, sf_id);
				_idToStr.get(sf_id).add(lf);
			}
			else if (sf_id != lf_id) {
				_strToID.put(lf, sf_id);
				for (String str : _idToStr.get(lf_id)) {
					_strToID.put(str, sf_id);
					_idToStr.get(sf_id).add(str);
				}
				_idToStr.remove(lf_id);
			}
			
			_strToSrc.put(sf, "short");
			_strToSrc.put(lf, "long"); 
		}
	}
	
	protected void outputPairs(BufferedWriter bw) throws IOException {
		Integer ids[] = _idToStr.keySet().toArray(new Integer[0]);
		for (int newId = 0; newId < ids.length; newId++) {
			int oldId = ids[newId];
			for (String str : _idToStr.get(oldId)) {
				bw.write(_strToSrc.get(str) + "\t" + newId + "\t" + str + "\n");
			}
		}
	}
	
	protected Stats predictAndTest(int docID, List<String> corpus, List<Map<String, String>> trueLabels, BufferedWriter bw_abbvs) 
	throws IOException {
		// predict
		String text = corpus.get(docID);
		Collection<Acronym> all_predictions = _alignPredictor.predict(text);
		Map<String, Acronym> final_predictions = _alignPredictor.acronymsArrayToMap(all_predictions);
		
		bw_abbvs.write(outputAbbvs(final_predictions)+"\n");
		addAbbreviationPairs(final_predictions);

		// test
		if(trueLabels != null){
			Map<String, String> docTrueLabels = trueLabels.get(docID);
			Stats stats = new Stats();
			
			stats.FN = docTrueLabels.size();
			stats.TP = 0;
			stats.FP = 0;
			for (String shortFort : final_predictions.keySet()) {
				String predictedLongForm = final_predictions.get(shortFort)._longForm;
				if(predictedLongForm == null){
					stats.FP++;
				}
				else{
					String trueLongForm = docTrueLabels.get(shortFort);
					if(predictedLongForm.toLowerCase().equals(trueLongForm.toLowerCase())){
						stats.FP++;
					}
					else{
						stats.TP++;
						stats.FN--;
					}
				}
			}
			
			if(stats.TP+stats.FP == 0){
				stats.precision = 1f;
			}
			else{
				stats.precision = new Float(stats.TP) / new Float(stats.TP+stats.FP);
			}
			stats.recall = stats.TP / new Float(stats.TP+stats.FN);
			stats.F1 = 2 * ((stats.precision*stats.recall) / (stats.precision+stats.recall));
			return stats;
		}
		return null;
	}

	/**
	 * Extracts abbreviation pairs from text.<br><br>
	 * Usage: ExtractAbbreviations input experiment_name [gold-file] [train-dir] 
	 */
	public static void main(String[] args) {
		if(args.length < 2){
			System.out.println("Usage: ExtractAbbreviations input experiment_name [gold-file] [train-dir] \n\n"+
					   "input - Corpus file (one line per file) from which abbreviations will be extracted.\n"+
					   "experiment_name - The experiment name will be used to create these output files:\n"+
					   "                 './<name>_abbvs' - contains the abbreviations extracted from the corpus, in a format similar to './train/abbvAlign_pairs.txt', "+
					   "the abbreviations from each document are concatenated to one line.\n"+
					   "                 './<name>_strings' - contains pairs of short and long forms of abbreviations extracted from the corpus, "+
					   "in a format that can be used for a matching experiment (using MatchExpt, AbbreviationsBlocker, and AbbreviationAlignment distance)."+
					   "train - Optional. Directory containing a corpus file named 'abbvAlign_corpus.txt' for training the abbreviation HMM. "+
					   "Corpus format is one line per file.\n"+
					   "                 The model parameters will be saved in this directory under 'hmmModelParams.txt' so the HMM will only have to be trained once.\n"+
					   "                 Default = './train/'\n"+
					   "gold - Optional. If available, the gold data will be used to estimate the performance of the HMM on the input corpus.\n"+
					   "                 './train/abbvAlign_pairs.txt' is a sample gold file for the 'train/abbvAlign_corpus.txt corpus.'\n"+
					   "                 Default = by default, no gold data is given and no estimation is done."
					   );
			System.exit(1);
		}
			
		String input = args[0];
		String output = args[1];
		
		String gold = null;
		if(args.length > 2)
			gold = args[2];
		
		String train = "./train";
		if(args.length > 3)
			train = args[3];
		
		ExtractAbbreviations tester = new ExtractAbbreviations(input, output, train, gold);
		try {
			tester.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
