/**
 * <p>Title: </p> TagLinkToken string distance
 *
 * <p>Description: </p> This is a string metric for pairs of tokens.
 * Matched character pairs are defined by Algorithm1.
 * This string distance follows notation as described in Camacho & Salhi 2006.
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

package com.wcohen.ss.tokens;

import java.util.*;
import com.wcohen.ss.*;
import com.wcohen.ss.api.*;

public class TagLinkToken
    extends AbstractStringDistance {
  private double matched, tr, sSize, tSize, totalScore;
  private static final double DEF_TR = 0.3;
  private String sA, sB, tokenT;
  private int largestIndex;

  /**
   * TagLinkToken default constructor. Instance of this class with parameter
   * gamma = 0.3
   */
  public TagLinkToken() {
    this(DEF_TR);
  }

  /**
   * TagLinkToken constrctur. Instance of this class with user specified
   * parameter.
   *
   * @param tr double
   */
  public TagLinkToken(double tr) {
    this.tr = tr;
  }


  /**
   * score return the a strng distance value between 0 and 1 of a pair
   * of tokens. Where 1 is the maximum similarity.
   *
   * @param S StringWrapper
   * @param T StringWrapper
   * @return double
   */
  public double score(StringWrapper s, StringWrapper t) {
	String S = s.unwrap(),
	  T =t.unwrap();
    totalScore = 0.0;
    if (S.equals(T)) {
      matched = S.length();
      return 1.0;
    }
    else {
      sSize = S.length();
      tSize = T.length();
      // let S be the largest token
      if (sSize < tSize) {
        String tmp1 = S;
        S = T;
        T = tmp1;
        double tmp2 = sSize;
        sSize = tSize;
        tSize = tmp2;
        tokenT = T;
      }
      tokenT = S;
      ArrayList candidateList = algorithm1(S, T);
      sortList(candidateList);
      totalScore = getScore(candidateList);
      totalScore = (totalScore / ( (double) sSize) + totalScore / ( (double) tSize)) / 2.0;
      return winkler(totalScore, S, T);
    }
  }

  /**
   * explainScore returns an explanation of how the string distance was
   * computed.
   *
   * @param S StringWrapper
   * @param T StringWrapper
   * @return String
   */
  public String explainScore(StringWrapper s, StringWrapper t) {
	String S = s.unwrap(),
	  T = t.unwrap();
    StringBuffer buff = new StringBuffer();
    buff.append("\n****TagLinkToken****\n");
    buff.append("Si=" + S + ", Tj=" + T + "\n");
    double totalScore = 0.0;
    if (S.equals(T)) {
      matched = S.length();
      buff.append("Sij=1.0");
    }
    else {
      sSize = S.length();
      tSize = T.length();
      // let S be the biggest token
      if (sSize < tSize) {
        String tmp1 = S;
        S = T;
        T = tmp1;
        double tmp2 = sSize;
        sSize = tSize;
        tSize = tmp2;
      }
      ArrayList candidateList = algorithm1(S, T);
      sortList(candidateList);
      buff.append("Common characteres:\n");
      buff.append("Si\tTj\tScore_ij(Si,Tj)\n");
      matched = 0;
      HashMap tMap = new HashMap(),
          uMap = new HashMap();
      java.util.Iterator it = candidateList.iterator();
      while (it.hasNext()) {
        TagLink.Candidates actualCandidates = (TagLink.Candidates) it.next();
        Integer sPos = new Integer(actualCandidates.getTPos()),
            tPos = new Integer(actualCandidates.getUPos());
        if ( (!tMap.containsKey(sPos)) &&
            (!uMap.containsKey(tPos))) {
          double actualScore = actualCandidates.getScore();
          totalScore += actualScore;
          tMap.put(sPos, null);
          uMap.put(tPos, null);
          buff.append(S.charAt(sPos.intValue()) + "\t" + T.charAt(tPos.intValue()) +
                      "\t" + round(actualScore) + "\n");
          matched++;
        }
      }
      totalScore = (totalScore / ( (double) sSize) + totalScore / ( (double) tSize)) / 2.0;
      System.out.println("score " + totalScore);
      buff.append("Score_ij(S,T)=" + round(winkler(totalScore, S, T)));
      buff.append("\nMatched characters=" + matched);
    }
    return buff.toString();
  }

  /**
   * getScore sum the total score of a candidate list of pair of characters.
   *
   * @param candidateList ArrayList
   * @return double
   */
  private double getScore(ArrayList candidateList) {
    matched = 0;
    largestIndex = -1;
    double scoreValue = 0;
    HashMap tMap = new HashMap(),
        uMap = new HashMap();
    java.util.Iterator it = candidateList.iterator();
    while (it.hasNext()) {
      TagLink.Candidates actualCandidates = (TagLink.Candidates) it.next();
      Integer actualTPos = new Integer(actualCandidates.getTPos()),
          actualUPos = new Integer(actualCandidates.getUPos());
      if ( (!tMap.containsKey(actualTPos)) &&
          (!uMap.containsKey(actualUPos))) {
        double actualScore = actualCandidates.getScore();
        scoreValue += actualScore;
        tMap.put(actualTPos, null);
        uMap.put(actualUPos, null);
        if (largestIndex < actualTPos.intValue()) {
          largestIndex = actualTPos.intValue();
        }
        matched++;
      }
    }
    return scoreValue;
  }

  /**
   * algorithm1 select the considered most appropiate character pairs are return
   * a list of candidates.
   *
   * @param S String
   * @param T String
   * @return ArrayList
   */
  private ArrayList algorithm1(String S, String T) {
    ArrayList candidateList = new ArrayList();
    int bound = (int) (1.0 / tr);
    for (int t = 0; t < S.length(); t++) {
      char chT = S.charAt(t);
      double lastTr = -1;
      for (int u = Math.max(0, t - bound), flag = 0;
           u < Math.min(t + bound + 1, T.length()) && flag == 0; u++) {
        double tr2 = ( (double) Math.abs(t - u));
        if ( (lastTr >= 0.0) && (lastTr < tr2)) {
          flag = 1;
        }
        else {
          char chU = T.charAt(u);
          double charScore = 0.0;
          if(chT==chU){
            charScore = 1.0;
          }
          if (charScore > 0.0) {
            if (charScore == 1.0) {
              lastTr = tr2;
            }
            charScore = charScore - (tr * tr2);
            if (charScore == 1.0) {
              flag = 1;
            }
            candidateList.add(new TagLink.Candidates(t, u, charScore));
          }
        }
      }
    }
    return candidateList;
  }

  /**
   * sortList sort a candidate list by its scores.
   *
   * @param candidateList ArrayList
   */
  private void sortList(ArrayList candidateList) {
    java.util.Collections.sort(candidateList, new java.util.Comparator() {
      public int compare(Object o1, Object o2) {
        double scoreT = ( (TagLink.Candidates) o1).getScore();
        double scoreU = ( (TagLink.Candidates) o2).getScore();
        if(scoreU > scoreT){
			return 1;
		}
        if(scoreU > scoreT){
			return -1;
		}
		return 0;
      }
    }
    );
  }

  /**
   * winkler scorer. Compute the Winkler heuristic as in Winkler 1999.
   *
   * @param score double
   * @param S String
   * @param T String
   * @return double
   */
  private double winkler(double totalScore, String S, String T) {
    totalScore = totalScore + (getPrefix(S, T) * 0.1 * (1.0 - totalScore));
    return totalScore;
  }

  private int getPrefix(String S, String T) {
    int bound = Math.min(4, Math.min(S.length(), T.length()));
    int prefix;
    for (prefix = 0; prefix < bound; prefix++) {
      if (S.charAt(prefix) != T.charAt(prefix)) {
        break;
      }
    }
    return prefix;
  }

  /**
   * getMatched return the number of matched character. This value is requiered
   * for the MR-IDF method as proposed in Horacio & Salhi (2006)
   *
   * @return double
   */
  public double getMatched() {
    return matched;
  }

  /**
   * getTr return the contant value Gamma.
   *
   * @return double
   */
  public double getTr() {
    return tr;
  }


  /**
   * setTreshold set a new value to the constant Gamma.
   *
   * @param treshold double
   */
  public void setTreshold(double treshold) {
    tr = treshold;
  }

  /**
   * toString return the name of the string metric.
   *
   * @return String
   */
  public String toString() {
    return "[TagLinkToken_Tr_" + tr + "]";
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

}
