package com.wcohen.ss.abbvGapsHmm;

import java.util.List;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.*;

/**
 * @author Dana Movshovitz-Attias
 */
public class AbbvGapsHmmBackwardsEvaluator extends AbbvGapsHMMEvaluator {
	
	protected class BackwardEvalParam extends EvalParam{		
		/**
		 * 
		 */
		public BackwardEvalParam(String str) {
			super(str);
			_eval_start = _length;
			_eval_end = 0;
			
			_current = _length;
			_currentStringPos = _length;
		}
				
		public void advanceEvalRange(){ 
			_current--;
			_currentStringPos = _current;
		}
		
		public boolean isInEvalRange(){ return _current >= _eval_end; }
		
		public boolean isInStringMatchingRange() { return _currentStringPos < _eval_start; }
		
		public int offset(int offset) { return _current+offset; }
		
		public boolean isCurrentPartialWordMatchPositionAtWordStart() { return _partialWordIsAtStart; }

		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#initEvalRange()
		 */
		@Override
		public void initEvalRange() {
			_current = _eval_start;
			_currentStringPos = _current;
		}

		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#setPartialWord()
		 */
		@Override
		public void setPartialWord(String str, boolean isAtWordStart) {
			_length = str.length();
			_eval_start = _length-1;
			_eval_end = 0;
			_partialWordIsAtStart = isAtWordStart;
			_partialWord = str;
		}

		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#getCurrentPartialWordLen()
		 */
		@Override
		public int getCurrentPartialWordLen() {
			return _current+1;
		}

		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#getCurrentPartialWordMatchPosition()
		 */
		@Override
		public int getCurrentPartialWordMatchPosition() {
			return 0;
		}

	}
	
	protected List<Double> _transitionParams;
	protected List<Double> _emissionParams;

	public AbbvGapsHmmBackwardsEvaluator(AbbvGapsHMM abbvGapsHMM) {
		super(abbvGapsHMM);
	}
	
	public void backwardEvaluate(Acronym acronym, List<Double> transitionParams, List<Double> emissionParams){
		
		_transitionParams = transitionParams;
		_emissionParams = emissionParams;
		
		super.evaluate(acronym);
	}

	protected void updateLegalOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		_evalMat.add(currS, currL, prevState.ordinal(), 
				_evalMat.at(prevS, prevL, currState.ordinal())*_emissionParams.get(emission.ordinal())*_transitionParams.get(transition.ordinal()));
		
	}
	
	protected boolean transitionIsLegal(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		boolean legal = true;
		
		// Start state can only be used in position (0,0) of the strings
		if(prevState.equals(States.S) && (currS != 0 || currL != 0) )
			legal = false;
		
		// Only the start state can be used at position (0,0)
		if( currS == 0 && currL == 0 && !prevState.equals(States.S))
			legal = false;
		
		// First match must be to word start
		if(transition.equals(Transitions.t_DL_to_M) || transition.equals(Transitions.t_S_to_M)){
			if(!posIsAtWordStart(_acronym._longForm, _lParam.getEvalStringPos())){
				legal = false;
			}
		}
		
		return legal && super.transitionIsLegal(currS, currL, currState, prevS, prevL, prevState, transition, emission);
	}
	
	protected String getCurrentChars(String str, int pos, int length){
		int endIndex = Math.min(pos+length, str.length());
		return str.substring(pos, endIndex);
	}

	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#getCurrentWord(java.lang.String, int)
	 */
	@Override
	protected String getCurrentWord(String str, int pos) {
		return getStartedWord(str, pos);
	}
	
	protected EvalParam getPartialWordParam(String str, int pos) {
		EvalParam partialWordPraram = new BackwardEvalParam("");
		String partialWordStr = getPartialStartedWord(str, pos);
		if(partialWordStr == null)
			return null;
		partialWordPraram.setPartialWord(partialWordStr, posIsAtWordStart(str, pos));
		return partialWordPraram;
	}
	
	/**
	 * If pos is starting a new word in str, returns this word.
	 * Else, returns null.
	 */
	protected static String getPartialStartedWord(String str, int pos){
		assert(pos < str.length() && pos >= 0);

		if(	posIsAtWord(str, pos) ){
			int nextSpace = findNextNonLetterOrDigit(str, pos);
			if(nextSpace == -1){
				nextSpace = str.length();
			}
			
			return str.substring(pos, nextSpace);
		}
		return null;
	}
	
	/**
	 * If pos is starting a new word in str, returns this word.
	 * Else, returns null.
	 */
	protected static String getStartedWord(String str, int pos){
		assert(pos < str.length() && pos >= 0);

		if(	posIsAtWordStart(str, pos) ){
			int nextSpace = findNextNonLetterOrDigit(str, pos);
			if(nextSpace == -1){
				nextSpace = str.length();
			}
			
			return str.substring(pos, nextSpace);
		}
		return null;
	}
	
	protected static int findNextNonLetterOrDigit(String str, int startPos){
		int nextNonWord = -1;
		for (int i = startPos+1; i < str.length(); i++) {
			if(!Character.isLetterOrDigit(str.charAt(i))){
				nextNonWord = i;
				break;
			}
		}
		return nextNonWord;
	}

	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalParams(Structures.Acronym)
	 */
	@Override
	protected void initEvalParams() {
		_sParam = new BackwardEvalParam(_acronym._shortForm);
		_lParam = new BackwardEvalParam(_acronym._longForm);
		_partialWordParam = null;
	}

	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalMat()
	 */
	@Override
	protected void initEvalMat() {
		_evalMat.set(_sParam.getEvalMatrixSize()-1, _lParam.getEvalMatrixSize()-1, States.END.ordinal(), 1);
		updateState(States.END, _sParam.getRangeStart(), _lParam.getRangeStart(), _sParam.getRangeStart()+1, _lParam.getRangeStart()+1, Emissions.e_END_end);
	}
	
	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalMat()
	 */
	@Override
	protected void finalizeEvalMat() {
		int V = States.values().length;
		for(int v = 0; v < V; ++v){
			if(v != States.S.ordinal())
				_evalMat.set(_sParam.getRangeEnd(), _lParam.getRangeEnd(), v, 0);
		}
	}

}
