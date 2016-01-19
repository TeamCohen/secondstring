package com.wcohen.ss;

import java.io.IOException;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.abbvGapsHmm.*;

/**
 * Abbreviation distance metric which evaluates the probability of a short-form string being an abbreviation/acronym 
 * of another long-form string. The probability is given by an HMM-based alignment between the two strings.
 * <br><br>
 * Sample command line:<br>
 * <code> java com.wcohen.ss.AbbreviationAlignment "DNA" "Deoxyribonucleic acid" </code><br>
 * Expected output:<br>
 * <pre>
 * M        |M      |M|M   |
 * D        |N      | |A   |
 * Deoxyribo|nucleic| |acid|
 * 
 * Probability = 3.674595157620664E-6
 * </pre>
 * where, the rows of the alignment contain: (1) the states of the Viterbi path in the Alignment-HMM, (2) the short form characters, and (3)
 * the long form characters. The probability of the Viterbi path is also given.
 * <br><br>
 * Citation: Dana Movshovitz-Attias and William Cohen, Alignment-HMM-based Extraction of Abbreviations from Biomedical Text, 2012, BioNLP in NAACL
 * 
 * @see com.wcohen.ss.expt.ExtractAbbreviations
 * @author Dana Movshovitz-Attias
 *
 */
public class AbbreviationAlignment extends AbstractStatisticalTokenDistance {
	
	private AlignmentPredictionModel _alignPredictor = null;
	private static String _trainingDir = "./train/";

	/**
	 * Evaluates the probability of the short-form string (string1) being an abbreviation/acronym 
	 * of the long-form string (string2).<br> 
	 * Usage: AbbreviationAlignment short_form_string long_form_string [train_data_dir]
	 */
	static public void main(String[] argv) {
		String[] newArgv = init(argv);
		AbbreviationAlignment aligner = new AbbreviationAlignment();
		doMain(aligner, newArgv);
	}
	
	static public String[] init(String[] argv) {
		if(argv.length < 2){
			System.out.println("Usage: AlignmentPredictionModel short_form_string long_form_string [train_data_dir]\n\n"+
							   "short_form_string long_form_string - Candidate abbreviation strings, for example, \"DNA\" \"Deoxyribonucleic acid\"\n"+
							   "train_data_dir - Optional. Directory containing a corpus file named 'abbvAlign_corpus.txt' for training the abbreviation HMM. "+
							   "Corpus format is one line per file.\n"+
							   "                 The model parameters will be saved in this directory under 'hmmModelParams.txt' so the HMM will only have to be trained once.\n"+
							   "                 Default = './train/'\n\n"+
							   "Example: java com.wcohen.ss.AbbreviationAlignment \"DNA\" \"Deoxyribonucleic acid\"\n"+
							   "Expected output:\n"+
							   "M        |M      |M|M   |\n"+
							   "D        |N      | |A   |\n"+
							   "Deoxyribo|nucleic| |acid|\n"+
							   "\n"+
							   "Probability = 3.674595157620664E-6");
			System.exit(1);
		}
		
		if(argv.length >= 3) {
			AbbreviationAlignment._trainingDir = argv[2];
		}
		String[] newArgv = {argv[0], argv[1]};
		return newArgv;
	}
	
	public AbbreviationAlignment() {
		loadPredictor();
		setTrainDir(_trainingDir);
	}
	
	public void setTrainDir(String trainDir) {
		_trainingDir = trainDir;
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

	@Override
	public double score(StringWrapper s, StringWrapper t) {
		com.wcohen.ss.abbvGapsHmm.AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> alignment = _alignPredictor.predict(s.unwrap(), t.unwrap());

		if(alignment != null){
			return alignment.getProbability();
		}
		return 0;
	}

	@Override
	public String explainScore(StringWrapper s, StringWrapper t) {
		String explainStr = "";
		com.wcohen.ss.abbvGapsHmm.AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> alignment = _alignPredictor.predict(s.unwrap(), t.unwrap());

		if(alignment != null){
			explainStr += alignment.toString()+"\n\n";
			explainStr += "Probability = "+alignment.getProbability();
		}
		else{
			explainStr += "No alignment found between: \""+s.unwrap()+"\", \""+t.unwrap()+"\"";
		}
		return explainStr;
	}

}
