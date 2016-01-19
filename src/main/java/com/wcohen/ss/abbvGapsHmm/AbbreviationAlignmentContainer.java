package com.wcohen.ss.abbvGapsHmm;

import java.util.List;

import com.wcohen.ss.abbvGapsHmm.AbbvGapsHMM.Emissions;

/**
 * @author Dana Movshovitz-Attias
 */
public class AbbreviationAlignmentContainer<T extends Enum<T>, S extends Enum<S>> {
	private List<String> _lAlign;
	private List<String> _sAlign;
	
	private Double _bestProbability;
	
	private List<T> _emissionsPath;
	
	private List<S> _statesPath;
	
	private int _numDeletionsInAcronym = -1;
	private int _acronymScore = 0;
	
	AbbreviationAlignmentContainer(List<String> sAlign, List<String> lAlign, List<T> emissionsPath, List<S> statesPath, Double probability){
		_sAlign = sAlign;
		_lAlign = lAlign;
		_emissionsPath = emissionsPath;
		_statesPath = statesPath;
		_bestProbability = probability;
	}
	
	AbbreviationAlignmentContainer(List<String> sAlign, List<String> lAlign, List<T> emissionsPath, Double probability){
		_sAlign = sAlign;
		_lAlign = lAlign;
		_emissionsPath = emissionsPath;
		_statesPath = null;
		_bestProbability = probability;
	}
	
	public Double getProbability(){
		return _bestProbability;
	}
	
	public List<String> getLAlign(){
		return _lAlign;
	}
	
	public List<String> getSAlign(){
		return _sAlign;
	}
	
	public List<T> retrunBestStates(){
		return _emissionsPath;
	}
	
	protected String getStrByEmission(String str, T emission){
		String out = str;
		if(Emissions.e_END_end.equals(emission) || Emissions.e_M_nonAlphaNumeric_to_none.equals(emission)){
			out = "";
		}
		else if(Emissions.e_M_partialWord_to_letter.equals(emission) ||
				Emissions.e_M_word_to_firstLetter.equals(emission)){
			out = str.substring(0, 1);
		}
		if(Emissions.e_M_AND_to_symbol.equals(emission)){
			out = "&";
		}
		
		return out;
	}
	
	public Acronym getAcronym(){
		String sf = "";
		String lf = "";
		String preSF = "";
		String preLF = "";
		
		boolean started = false;
		int latestWordStart = -1;
		int numDeletions = 0;
		int score = 0;
		
		for (int i = 0; i < _sAlign.size(); i++) {
			String sPart = _sAlign.get(i);
			String lPart = _lAlign.get(i);
			T emission = _emissionsPath.get(i);
			
			if(!started && emission.name().contains("none") ){
				// record latest word start
				if(Character.isDigit(lPart.charAt(0))){
					latestWordStart = i;
				}
				else if(Character.isWhitespace(lPart.charAt(0)) || Character.isLetter(lPart.charAt(0))){
					latestWordStart = -1;
					preSF = "";
					preLF = "";
					numDeletions = 0;
					score = 0;
				}
				// Accumulate header
				if(latestWordStart != -1){
					preSF += sPart;
					preLF += lPart;
					
					if(emission.name().startsWith("e_D_") || emission.name().startsWith("e_DL_")){
						numDeletions++;
						score--;
					}
					else if(emission.name().startsWith("e_M_") || emission.name().contains("word")){
						score++;
					}
				}
				continue;
			}
			started = true;
			
			if(emission.name().startsWith("e_D_") || emission.name().startsWith("e_DL_")){
				numDeletions++;
				score--;
			}
			else if(emission.name().startsWith("e_M_") && ( emission.name().contains("word") || emission.name().contains("Word") )){
				score++;
			}
			
			sf += sPart;
			lf += lPart;
		}
		
		if(sf.isEmpty() || lf.isEmpty())
			return null;
		
		if(sf.equalsIgnoreCase(lf))
			return null;
		
		String finalSF = preSF+sf;
		String finalLF = preLF+lf;
		
		// Single letter SF can only match one word
		if(finalSF.length() == 1 && finalLF.indexOf(" ") != -1)
			return null;
		
		_numDeletionsInAcronym = numDeletions;
		_acronymScore = score;
		
		if(_acronymScore < 0)
			return null;
		
		return new Acronym(finalSF, finalLF);
	}
	
	public int getAcronymScore() {
		return _acronymScore;
	}
	
	public int getNumDeletionsInAcronym() {
		return _numDeletionsInAcronym;
	}
	
	public String toString(){
		if(_statesPath == null)
			return toStringNoStates();
		return toStringWithStates();
	}
	
	public String toStringNoStates(){
		String sFinal = "";
		String lFinal = "";
		
		int maxLen;
		
		for (int i = 0; i < _sAlign.size(); i++) {
			String sPart = _sAlign.get(i);
			String lPart = _lAlign.get(i);
			
			maxLen = Math.max(sPart.length(), lPart.length());
			
			sFinal += String.format("%-"+maxLen+"s", sPart) + "|";
			lFinal += String.format("%-"+maxLen+"s", lPart) + "|";
		}
		
		return sFinal+"\n"+lFinal;
	}
	
	public String toStringWithStates(){
		String sFinal = "";
		String lFinal = "";
		String stateFinal = "";
		
		int maxLen;
		
		for (int i = 0; i < _sAlign.size(); i++) {
			String sPart = _sAlign.get(i);
			String lPart = _lAlign.get(i);
			String statePart = _statesPath.get(i).name();
			
			maxLen = Math.max(sPart.length(), lPart.length());
			maxLen = Math.max(maxLen, statePart.length());
			
			stateFinal += String.format("%-"+maxLen+"s", statePart) + "|";
			sFinal += String.format("%-"+maxLen+"s", sPart) + "|";
			lFinal += String.format("%-"+maxLen+"s", lPart) + "|";
		}
		
		return stateFinal+"\n"+sFinal+"\n"+lFinal;
	}
	
	public String toStringWithEmissions(){
		String sFinal = "";
		String lFinal = "";
		String stateFinal = "";
		String emissionsFinal = "Emissions: ";
		
		int maxLen;
		
		for (int i = 0; i < _sAlign.size(); i++) {
			String sPart = _sAlign.get(i);
			String lPart = _lAlign.get(i);
			String statePart = _statesPath.get(i).name();
			String emissionPart = _emissionsPath.get(i).name();
			
			maxLen = Math.max(sPart.length(), lPart.length());
			maxLen = Math.max(maxLen, statePart.length());
			
			emissionsFinal += emissionPart + "|";
			stateFinal += String.format("%-"+maxLen+"s", statePart) + "|";
			sFinal += String.format("%-"+maxLen+"s", sPart) + "|";
			lFinal += String.format("%-"+maxLen+"s", lPart) + "|";
		}
		
		return stateFinal+"\n"+sFinal+"\n"+lFinal+"\n\n"+emissionsFinal;
	}
}
