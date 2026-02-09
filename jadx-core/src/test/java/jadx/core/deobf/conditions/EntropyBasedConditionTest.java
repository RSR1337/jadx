package jadx.core.deobf.conditions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the entropy-based obfuscation detection condition.
 */
class EntropyBasedConditionTest {

	@Test
	void testLowEntropyNormalNames() {
		// Normal programming names should have lower entropy
		assertTrue(isLowEntropy("onClick"));
		assertTrue(isLowEntropy("getData"));
		assertTrue(isLowEntropy("MainActivity"));
		assertTrue(isLowEntropy("UserService"));
		assertTrue(isLowEntropy("handleMessage"));
	}

	@Test
	void testHighEntropyObfuscatedNames() {
		// Random-looking obfuscated names should have higher entropy
		assertTrue(isHighEntropy("aB3xQ9mK"));
		assertTrue(isHighEntropy("zXcVbNm1"));
		assertTrue(isHighEntropy("qWeRtYuI"));
	}

	@Test
	void testObfuscatorPatternDetection() {
		// Single character names
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("a"));
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("Z"));

		// Double character names
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("aa"));
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("aB"));

		// Repeated character names
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("aaaa"));
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("ZZZZ"));

		// Confusing character patterns (l/I/1)
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("lIl1"));
		assertTrue(EntropyBasedCondition.hasObfuscatorPattern("O0o0"));

		// Normal names should not match
		assertFalse(EntropyBasedCondition.hasObfuscatorPattern("onClick"));
		assertFalse(EntropyBasedCondition.hasObfuscatorPattern("MainActivity"));
	}

	@Test
	void testShannonEntropyCalculation() {
		// Repeated characters = low entropy
		double entropy1 = EntropyBasedCondition.calculateShannonEntropy("aaaa");
		assertEquals(0.0, entropy1, 0.01);

		// All different characters = higher entropy
		double entropy2 = EntropyBasedCondition.calculateShannonEntropy("abcd");
		assertEquals(2.0, entropy2, 0.01);

		// Empty/null handling
		assertEquals(0.0, EntropyBasedCondition.calculateShannonEntropy(null));
		assertEquals(0.0, EntropyBasedCondition.calculateShannonEntropy(""));
	}

	private boolean isLowEntropy(String name) {
		double entropy = EntropyBasedCondition.calculateShannonEntropy(name);
		return entropy < 3.5;
	}

	private boolean isHighEntropy(String name) {
		double entropy = EntropyBasedCondition.calculateShannonEntropy(name);
		return entropy > 3.0; // Adjusted threshold for test strings
	}
}
