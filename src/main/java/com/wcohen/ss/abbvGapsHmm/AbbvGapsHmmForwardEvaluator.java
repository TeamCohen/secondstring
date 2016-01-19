package com.wcohen.ss.abbvGapsHmm;

import java.util.List;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.Emissions;
import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.States;
import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.Transitions;

/**
 * @author Dana Movshovitz-Attias 
 */
public class AbbvGapsHmmForwardEvaluator 
		extends 
			AbbvGapsHMMEvaluator {

	protected class ForwardEvalParam extends EvalParam{	
		/**
		 * 
		 */
		public ForwardEvalParam(String str) {
			super(str);
			_eval_start = 0;
			_eval_end = _length;
		}
		
		public void advanceEvalRange(){ 
			_current++;
			_currentStringPos = _current-1;
		}
		
		public boolean isInEvalRange(){ return _current <= _eval_end; }
		
		public boolean isInStringMatchingRange() { return _currentStringPos >= _eval_start; }
		
		public int offset(int offset) { return _current-offset; }
		
		public boolean isCurrentPartialWordMatchPositionAtWordStart() { 
			return _current == _eval_start;
		}
		
		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#initEvalRange()
		 */
		@Override
		public void initEvalRange() {
			_current = _eval_start;
			_currentStringPos = _current-1;
		}
		
		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#setPartialWord()
		 */
		@Override
		public void setPartialWord(String str, boolean isAtWordStart) {
			_length = str.length();
			_eval_start = 0;
			_eval_end = _length-1;
			_partialWordIsAtStart = isAtWordStart;
			_partialWord = str;
		}
		
		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#getCurrentPartialWordLen()
		 */
		@Override
		public int getCurrentPartialWordLen() {
			return _length-_current;
		}
		
		/* (non-Javadoc)
		 * @see Structures.AbbvGapsHMMEvaluator.EvalParam#getCurrentPartialWordMatchPosition()
		 */
		@Override
		public int getCurrentPartialWordMatchPosition() {
			return _current;
		}
	}
	
	protected List<Double> _transitionParams;
	protected List<Double> _emissionParams;

	public AbbvGapsHmmForwardEvaluator(AbbvGapsHMM abbvGapsHMM) {
		super(abbvGapsHMM);
	}
	
	public void forwardEvaluate(Acronym acronym, List<Double> transitionParams, List<Double> emissionParams){
		
		_transitionParams = transitionParams;
		_emissionParams = emissionParams;
		
		super.evaluate(acronym);
	}
	
	protected void updateLegalOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		_evalMat.add(currS, currL, currState.ordinal(), 
				_evalMat.at(prevS, prevL, prevState.ordinal())*_emissionParams.get(emission.ordinal())*_transitionParams.get(transition.ordinal()));
	}
	
	protected String getCurrentChars(String str, int pos, int length){
		int startIndex = Math.max(pos-length+1, 0);
		return str.substring(startIndex, pos+1);
	}
	
	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#getCurrentWord(java.lang.String, int)
	 */
	@Override
	protected String getCurrentWord(String str, int pos) {
		return getEndedWord(str, pos);
	}
	
	protected EvalParam getPartialWordParam(String str, int pos) {
		EvalParam partialWordPraram = new ForwardEvalParam("");
		String partialWordStr = getPartialEndedWord(str, pos);
		if(partialWordStr == null)
			return null;
		partialWordPraram.setPartialWord(partialWordStr, posIsAtWordStart(str, pos-partialWordStr.length()+1));
		return partialWordPraram;
	}
	
	/**
	 * If pos is ending a word in str: returns this word.
	 * Else: returns null;
	 */
	protected static String getPartialEndedWord(String str, int pos){
		assert(pos < str.length() && pos >= 0);

		if(	posIsAtWord(str, pos) ){
			int prevSpace = findLastNonLetterOrDigit(str, pos);

			return str.substring(prevSpace+1, pos+1);
		}
		return null;
	}
	
	/**
	 * If pos is ending a word in str: returns this word.
	 * Else: returns null;
	 */
	protected static String getEndedWord(String str, int pos){
		assert(pos < str.length() && pos >= 0);

		if(	posIsAtWordEnd(str, pos) ){
			int prevSpace = findLastNonLetterOrDigit(str, pos);

			return str.substring(prevSpace+1, pos+1);
		}
		return null;
	}
	
	protected static boolean posIsAtWordEnd(String str, int pos){
		assert(pos < str.length() && pos >= 0);
		//		return pos == 0 || Character.isWhitespace(str.charAt(pos-1));
		return Character.isLetterOrDigit(str.charAt(pos)) &&
		(pos == str.length()-1 || !Character.isLetterOrDigit(str.charAt(pos+1)));
	}
	
	protected static int findLastNonLetterOrDigit(String str, int startPos){
		int lastNonWord = -1;
		for (int i = startPos-1; i >= 0; i--) {
			if(!Character.isLetterOrDigit(str.charAt(i))){
				lastNonWord = i;
				break;
			}
		}
		return lastNonWord;
	}


	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalParams(Structures.Acronym)
	 */
	@Override
	protected void initEvalParams() {
		_sParam = new ForwardEvalParam(_acronym._shortForm);
		_lParam = new ForwardEvalParam(_acronym._longForm);
		_partialWordParam = null;
	}

	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalMat()
	 */
	@Override
	protected void initEvalMat() {
		_evalMat.set(_sParam.getRangeStart(), _lParam.getRangeStart(), States.S.ordinal(), 1);
	}
	
	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalMat()
	 */
	@Override
	protected void finalizeEvalMat() {
		updateState(States.END, _sParam.val(), _lParam.val(), _sParam.offset(1), _lParam.offset(1), Emissions.e_END_end);
	}
	
	protected boolean transitionIsLegal(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		boolean legal = true;
		
		// First match must be to word start
		if(transition.equals(Transitions.t_DL_to_M) || transition.equals(Transitions.t_S_to_M)){
			if(! (posIsAtWordStart(_acronym._longForm, _lParam.getEvalStringPos()) || _currPartialWord != null || _currWord != null )){
				legal = false;
			}
		}
		
		return legal && super.transitionIsLegal(currS, currL, currState, prevS, prevL, prevState, transition, emission);
	}
}
