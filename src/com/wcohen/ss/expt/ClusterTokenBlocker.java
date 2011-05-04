package com.wcohen.ss.expt;

import java.util.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;

/**
 * TokenBlocker for clustering.
 */

public class ClusterTokenBlocker extends TokenBlocker
{
	public ClusterTokenBlocker() {
		super();
		setClusterMode(true);
	}
	public ClusterTokenBlocker(Tokenizer tokenizer, double maxFraction) {
		super(tokenizer,maxFraction);
		setClusterMode(true);
	}
	public String toString() { return "[ClusterTokenBlocker]"; }
}
