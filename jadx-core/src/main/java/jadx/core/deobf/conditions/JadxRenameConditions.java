package jadx.core.deobf.conditions;

import java.util.ArrayList;
import java.util.List;

import jadx.api.deobf.IDeobfCondition;
import jadx.api.deobf.IRenameCondition;
import jadx.api.deobf.impl.CombineDeobfConditions;

public class JadxRenameConditions {

	/**
	 * This method provides a mutable list of default deobfuscation conditions used
	 * by jadx.
	 * To build {@link IRenameCondition} use
	 * {@link CombineDeobfConditions#combine(List)} method.
	 */
	public static List<IDeobfCondition> buildDefaultDeobfConditions() {
		List<IDeobfCondition> list = new ArrayList<>();
		list.add(new BaseDeobfCondition());
		list.add(new DeobfWhitelist());
		list.add(new ExcludePackageWithTLDNames());
		list.add(new ExcludeAndroidRClass());
		list.add(new AvoidClsAndPkgNamesCollision());
		list.add(new DeobfLengthCondition());
		return list;
	}

	/**
	 * Build enhanced deobfuscation conditions with additional pattern detection.
	 * Includes conditions for:
	 * - Common obfuscator pattern detection (ProGuard, R8, DexGuard, Allatori,
	 * etc.)
	 * - Entropy-based obfuscation detection
	 * - Common programming words preservation
	 */
	public static List<IDeobfCondition> buildEnhancedDeobfConditions() {
		List<IDeobfCondition> list = new ArrayList<>();
		// Base conditions first - they handle already renamed and flagged items
		list.add(new BaseDeobfCondition());
		list.add(new DeobfWhitelist());
		list.add(new ExcludePackageWithTLDNames());
		list.add(new ExcludeAndroidRClass());
		list.add(new AvoidClsAndPkgNamesCollision());

		// Preserve common programming terms from being renamed
		list.add(new CommonWordsCondition());

		// Detect and rename common obfuscator patterns
		list.add(new ObfuscatorPatternCondition());

		// Use entropy analysis to detect random-looking names
		list.add(new EntropyBasedCondition());

		// Length-based condition as fallback
		list.add(new DeobfLengthCondition());

		return list;
	}

	/**
	 * Build aggressive deobfuscation conditions that rename more aggressively.
	 * Use this when you want maximum deobfuscation regardless of potential false
	 * positives.
	 */
	public static List<IDeobfCondition> buildAggressiveDeobfConditions() {
		List<IDeobfCondition> list = new ArrayList<>();
		// Only essential exclusions
		list.add(new BaseDeobfCondition());
		list.add(new ExcludeAndroidRClass());
		list.add(new AvoidClsAndPkgNamesCollision());

		// All detection conditions
		list.add(new ObfuscatorPatternCondition());
		list.add(new EntropyBasedCondition());
		list.add(new DeobfLengthCondition());

		return list;
	}

	/**
	 * Build conservative deobfuscation conditions that minimize false positives.
	 * Use this when you want to preserve as many original names as possible.
	 */
	public static List<IDeobfCondition> buildConservativeDeobfConditions() {
		List<IDeobfCondition> list = new ArrayList<>();
		// All preservation conditions
		list.add(new BaseDeobfCondition());
		list.add(new DeobfWhitelist());
		list.add(new ExcludePackageWithTLDNames());
		list.add(new ExcludeAndroidRClass());
		list.add(new AvoidClsAndPkgNamesCollision());
		list.add(new CommonWordsCondition());

		// Only pattern-based detection (most reliable)
		list.add(new ObfuscatorPatternCondition());

		return list;
	}

	public static IRenameCondition buildDefault() {
		return CombineDeobfConditions.combine(buildDefaultDeobfConditions());
	}

	/**
	 * Build enhanced rename condition with improved detection capabilities.
	 */
	public static IRenameCondition buildEnhanced() {
		return CombineDeobfConditions.combine(buildEnhancedDeobfConditions());
	}

	/**
	 * Build aggressive rename condition for maximum deobfuscation.
	 */
	public static IRenameCondition buildAggressive() {
		return CombineDeobfConditions.combine(buildAggressiveDeobfConditions());
	}

	/**
	 * Build conservative rename condition for minimum false positives.
	 */
	public static IRenameCondition buildConservative() {
		return CombineDeobfConditions.combine(buildConservativeDeobfConditions());
	}
}
