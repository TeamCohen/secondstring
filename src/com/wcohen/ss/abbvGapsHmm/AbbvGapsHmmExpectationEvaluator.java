package com.wcohen.ss.abbvGapsHmm;

import java.util.List;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.*;

/**
 * @author Dana Movshovitz-Attias
 */
public class AbbvGapsHmmExpectationEvaluator
		extends
			AbbvGapsHmmForwardEvaluator {
	
	protected List<Double> _transitionCounters;
	protected List<Double> _emissionCounters;
	
	protected Matrix3D _alpha;
	protected Matrix3D _beta;

	/**
	 */
	public AbbvGapsHmmExpectationEvaluator(AbbvGapsHMM abbvGapsHMM) {
		super(abbvGapsHMM);
	}
	
	public void expectationEvaluate(
			Acronym acronym, 
			List<Double> transitionCounters, List<Double> emissionCounters,
			List<Double> transitionParams, List<Double> emissionParams,
			Matrix3D alpha, Matrix3D beta){
		_transitionCounters = transitionCounters;
		_emissionCounters = emissionCounters;
		_transitionParams = transitionParams;
		_emissionParams = emissionParams;
		_alpha = alpha;
		_beta = beta;
		
		super.evaluate(acronym);
	}
	
	public List<Double> getTransitionCounters(){
		return _transitionCounters;
	}
	
	public List<Double> getEmissionCounters(){
		return _emissionCounters;
	}
	
	protected void updateLegalOutgoingEdges(
			int currS, int currL, States currState,
			int prevS, int prevL, States prevState,
			Transitions transition, Emissions emission
	){
		Double currProb = (	_alpha.at(prevS, prevL, prevState.ordinal())*
				_emissionParams.get(emission.ordinal())*
				_transitionParams.get(transition.ordinal())*
				_beta.at(currS, currL, currState.ordinal()) )
				/ _alpha.at(_alpha.dimension1()-1, _alpha.dimension2()-1, _alpha.dimension3()-1);
		increaseCounter(emission, currProb);
		increaseCounter(transition, currProb);
	}

	protected void increaseCounter(Emissions emission, double addition){
		double tmpCounter = _emissionCounters.get(emission.ordinal());
		tmpCounter += addition;
		_emissionCounters.set(emission.ordinal(), tmpCounter);
	}
	
	protected void increaseCounter(Transitions transition, double addition){
		double tmpCounter = _transitionCounters.get(transition.ordinal());
		tmpCounter += addition;
		_transitionCounters.set(transition.ordinal(), tmpCounter);
	}

}
