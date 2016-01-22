/**
 * <p>Title: </p> TagLink string distance
 *
 * <p>Description: </p> This is a Hybrid string metric. Token scores are
 * computed by our character based method (TagLinkToken).
 * Matched token pairs are defined by Algorithm1.
 * This hybrid string distance follows notation as described in Camacho & Salhi 2006.
 *
 *
 * @author Horacio Camacho
 *
 * email:       jhcama@essex.ac.uk
 * www:         http://privatewww.essex.ac.uk/~jhcama/
 *
 * address:     Horacio Camacho,
 *              Department of Mathematical Sciences,
 *              University of Essex,
 *              Colchester,
 *              Wivenhoe Park,
 *              CO4 3SQ
 *              United Kingdom,
 *
 * @version 1.0
 */

package com.wcohen.ss;

import java.util.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

public class TagLink
    extends AbstractStatisticalTokenDistance {
  private AbstractStringDistance tokenDistance;
  private static final AbstractStringDistance DEFAULT_TOKEN_METRIC = new TagLinkToken();

  /**
   * TagLink default constructor. IDF weights are all equally weighted.
   * Transposition constant value is 0.3
   */
  public TagLink() {
    this(DEFAULT_TOKEN_METRIC);
  }

  /**
   * TagLink constructor requires a character based string metric.
   *
   * @param characterBasedStringMetric CharacterBasedStringMetric
   */
  public TagLink(AbstractStringDistance tokenDistance) {
    super();
    this.tokenDistance = tokenDistance;
  }

  /**
   * TagLink constructor requires a tokenizer and a tokenDistance metric
   *
   * @param trainDataObjectArray TrainDataObject[]
   */
  public TagLink(Tokenizer tokenizer, AbstractStringDistance tokenDistance) {
    super(tokenizer);
    this.tokenDistance = tokenDistance;
  }

  /**
   * TagLink constructor requires dataset string array in order to compute the IDF
   * weights. Default character based string metric is TagLinkToken.
   *
   * @param dataSetArray String[]
   */
  public TagLink(String[] dataSetArray) {
    this(dataSetArray, DEFAULT_TOKEN_METRIC);
  }

  /**
   * TagLink constructor requires dataset string array in order to compute the IDF
   * weights and a tokenDistance metric.
   *
   * @param dataSetArray String[]
   */
  public TagLink(String[] dataSetArray,
                       AbstractStringDistance tokenDistance) {
    this.tokenDistance = tokenDistance;
    Vector StringWrapperVector = new Vector();
    for (int i = 0; i < dataSetArray.length; i++) {
      StringWrapperVector.add(
          this.prepare(dataSetArray[i]));
    }
    Iterator it = StringWrapperVector.iterator();
    //accumulateStatistics(it);
    train((StringWrapperIterator)it);
  }

  /**
   * getMinStringSize count the number of characters in String array tTokens and
   * String array uTokens and return the minimun size.
   *
   * @param tTokens String[]
   * @param uTokens String[]
   * @return double
   */
  private double getMinStringSize(String[] tTokens, String[] uTokens) {
    double tSize = 0, uSize = 0;
    for (int i = 0; i < tTokens.length; i++) {
      tSize += tTokens[i].length();
    }
    for (int i = 0; i < uTokens.length; i++) {
      uSize += uTokens[i].length();
    }
    return Math.min(tSize, uSize);
  }

  /**
   * getStringMetric computes the similarity between a pair of strings T and U.
   *
   * @param T String
   * @param U String
   * @return double
   */
  public double score(StringWrapper s, StringWrapper t) {
	checkTrainingHasHappened(s,t);
	UnitVector sBag = asUnitVector(s);
	UnitVector tBag = asUnitVector(t);
    if (s.unwrap().equals(t.unwrap())) {
      return 1.0;
    }
    else {
      String[] sTokens = getTokenArray(sBag),
          tTokens = getTokenArray(tBag);
      double[] sIdfArray = getIDFArray(sBag),
          tIdfArray = getIDFArray(tBag);
      return algorithm1(sTokens, tTokens, sIdfArray, tIdfArray);
    }
  }


	protected UnitVector asUnitVector(StringWrapper w) {
		if (w instanceof UnitVector) return (UnitVector)w;
		else if (w instanceof BagOfTokens) return new UnitVector((BagOfTokens)w);
		else return new UnitVector(w.unwrap(),tokenizer.tokenize(w.unwrap()));
	}

	/** Preprocess a string by finding tokens and giving them TFIDF weights */
	public StringWrapper prepare(String s) {
		return new UnitVector(s, tokenizer.tokenize(s));
	}

	/** Marker class extending BagOfTokens */
	protected class UnitVector extends BagOfTokens
	{
		public UnitVector(String s,Token[] tokens) {
			super(s,tokens);
			termFreq2TFIDF();
		}
		public UnitVector(BagOfTokens bag) {
			this(bag.unwrap(), bag.getTokens());
			termFreq2TFIDF();
		}
		/** convert term frequency weights to unit-length TFIDF weights */
		private void termFreq2TFIDF() {
			double normalizer = 0.0;
			for (Iterator i=tokenIterator(); i.hasNext(); ) {
				Token tok = (Token)i.next();
				if (collectionSize>0) {
					Integer dfInteger = (Integer)documentFrequency.get(tok);
					// set previously unknown words to df==1, which gives them a high value
					double df = dfInteger==null ? 1.0 : dfInteger.intValue();
					double w = Math.log( getWeight(tok) + 1) * Math.log( collectionSize/df );
					setWeight( tok, w );
					normalizer += w*w;
				} else {
					setWeight( tok, 1.0 );
					normalizer += 1.0;
				}
			}
			normalizer = Math.sqrt(normalizer);
			for (Iterator i=tokenIterator(); i.hasNext(); ) {
				Token tok = (Token)i.next();
				setWeight( tok, getWeight(tok)/normalizer );
			}
		}
	}


  /**
   * getIDFArray normalize a vector of IDF weights.
   *
   * @param bag BagOfTokens
   * @return double[]
   */
  private double[] getIDFArray(BagOfTokens bag) {
    double[] idfArray = new double[bag.size()];
    Iterator it = bag.tokenIterator();
    int i = 0;
    while (it.hasNext()) {
      Token tok = (Token) it.next();
      idfArray[i] = bag.getWeight(tok);
      i++;
    }
    return idfArray;
  }

  /**
   * algorithm1 select the considered most appropiate token pairs and compute
   * the sum of the selected pairs.
   *
   * @param tTokens String[]
   * @param uTokens String[]
   * @param tIdfArray double[]
   * @param uIdfArray double[]
   * @return double
   */
  private double algorithm1(String[] tTokens, String[] uTokens,
                            double[] tIdfArray, double[] uIdfArray) {
    ArrayList candidateList = obtainCandidateList(tTokens, uTokens, tIdfArray,
                                                  uIdfArray);
    sortCandidateList(candidateList);
    double scoreValue = 0.0;
    HashMap tMap = new HashMap(),
        uMap = new HashMap();
    Iterator it = candidateList.iterator();
    while (it.hasNext()) {
      Candidates actualCandidates = (Candidates) it.next();
      Integer tPos = new Integer(actualCandidates.getTPos());
      Integer uPos = new Integer(actualCandidates.getUPos());
      if ( (!tMap.containsKey(tPos)) &&
          (!uMap.containsKey(uPos))) {
        double actualScore = actualCandidates.getScore();
        scoreValue += actualScore;
        tMap.put(tPos, null);
        uMap.put(uPos, null);
      }
    }
    return scoreValue;
  }

  private String[] getTokenArray(BagOfTokens bag) {
    String[] stringArray = new String[bag.size()];
    Iterator it = bag.tokenIterator();
    int i = 0;
    while (it.hasNext()) {
      Token tok = (Token) it.next();
      stringArray[i] = tok.getValue();
      i++;
    }
    return stringArray;
  }

  /**
   * explainStringMetric gives a brief explanation of how the stringMetric was
   * computed.
   *
   * @param S String
   * @param T String
   * @return String
   */
  public String explainScore(StringWrapper s, StringWrapper t) {
    BagOfTokens sBag = (BagOfTokens) s;
    BagOfTokens tBag = (BagOfTokens) t;

    StringBuffer buff = new StringBuffer();
    buff.append("\n\t*****TagLink String Distance*****");
    if (s.unwrap().equals(t.unwrap())) {
      buff.append("\nScore(S,T)=1.0\n");
    }
    else {
      String[] sTokens = getTokenArray(sBag),
          tTokens = getTokenArray(tBag);
      buff.append("\nS={");
      for (int i = 0; i < sTokens.length; i++) {
        buff.append(sTokens[i] + ", ");
      }
      buff.append("}\n");

      buff.append("T={");
      for (int i = 0; i < tTokens.length; i++) {
        buff.append(tTokens[i] + ", ");
      }
      buff.append("}\n");

      double minStringSize = getMinStringSize(sTokens, tTokens);
      buff.append("min(|S|,|T|)=" + minStringSize + "\n");

      buff.append("\nIDF weights:\n");
      buff.append("Ti\tai(Ti)\n");
      double[] tIdfArray = getIDFArray(sBag),
          uIdfArray = getIDFArray(tBag);
      for (int i = 0; i < tIdfArray.length; i++) {
        buff.append(sTokens[i] + "\t" + round(tIdfArray[i]) + "\n");
      }
      buff.append("\nTj\taj(Tj)\n");
      for (int i = 0; i < uIdfArray.length; i++) {
        buff.append(tTokens[i] + "\t" + round(uIdfArray[i]) + "\n");
      }
      buff.append("\nScores:\n");
      buff.append(
          "Si\tTj\tScore_ij(Si,Tj)\tIDFij(Si,Tj)\tMRij(Si,Tj)\tScore_ij\n");
      ArrayList candidateList = new ArrayList();
      for (int i = 0; i < sTokens.length; i++) {
        int lastTr = -1;
        for (int j = 0, flag = 0; j < tTokens.length && flag == 0; j++) {
          int tr = Math.abs(i - j);
          if (lastTr >= 0 && lastTr < tr) {
            flag = 1;
          }
          else {
            String tTok = sTokens[i], uTok = tTokens[j];
            double innerScore = tokenDistance.score(tTok,
                uTok);
            if (innerScore >= 0.0) {
              double MR = 0.0;
              if (innerScore == 1.0) {
                MR = sTokens[i].length();
              }
              else {
                MR = ( (TagLinkToken) tokenDistance).getMatched();
              }
              MR = MR / minStringSize;
              double IDF = tIdfArray[i] * uIdfArray[j],
                  weight = (IDF + MR) / 2.0;
              if (innerScore == 1) {
                lastTr = tr;
              }
              buff.append(tTok + "\t" + uTok + "\t" + round(innerScore) +
                          "\t" + round(IDF) + "\t" + round(MR) +
                          "\t" + round(innerScore * weight) + "\n");
              candidateList.add(new Candidates(i, j, innerScore * weight));
            }
          }
        }
      }
      sortCandidateList(candidateList);

      // iterate the candidate list
      buff.append("\nCommon tokens (Algorithm 1):\n");
      buff.append("Ti\tUj\tSij*Xij\n");
      double score = 0.0;
      HashMap tMap = new HashMap(),
          uMap = new HashMap();
      Iterator it = candidateList.iterator();
      while (it.hasNext()) {
        Candidates actualCandidates = (Candidates) it.next();
        Integer tPos = new Integer(actualCandidates.getTPos());
        Integer uPos = new Integer(actualCandidates.getUPos());
        if ( (!tMap.containsKey(tPos)) &&
            (!uMap.containsKey(uPos))) {
          double tokenScore = actualCandidates.getScore();
          score += tokenScore;
          tMap.put(tPos, null);
          uMap.put(uPos, null);
          buff.append(sTokens[tPos.intValue()] + "\t" + tTokens[uPos.intValue()] +
                      "\t" +
                      round(tokenScore) + "\n");
        }
      }
      buff.append("\nS(T,U)=" + round(score) + "\n");
    }
    return buff.toString();
  }


  /**
   * obtainCandidateList set a candidate list of pair of tokens. Sometimes it
   * will not compute all candidate pairs in oder to reduce the computational
   * cost.
   *
   * @param tTokens String[]
   * @param uTokens String[]
   * @param tIdfArray double[]
   * @param uIdfArray double[]
   * @return ArrayList
   */
  private ArrayList obtainCandidateList(String[] tTokens, String[] uTokens,
                                        double[] tIdfArray, double[] uIdfArray) {
    ArrayList candidateList = new ArrayList();
    double minStringSize = getMinStringSize(tTokens, uTokens);
    for (int t = 0; t < tTokens.length; t++) {
      int lastTr = -1;
      for (int u = 0, flag = 0; u < uTokens.length && flag == 0; u++) {
        int tr = Math.abs(t - u);
        if (lastTr >= 0 && lastTr < tr) {
          flag = 1;
        }
        else {
          String tTok = tTokens[t], uTok = uTokens[u];
          double innerScore = tokenDistance.score(tTok,
              uTok);
          if (innerScore >= 0.0) {
            double matched = 0.0;
            if (innerScore == 1.0) {
              matched = tTokens[t].length();
            }
            else {
              matched = ( (TagLinkToken) tokenDistance).getMatched();
            }
            double weightMatched = matched / minStringSize,
                weightTFIDF = tIdfArray[t] * uIdfArray[u],
                weight = (weightTFIDF + weightMatched) / 2.0;
            if (innerScore == 1) {
              lastTr = tr;
            }
            candidateList.add(new Candidates(t, u, innerScore * weight));
          }
        }
      }
    }
    return candidateList;
  }

  /**
   * sortCandidateList sort a list of candidate pair of tokens.
   *
   * @param tokenArray String[]
   * @return float[]
   */
  private void sortCandidateList(ArrayList list) {
    java.util.Collections.sort(list, new java.util.Comparator() {
      public int compare(Object o1, Object o2) {
        // First sort, by score in index
        double scoreT = ( (Candidates) o1).getScore(),
            scoreU = ( (Candidates) o2).getScore();
        if (scoreU > scoreT) {
          return 1;
        }
        if (scoreU < scoreT) {
          return -1;
        }
        return 0;
      }
    }
    );
  }

  /**
   * toString returns the name and parameters of this string metric
   *
   * @return String
   */
  public String toString() {
    return "[TagLink_[" + tokenDistance.toString() + "]";
  }

  /**
   * round a double number.
   *
   * @param number double
   * @return double
   */
  private double round(double number) {
    int round = (int) (number * 1000.00);
    double rest = (number * 1000.00) - round;
    if (rest >= 0.5) {
      round++;
    }
    return (round / 1000.00);
  }

    static public class Candidates {
	private int tPos, uPos;
	private double score;

	/**
	 * Candidates constructor. Creates an instance of a candidate string pair T and U. It
	 * requires the position of the pair in the string and the score or distance
	 * between them.
	 *
	 * @param tPos int
	 * @param uPos int
	 * @param score float
	 */
	public Candidates(int tPos, int uPos, double score) {
	    this.tPos = tPos;
	    this.uPos = uPos;
	    this.score = score;
	}

  /**
   * getTPos, return the position of string T.
   *
   * @return int
   */
  public int getTPos() {
    return tPos;
  }

  /**
   * getUPos, return the position of string U.
   *
   * @return int
   */
  public int getUPos() {
    return uPos;
  }

  /**
   * getScore, return the score or distance between strings T and U.
   *
   * @return float
   */
  public double getScore() {
    return score;
  }

}

}
