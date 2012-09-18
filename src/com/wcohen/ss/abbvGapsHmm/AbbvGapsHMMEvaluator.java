package com.wcohen.ss.abbvGapsHmm;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.*;

/**
 * @author Dana Movshovitz-Attias 
 */
public abstract class AbbvGapsHMMEvaluator {
	
	protected AbbvGapsHMM _gapsHMM = null;
	
	public AbbvGapsHMMEvaluator(AbbvGapsHMM abbvGapsHMM){
		_gapsHMM = abbvGapsHMM;
	}
	
	protected abstract class EvalParam{
		
		public int _eval_start;
		public int _eval_end;
		public int _eval_mat_size;
		public int _length;
		
		public int _current = -1;
		public int _currentStringPos = -1;
		
		public boolean _partialWordIsAtStart = false;
		public String _partialWord = null;
		
		/**
		 * 
		 */
		public EvalParam(String str) {
			_length = str.length();
			_eval_mat_size = _length+2;
		}
		
		public int val() { return _current; }
		
		/**
		 * Init evaluation range
		 */
		public abstract void initEvalRange();
		
		/**
		 * Advance within evaluation range
		 */
		public abstract void advanceEvalRange();
		
		/**
		 * Is in evaluation range
		 */
		public abstract boolean isInEvalRange();
		
		public boolean isAtRangeStart() { return _current == _eval_start; }
		
		public boolean isAtRangeEnd() { return _current == _eval_start; }
		
		public abstract boolean isInStringMatchingRange();
		
		public int getEvalStringPos() { return _currentStringPos; }
		
		public abstract int offset(int offset);
		
		public int getEvalMatrixSize() { return _eval_mat_size;	}
		
		public int getRangeStart() { return _eval_start; }
		
		public int getRangeEnd() { return _eval_end; }
		
		public abstract void setPartialWord(String str, boolean isAtWordStart);
		
		public abstract int getCurrentPartialWordLen();
		
		public abstract int getCurrentPartialWordMatchPosition();
		
		public abstract boolean isCurrentPartialWordMatchPositionAtWordStart();
		
	};
	
