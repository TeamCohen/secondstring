package com.wcohen.ss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.wcohen.ss.api.StringDistance;
import com.wcohen.ss.api.StringWrapper;
import com.wcohen.ss.api.Token;
import com.wcohen.ss.api.Tokenizer;

/**
 * TFIDF-based distance metric, extended to use "soft" token-matching. Specifically, tokens are considered a partial
 * match if they get a good score using an inner string comparator.
 * 
 * <p>
 * On the WHIRL datasets, thresholding JaroWinkler at 0.9 or 0.95 seems to be about right.
 */

public class SoftTFIDF extends TFIDF {
    // distance to use to compare tokens
    private StringDistance tokenDistance;
    // threshold beyond which tokens are considered a match
    private double tokenMatchThreshold;
    // default token distance
    private static final StringDistance DEFAULT_TOKEN_DISTANCE = new JaroWinkler();

    public SoftTFIDF(Tokenizer tokenizer, StringDistance tokenDistance, double tokenMatchThreshold) {
        super(tokenizer);
        this.tokenDistance = tokenDistance;
        this.tokenMatchThreshold = tokenMatchThreshold;
    }

    public SoftTFIDF(StringDistance tokenDistance, double tokenMatchThreshold) {
        super();
        this.tokenDistance = tokenDistance;
        this.tokenMatchThreshold = tokenMatchThreshold;
    }

    public SoftTFIDF(StringDistance tokenDistance) {
        this(tokenDistance, 0.9);
    }

    public SoftTFIDF() {
	this(new JaroWinkler(), 0.9);
    }

    public void setTokenMatchThreshold(double d) {
        tokenMatchThreshold = d;
    }

    public void setTokenMatchThreshold(Double d) {
        tokenMatchThreshold = d.doubleValue();
    }

    public double getTokenMatchThreshold() {
        return tokenMatchThreshold;
    }

    private class Similarity implements Comparable<Similarity> {
        int r1;
        int r2;
        double sim;

        public Similarity(int r1, int r2, double sim) {
            this.r1 = r1;
            this.r2 = r2;
            this.sim = sim;
        }

        public int compareTo(Similarity o) {
            if (sim > o.sim)
                return 1;
            else if (sim < o.sim)
                return -1;
            return 0;
        }
    }

    public double score(StringWrapper s, StringWrapper t) {
        checkTrainingHasHappened(s, t);
        UnitVector sBag = asUnitVector(s);
        UnitVector tBag = asUnitVector(t);
        List<Similarity> similarities = new ArrayList<Similarity>(sBag.size());
        double sim = 0.0;
        int i = 0;
        for (Iterator<Token> ti = sBag.tokenIterator(); ti.hasNext(); i++) {
            Token tok = ti.next();
            int j = 0;
            for (Iterator<Token> tj = tBag.tokenIterator(); tj.hasNext(); j++) {
                Token tokJ = tj.next();
                double distItoJ = tokenDistance.score(tok.getValue(), tokJ.getValue());
                if (distItoJ >= tokenMatchThreshold) {
                    similarities.add(new Similarity(i, j, distItoJ * sBag.getWeight(tok) * tBag.getWeight(tokJ)));
                }
            }

        }

        /*
         * This could be O(sBag.size() * tBag.size() * log (sBag.size() * tBag.size())) in the worst case but usually
         * the threshold will make it much better and likely less than O(sBag.size() * tBag.size()) which is the current
         * complexity.
         */
        Collections.sort(similarities, Collections.reverseOrder());

        boolean[] sUsed = new boolean[sBag.size()];
        boolean[] tUsed = new boolean[tBag.size()];

        // enforce that each word is only used for one similarity, to make sure normalization works
        for (int k = 0; k < similarities.size(); k++) {
            Similarity similarity = similarities.get(k);
            if (sUsed[similarity.r1] || tUsed[similarity.r2])
                continue;
            sim += similarity.sim;
            sUsed[similarity.r1] = true;
            tUsed[similarity.r2] = true;
        }

        return sim;
    }

    /**
     * Explain how the distance was computed. In the output, the tokens in S and T are listed, and the common tokens are
     * marked with an asterisk.
     */
    public String explainScore(StringWrapper s, StringWrapper t) {
        BagOfTokens sBag = (BagOfTokens) s;
        BagOfTokens tBag = (BagOfTokens) t;
        StringBuilder buf = new StringBuilder("");
        PrintfFormat fmt = new PrintfFormat("%.3f");
        buf.append("Common tokens: ");
        for (Iterator<Token> i = sBag.tokenIterator(); i.hasNext();) {
            Token tok = i.next();
            if (tBag.contains(tok)) {
                buf.append(" " + tok.getValue() + ": ");
                buf.append(fmt.sprintf(sBag.getWeight(tok)));
                buf.append("*");
                buf.append(fmt.sprintf(tBag.getWeight(tok)));
            } else {
                // find best matching token
                double matchScore = tokenMatchThreshold;
                Token matchTok = null;
                for (Iterator<Token> j = tBag.tokenIterator(); j.hasNext();) {
                    Token tokJ = j.next();
                    double distItoJ = tokenDistance.score(tok.getValue(), tokJ.getValue());
                    if (distItoJ >= matchScore) {
                        matchTok = tokJ;
                        matchScore = distItoJ;
                    }
                }
                if (matchTok != null) {
                    buf.append(" '" + tok.getValue() + "'~='" + matchTok.getValue() + "': ");
                    buf.append(fmt.sprintf(sBag.getWeight(tok)));
                    buf.append("*");
                    buf.append(fmt.sprintf(tBag.getWeight(matchTok)));
                    buf.append("*");
                    buf.append(fmt.sprintf(matchScore));
                }
            }
        }
        buf.append("\nscore = " + score(s, t));
        return buf.toString();
    }

    public String toString() {
        return "[SoftTFIDF thresh=" + tokenMatchThreshold + ";" + tokenDistance + "]";
    }
}
