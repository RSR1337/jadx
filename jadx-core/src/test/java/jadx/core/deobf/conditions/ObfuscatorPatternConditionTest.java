package jadx.core.deobf.conditions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the obfuscator pattern detection condition.
 */
class ObfuscatorPatternConditionTest {

	@Test
	void testProGuardPatterns() {
		// Single letter names
		assertEquals("ProGuard/R8", ObfuscatorPatternCondition.detectObfuscatorType("a"));
		assertEquals("ProGuard/R8", ObfuscatorPatternCondition.detectObfuscatorType("b"));
		assertEquals("ProGuard/R8", ObfuscatorPatternCondition.detectObfuscatorType("Z"));

		// Two letter names
		assertEquals("ProGuard/R8", ObfuscatorPatternCondition.detectObfuscatorType("aa"));
		assertEquals("ProGuard/R8", ObfuscatorPatternCondition.detectObfuscatorType("ab"));
	}

	@Test
	void testAllatoriPatterns() {
		// I/l/1 confusion patterns
		assertEquals("Allatori", ObfuscatorPatternCondition.detectObfuscatorType("lIl"));
		assertEquals("Allatori", ObfuscatorPatternCondition.detectObfuscatorType("IllI"));
		assertEquals("Allatori", ObfuscatorPatternCondition.detectObfuscatorType("O0o0"));
	}

	@Test
	void testConfidenceScoring() {
		// Single letter = high confidence
		assertTrue(ObfuscatorPatternCondition.getObfuscationConfidence("a") >= 40);

		// Normal names = low confidence
		assertTrue(ObfuscatorPatternCondition.getObfuscationConfidence("onClick") < 30);
		assertTrue(ObfuscatorPatternCondition.getObfuscationConfidence("MainActivity") < 30);

		// Pattern matches = higher confidence
		assertTrue(ObfuscatorPatternCondition.getObfuscationConfidence("lIl1") >= 50);
	}

	@Test
	void testNormalNamesNotDetected() {
		// Common programming names should not be detected as obfuscator output
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType("onClick"));
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType("MainActivity"));
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType("getUserById"));
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType("handleMessage"));
	}

	@Test
	void testHexPatterns() {
		// Long hex strings
		assertEquals("Hash-based obfuscator",
				ObfuscatorPatternCondition.detectObfuscatorType("a1b2c3d4e5f6"));
	}

	@Test
	void testEdgeCases() {
		// Null and empty
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType(null));
		assertNull(ObfuscatorPatternCondition.detectObfuscatorType(""));

		assertEquals(0, ObfuscatorPatternCondition.getObfuscationConfidence(null));
		assertEquals(0, ObfuscatorPatternCondition.getObfuscationConfidence(""));
	}
}
