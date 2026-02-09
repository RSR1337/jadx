package jadx.core.deobf.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.ArgType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the parameter naming condition.
 */
class ParameterNamingConditionTest {

	@Test
	void testObfuscatedParameterNameDetection() {
		// Single letter
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("a"));
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("p"));

		// Letter + number patterns
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("p0"));
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("p1"));
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("var0"));
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("arg0"));

		// Register-based names
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("r0"));
		assertTrue(ParameterNamingCondition.isObfuscatedParameterName("r15"));
	}

	@Test
	void testNonObfuscatedParameterNames() {
		// Reserved names
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName("this"));
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName("super"));

		// Normal parameter names
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName("context"));
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName("listener"));
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName("callback"));
	}

	@Test
	void testSuggestParameterNameForPrimitives() {
		assertEquals("flag", ParameterNamingCondition.suggestParameterName(ArgType.BOOLEAN, 0));
		assertEquals("flag1", ParameterNamingCondition.suggestParameterName(ArgType.BOOLEAN, 1));

		assertEquals("index", ParameterNamingCondition.suggestParameterName(ArgType.INT, 0));
		assertEquals("count", ParameterNamingCondition.suggestParameterName(ArgType.INT, 1));
	}

	@Test
	void testSuggestParameterNameForStrings() {
		ArgType stringType = ArgType.object("java.lang.String");
		String suggestion = ParameterNamingCondition.suggestParameterName(stringType, 0);
		assertEquals("str", suggestion);
	}

	@Test
	void testSuggestParameterNameForAndroidTypes() {
		ArgType contextType = ArgType.object("android.content.Context");
		assertEquals("context", ParameterNamingCondition.suggestParameterName(contextType, 0));

		ArgType intentType = ArgType.object("android.content.Intent");
		assertEquals("intent", ParameterNamingCondition.suggestParameterName(intentType, 0));

		ArgType viewType = ArgType.object("android.view.View");
		assertEquals("view", ParameterNamingCondition.suggestParameterName(viewType, 0));
	}

	@Test
	void testSuggestParameterNameForCollections() {
		ArgType listType = ArgType.object("java.util.List");
		assertEquals("list", ParameterNamingCondition.suggestParameterName(listType, 0));

		ArgType mapType = ArgType.object("java.util.HashMap");
		assertEquals("map", ParameterNamingCondition.suggestParameterName(mapType, 0));
	}

	@Test
	void testSuggestParameterNameForArrays() {
		ArgType intArrayType = ArgType.array(ArgType.INT);
		String suggestion = ParameterNamingCondition.suggestParameterName(intArrayType, 0);
		assertTrue(suggestion.contains("Array") || suggestion.contains("arr"));
	}

	@Test
	void testNullAndEdgeCases() {
		assertEquals("param0", ParameterNamingCondition.suggestParameterName(null, 0));

		assertFalse(ParameterNamingCondition.isObfuscatedParameterName(null));
		assertFalse(ParameterNamingCondition.isObfuscatedParameterName(""));
	}
}
