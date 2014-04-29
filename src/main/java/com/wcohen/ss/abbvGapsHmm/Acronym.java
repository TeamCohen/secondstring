package com.wcohen.ss.abbvGapsHmm;
import java.util.Comparator;

/**
 * @author Dana Movshovitz-Attias
 */
public class Acronym implements Comparable<Acronym>{
	public String _shortForm;
	public String _longForm;
	
	public Integer _frequency = null;
	
	public Double _probability = null;
	
	public AbbreviationAlignmentContainer<AbbvGapsHMM.Emissions, AbbvGapsHMM.States> _alignment = null;
	
	int _hashCode;

	public Acronym(String shortForm, String longForm){
		_shortForm = shortForm;
		_longForm = longForm;
		
		_hashCode = (_shortForm+"\t"+_longForm).hashCode();
	}
	
	public Acronym(String shortForm, String longForm, Integer frequency){
		this(shortForm, longForm);
		_frequency = frequency;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Acronym o) {
		int compScore = _shortForm.compareTo(o._shortForm);
		if(compScore == 0){
			compScore = _longForm.compareTo(o._longForm);
		}
		return compScore;
	}

	public boolean equals(Object o){
		if ( this == o ) return true;
		if ( !(o instanceof Acronym) ) return false;
		Acronym oPair = (Acronym)o;
		
		return this.compareTo(oPair) == 0;
	}
	
	public int hashCode(){
		return _hashCode;
	}
	
	
	public static class AcronymShortFormComparator implements Comparator<Acronym>{
	    @Override
	    public int compare(Acronym t1, Acronym t2) {
	        return t1._shortForm.compareTo(t2._shortForm); 
	    }
	}

	public static class AcronymFrequencyComparator implements Comparator<Acronym>{
	    @Override
	    public int compare(Acronym t1, Acronym t2) {
	        return new Integer(t1._frequency).compareTo(t2._frequency); 
	    }
	}
	
	public String toString(){
		return _shortForm+"\t"+_longForm;
	}
	
	public String toStringWithFrequency(){
		return toString()+"\t"+_frequency;
	}
}
