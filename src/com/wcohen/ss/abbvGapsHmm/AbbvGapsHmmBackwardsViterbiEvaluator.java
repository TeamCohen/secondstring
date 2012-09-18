package com.wcohen.ss.abbvGapsHmm;

import java.util.ArrayList;
import java.util.List;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.*;

/**
 * @author Dana Movshovitz-Attias 
 */
public class AbbvGapsHmmBackwardsViterbiEvaluator
		extends
			AbbvGapsHmmBackwardsEvaluator {
	
	protected Matrix3D _alpha;
	protected Matrix3D _beta;
	
	protected Matrix3D _emissions;
	protected Matrix3D _states;
	protected Matrix3D _prevSF;
	protected Matrix3D _prevLF;
	protected Matrix3D _prevSF_stringPos;
	protected Matrix3D _prevLF_stringPos;
	
	protected Double _bestProb;
	protected Double _currProb;

	public AbbvGapsHmmBackwardsViterbiEvaluator(AbbvGapsHMM abbvGapsHMM) {
		super(abbvGapsHMM);
	}
	
	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> backwardViterbiEvaluate(Acronym acronym, List<Double> transitionParams, List<Double> emissionParams){
		_transitionParams = transitionParams;
		_emissionParams = emissionParams;
		
		super.evaluate(acronym);
		
		return findMostProbablePathBackwards(acronym);
	}
	
	/* (non-Javadoc)
	 * @see Structures.AbbvGapsHMMEvaluator#initEvalMat()
	 */
	@Override
	protected void initEvalMat() {
		_emissions 			= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		_states 			= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		_prevSF 			= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		_prevLF 			= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		_prevSF_stringPos 	= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		_prevLF_stringPos 	= new Matrix3D(_evalMat.dimension1(), _evalMat.dimension2(), _evalMat.dimension3());
		
		super.initEvalMat();
	}
	
	protected int getLegalStringPos(int strPos, String str){
		int legalStrPos = strPos;
		if(strPos > str.length() || strPos == -1){
			legalStrPos = str.length();
		}
		
		return legalStrPos;
	}
	
	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> findMostProbablePathBackwards(Acronym acronym){
		int sPos = _sParam.getRangeEnd();
		int lPos = _lParam.getRangeEnd();
		int state = States.S.ordinal();
		int stringPosS = -1;
		int stringPosL = -1;
		
		if(_evalMat.at(0, 0, States.S.ordinal()) == 0){
			return null;
		}
		
		List<Emissions> emmisionsPath = new ArrayList<AbbvGapsHMM.Emissions>();
		List<States> statesPath = new ArrayList<AbbvGapsHMM.States>();
		
		List<String> lAlign = new ArrayList<String>();
		List<String> sAlign = new ArrayList<String>();
		
		Emissions[] emissionValues = Emissions.values();
		States[] stateValues = States.values();
		
		Emissions currEmission;
		Integer currSF;
		Integer currLF;
		Integer currState;
		Integer currSF_stringPos;
		Integer currLF_stringPos;
		
		while(sPos < _sParam.getEvalMatrixSize()-1 || lPos < _lParam.getEvalMatrixSize()-1){
			currEmission 		= emissionValues[ (int) Math.round(_emissions.at(sPos, lPos, state)) ];
			currSF 				= (int) Math.round(_prevSF.at(sPos, lPos, state));
			currLF 				= (int) Math.round(_prevLF.at(sPos, lPos, state));
			currState 			= (int) Math.round(_states.at(sPos, lPos, state));
			currSF_stringPos	= (int) Math.round(_prevSF_stringPos.at(sPos, lPos, state));
			currLF_stringPos	= (int) Math.round(_prevLF_stringPos.at(sPos, lPos, state));
			
			if(stringPosS != -1 && stringPosL != -1){
				
				stringPosS = getLegalStringPos(stringPosS, acronym._shortForm);
				stringPosL = getLegalStringPos(stringPosL, acronym._longForm);
				
				lAlign.add(acronym._longForm.substring(stringPosL, currLF_stringPos));
				sAlign.add(acronym._shortForm.substring(stringPosS, currSF_stringPos));
			}
			
			emmisionsPath.add(currEmission);
			statesPath.add(stateValues[currState]);
			
			sPos 		= currSF;
			lPos 		= currLF;
			state 		= currState;
			stringPosS 	= currSF_stringPos;
			stringPosL 	= currLF_stringPos;
		}
		
		return new AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States>(sAlign, lAlign, emmisionsPath, statesPath, _evalMat.at(0, 0, States.S.ordinal()));
	}
	
	protected void updateLegalOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		Double prevProb = _evalMat.at(currS, currL, prevState.ordinal());
		Double currProb = _evalMat.at(prevS, prevL, currState.ordinal())*_emissionParams.get(emission.ordinal())*_transitionParams.get(transition.ordinal());
		
		if(prevProb.compareTo(currProb) < 0){
			// Save best probability
			_evalMat.set(currS, currL, prevState.ordinal(), currProb);
			// Save emission with best probability
			_emissions.set(currS, currL, prevState.ordinal(), emission.ordinal());
			// Save previous location that produced best probability
			_states.set(currS, currL, prevState.ordinal(), currState.ordinal());
			_prevSF.set(currS, currL, prevState.ordinal(), prevS);
			_prevLF.set(currS, currL, prevState.ordinal(), prevL);
			_prevSF_stringPos.set(currS, currL, prevState.ordinal(), _sParam.getEvalStringPos());
			_prevLF_stringPos.set(currS, currL, prevState.ordinal(), _lParam.getEvalStringPos());
		}
	}

}