	protected abstract void updateLegalOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission);
	
	protected abstract void initEvalMat();
	
	protected abstract void finalizeEvalMat();
	
	/**
	 * Get the current long form word going backwards or forwards (depending on the implementing class).
	 */
	protected abstract String getCurrentWord(String str, int pos);
	
	/**
	 * Get characters from the string going backwards or forwards (depending on the implementing class), 
	 * and according to the specifies length.
	 */
	protected abstract String getCurrentChars(String str, int pos, int length);
	
	protected abstract EvalParam getPartialWordParam(String str, int pos);
	
	protected abstract void initEvalParams();
	
	
	protected Matrix3D _evalMat;

	protected EvalParam _sParam;
	protected EvalParam _lParam;
	protected EvalParam _partialWordParam = null;
	
	protected Acronym _acronym;
	
	protected String _currWord;
	protected String _currPartialWord;
	
	public Matrix3D getEvalMatrix(){
		return _evalMat;
	}
	
	/**
	 * This function enumerates the possible transitions and emissions in a hard coded way, in order to save running-time
	 */
	protected void evaluate(Acronym acronym){
		
		_acronym = acronym;
		
		initEvalParams();
		
		// Number of states
		int V = States.values().length;

		_evalMat = new Matrix3D(_sParam.getEvalMatrixSize(), _lParam.getEvalMatrixSize(), V);

		// Iterate over possible alignments
		Integer wordLen = -1;
		int lPos, sPos;
		
		initEvalMat();
		
		for(_sParam.initEvalRange(); _sParam.isInEvalRange(); _sParam.advanceEvalRange()) {
			for (_lParam.initEvalRange(); _lParam.isInEvalRange(); _lParam.advanceEvalRange()) {
				wordLen = null;

				// Current position in evaluated strings
				lPos = _lParam.getEvalStringPos();
				sPos = _sParam.getEvalStringPos();

				// Deletions
				if(_lParam.isInStringMatchingRange()){
					
					_currWord = getCurrentWord(acronym._longForm, lPos);
					if(_currWord != null)
						wordLen = _currWord.length();
					
					_partialWordParam = getPartialWordParam(acronym._longForm, lPos);
					
					if(Character.isLetterOrDigit(acronym._longForm.charAt(lPos))){
						// Deleting leading letter
						updateState(States.DL, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(1), Emissions.e_DL_alphaNumeric_to_none);
						// Deleting inner letter
						updateState(States.D, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(1), Emissions.e_D_alphaNumeric_to_none);
					}
					else{
						// Deleting leading space
						updateState(States.DL, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(1), Emissions.e_DL_nonAlphaNumeric_to_none);
						
						// Deleting inner space
						updateState(States.M, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(1), Emissions.e_M_nonAlphaNumeric_to_none);
					}
					
					if(_currWord != null){
						// Deleting leading word
						updateState(States.DL, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(wordLen), Emissions.e_DL_word_to_none);
						// Deleting inner word
						updateState(States.D, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(wordLen), Emissions.e_D_word_to_none);
						
						// Allow deletion of common word
						if(_gapsHMM.useTDIDF()){
							Double wordDF = _gapsHMM.getDF(_currWord);
							if(wordDF != null){
								updateState(States.M, _sParam.val(), _lParam.val(), _sParam.val(), _lParam.offset(wordLen), Emissions.e_M_commonWordDeletion);
							}
						}
					}
				}//deletions

				// Insertions
				if(_sParam.isInStringMatchingRange()){
					// Allowing digit deletions in the short form can ONLY work if low scoring alignments are discarded.
					// Scores are calculated in AbbreviationAlignment by considering the number of deletions vs. the number
					// of matched words (and not single letters). This makes for a "nice" alignment which usually corresponds
					// with a good alignment.
					
					if(!Character.isLetter(acronym._shortForm.charAt(sPos))){
						// Insertion of non-alphanumeric character in short form
						updateState(States.D, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.val(), Emissions.e_D_none_to_nonAlphaNumeric);
					}
				}

				// Matches
				if(_sParam.isInStringMatchingRange() && _lParam.isInStringMatchingRange()){
					
					if(!Character.isLetter(acronym._shortForm.charAt(sPos)) && charEqualIgnoreCase(acronym._shortForm.charAt(sPos), acronym._longForm.charAt(lPos))){
						// letter_to_letter
						updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(1), Emissions.e_M_letter_to_letter);
					}

					String sfChars_2 = getCurrentChars(acronym._shortForm, sPos, 2);
					if(_currWord != null){
						// AND_to_symbol
						if(_currWord.equalsIgnoreCase("and") && andToSymbolMatch(acronym._shortForm.charAt(sPos))){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_AND_to_symbol);
						}
						// numbers
						else if(_currWord.equalsIgnoreCase("one") && acronym._shortForm.charAt(sPos) == '1'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_one_to_1);
						}
						else if(_currWord.equalsIgnoreCase("two") && acronym._shortForm.charAt(sPos) == '2'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_two_to_2);
						}
						else if(_currWord.equalsIgnoreCase("three") && acronym._shortForm.charAt(sPos) == '3'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_three_to_3);
						}
						else if(_currWord.equalsIgnoreCase("four") && acronym._shortForm.charAt(sPos) == '4'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_four_to_4);
						}
						else if(_currWord.equalsIgnoreCase("five") && acronym._shortForm.charAt(sPos) == '5'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_five_to_5);
						}
						else if(_currWord.equalsIgnoreCase("six") && acronym._shortForm.charAt(sPos) == '6'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_six_to_6);
						}
						else if(_currWord.equalsIgnoreCase("seven") && acronym._shortForm.charAt(sPos) == '7'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_seven_to_7);
						}
						else if(_currWord.equalsIgnoreCase("eight") && acronym._shortForm.charAt(sPos) == '8'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_eight_to_8);
						}
						else if(_currWord.equalsIgnoreCase("nine") && acronym._shortForm.charAt(sPos) == '9'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_nine_to_9);
						}
						// Double letter chemical elements
						else if(_currWord.equalsIgnoreCase("Silver") && sfChars_2.equalsIgnoreCase("Ag")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Silver_Ag);
						}
						else if(_currWord.equalsIgnoreCase("Gold") && sfChars_2.equalsIgnoreCase("Au")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Gold_Au);
						}
						else if(_currWord.equalsIgnoreCase("Copper") && sfChars_2.equalsIgnoreCase("Cu")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Copper_Cu);
						}
						else if(_currWord.equalsIgnoreCase("Iron") && sfChars_2.equalsIgnoreCase("Fe")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Iron_Fe);
						}
						else if(_currWord.equalsIgnoreCase("Mercury") && sfChars_2.equalsIgnoreCase("Hg")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Mercury_Hg);
						}
						else if(_currWord.equalsIgnoreCase("Sodium") && sfChars_2.equalsIgnoreCase("Na")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Sodium_Na);
						}
						else if(_currWord.equalsIgnoreCase("Lead") && sfChars_2.equalsIgnoreCase("Pb")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Lead_Pb);
						}
						else if(_currWord.equalsIgnoreCase("Antimony") && sfChars_2.equalsIgnoreCase("Sb")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Antimony_Sb);
						}
						else if(_currWord.equalsIgnoreCase("Tin") && sfChars_2.equalsIgnoreCase("Sn")){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(2), _lParam.offset(wordLen), Emissions.e_M_Tin_Sn);
						}
						// Single letter chemical elements
						else if(_currWord.equalsIgnoreCase("Potassium") && acronym._shortForm.charAt(sPos) == 'K'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_Potassium_K);
						}
						else if(_currWord.equalsIgnoreCase("Tungsten") && acronym._shortForm.charAt(sPos) == 'W'){
							updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(wordLen), Emissions.e_M_Tungsten_W);
						}
						
					}
					if(_partialWordParam != null){
						// Partial Matches
						_currPartialWord = _partialWordParam._partialWord;
						for (_partialWordParam.initEvalRange(); _partialWordParam.isInEvalRange(); _partialWordParam.advanceEvalRange()) {
							if(charEqualIgnoreCase(acronym._shortForm.charAt(sPos), _currPartialWord.charAt(_partialWordParam.getCurrentPartialWordMatchPosition()))){
								
								if(_partialWordParam.getCurrentPartialWordLen() == 1){
									updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(1), Emissions.e_M_letter_to_letter);
								}
								else if(_partialWordParam.isCurrentPartialWordMatchPositionAtWordStart()){
									updateState(States.M, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(_partialWordParam.getCurrentPartialWordLen()), Emissions.e_M_word_to_firstLetter);
								}
								else{
									updateState(States.M, _sParam.val(), _lParam.val(), 
											_sParam.offset(1), _lParam.offset(_partialWordParam.getCurrentPartialWordLen()), Emissions.e_M_partialWord_to_letter);
								}
							}
						}
					}
				}//matches
			}//l
		}//s
		
		finalizeEvalMat();
	}
	
	protected static boolean posIsAtWordStart(String str, int pos){
		assert(pos < str.length() && pos >= 0);
		return Character.isLetterOrDigit(str.charAt(pos)) &&
		(pos == 0 || !Character.isLetterOrDigit(str.charAt(pos-1)));
	}
	
	protected boolean charEqualIgnoreCase(char c1, char c2){
		boolean charMatch = Character.toLowerCase(c1) == Character.toLowerCase(c2);
		return charMatch;
	}
		
	protected boolean andToSymbolMatch(char c){
		return c == '&' || c == '/' || c == '-';
	}
	
	protected static boolean posIsAtWord(String str, int pos){
		assert(pos < str.length() && pos >= 0);
		return Character.isLetterOrDigit(str.charAt(pos));
	}
	
	protected void updateState(
			States s, 
			int currS, int currL,
			int prevS, int prevL,
			Emissions emission
	){
		switch (s) {
			case M :
				updateOutgoingEdgesStateM(currS, currL, prevS, prevL, emission);
				break;
				
			case D :
				updateOutgoingEdgesStateD(currS, currL, prevS, prevL, emission);
				break;
				
			case DL :
				updateOutgoingEdgesStateDL(currS, currL, prevS, prevL, emission);
				break;
				
			case END :
				updateOutgoingEdgesStateEND(currS, currL, prevS, prevL, emission);
				break;

			default :
				break;
		}
	}
	
	/**
	 * Enumerates the possible transitions into state M.
	 * @param currS
	 * @param currL
	 * @param prevS
	 * @param prevL
	 */
	protected void updateOutgoingEdgesStateM(
			int currS, int currL,
			int prevS, int prevL,
			Emissions emission
	){
		updateOutgoingEdges(currS, currL, States.M, prevS, prevL, States.DL, Transitions.t_DL_to_M, emission);
		updateOutgoingEdges(currS, currL, States.M, prevS, prevL, States.M, Transitions.t_M_in, emission);
		updateOutgoingEdges(currS, currL, States.M, prevS, prevL, States.D, Transitions.t_D_to_M, emission);
		updateOutgoingEdges(currS, currL, States.M, prevS, prevL, States.S, Transitions.t_S_to_M, emission);
	}
	
	/**
	 * Enumerates the possible transitions into state D.
	 */
	protected void updateOutgoingEdgesStateD(
			int currS, int currL,
			int prevS, int prevL,
			Emissions emission
	){
		updateOutgoingEdges(currS, currL, States.D, prevS, prevL, States.D, Transitions.t_D_in, emission);
		updateOutgoingEdges(currS, currL, States.D, prevS, prevL, States.M, Transitions.t_M_to_D, emission);
	}
	
	/**
	 * Enumerates the possible transitions into state DL.
	 */
	protected void updateOutgoingEdgesStateDL(
			int currS, int currL,
			int prevS, int prevL,
			Emissions emission
	){
		updateOutgoingEdges(currS, currL, States.DL, prevS, prevL, States.DL, Transitions.t_DL_in, emission);
		updateOutgoingEdges(currS, currL, States.DL, prevS, prevL, States.S, Transitions.t_S_to_DL, emission);
	}
	
	/**
	 * Enumerates the possible transitions into state END.
	 */
	protected void updateOutgoingEdgesStateEND(
			int currS, int currL,
			int prevS, int prevL,
			Emissions emission
	){
		updateOutgoingEdges(currS, currL, States.END, prevS, prevL, States.D, Transitions.t_D_to_END, emission);
		updateOutgoingEdges(currS, currL, States.END, prevS, prevL, States.M, Transitions.t_M_to_END, emission);
	}
	
	protected void updateOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		if(! transitionIsLegal(currS, currL, currState, prevS, prevL, prevState, transition, emission) )
			return;
		
		updateLegalOutgoingEdges(currS, currL, currState, prevS, prevL, prevState, transition, emission);
		
	}
	
	protected boolean transitionIsLegal(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		return true;
	}

}
