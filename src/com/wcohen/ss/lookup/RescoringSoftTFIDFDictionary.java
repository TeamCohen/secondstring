package com.wcohen.ss.lookup;

import java.io.*;
import java.util.*;

import com.wcohen.ss.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * Wrapper around a SoftTFIDFDictionary that allows you to 'rescore'
 * the result using an arbitrary StringDistance.
 */

public class RescoringSoftTFIDFDictionary implements FastLookup
{
    private StringDistance rescorer;
    private FastLookup inner;
    private double innerMinScore;
    private ArrayList result;

    public RescoringSoftTFIDFDictionary(FastLookup inner,double innerMinScore,StringDistance rescorer)
    {
        this.inner=inner;
        this.rescorer=rescorer;
        this.innerMinScore=innerMinScore;
    }

    public int lookup(double minScore,String toFind)
    {
        result = new ArrayList();
        int n = inner.lookup(innerMinScore,toFind);
        if (n>0) {
            StringWrapper w = rescorer.prepare(toFind);
            rescore(n,minScore,w);
        }
        //System.out.println("original: "+n+" rescored:  "+result.size());
        return result.size();
    }

    private void rescore(int n,double minScore,StringWrapper w)
    {
        for (int i=0; i<n; i++) {
            String si = inner.getResult(i);
            double di = rescorer.score(w,rescorer.prepare(si));
            //System.out.println("rescore: "+w+"~"+si+" = "+di);
            if (di>=minScore) {
                result.add(new LookupResult(si, inner.getValue(i), di));
                //System.out.println("added to result, size="+result.size());
            }
        }
        Collections.sort(result);
    }

    public String getResult(int i) { return ((LookupResult)result.get(i)).found; }

    public Object getValue(int i) { return ((LookupResult)result.get(i)).value; }

    public double getScore(int i) { return ((LookupResult)result.get(i)).score; }
    
}
