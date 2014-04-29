package com.wcohen.ss;

import org.junit.Before;
import org.junit.Test;

public class JaroTest {

	private Jaro jaro;
	
	@Before
	public void setUp() {
		jaro = new Jaro();
	}
	
	@Test
	public void shouldExplainScore() {
		jaro.explainScore("aaa", "aba");
	}
	
}
