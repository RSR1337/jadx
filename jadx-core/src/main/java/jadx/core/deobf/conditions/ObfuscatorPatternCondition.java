package jadx.core.deobf.conditions;

import java.util.regex.Pattern;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;

/**
 * Detects common obfuscator output patterns from tools like:
 * - ProGuard/R8 (Android's default obfuscator)
 * - DexGuard (commercial Android obfuscator)
 * - Allatori (Java obfuscator)
 * - Zelix KlassMaster (Java obfuscator)
 * - yGuard (open source Java obfuscator)
 * - Stringer (Java obfuscator)
 * - DashO (Java/Android obfuscator)
 */
public class ObfuscatorPatternCondition extends AbstractDeobfCondition {

	// ProGuard/R8 typical patterns: single letters, aa, ab, a$a, etc.
	private static final Pattern PROGUARD_PATTERN = Pattern.compile(
			"^[a-z]$" // Single lowercase letter
					+ "|^[a-z]{1,2}$" // One or two lowercase letters
					+ "|^[a-z]\\$[a-z]$" // Inner class pattern like a$a
					+ "|^[A-Z]$" // Single uppercase letter
					+ "|^[A-Z]{1,2}$" // One or two uppercase letters
	);

	// DexGuard patterns: often uses unicode characters, combining marks
	private static final Pattern DEXGUARD_PATTERN = Pattern.compile(
			"[\u0080-\u00ff]+" // Extended ASCII
					+ "|[\u0300-\u036f]+" // Combining diacritical marks
					+ "|[\\p{So}\\p{Sk}]+" // Symbol characters
	);

	// Allatori patterns: often uses Il1O0 confusing characters
	private static final Pattern ALLATORI_PATTERN = Pattern.compile(
			"^[Il1O0]+$" // Only confusing chars
					+ "|^[lI]+$" // Only l and I
					+ "|^[oO0]+$" // Only o, O, and 0
	);

	// Zelix patterns: typically uses sequential naming like zzA, zzB, etc.
	private static final Pattern ZELIX_PATTERN = Pattern.compile(
			"^zz[A-Za-z]$" // zz prefix
					+ "|^_[A-Za-z]{1,2}$" // Underscore prefix
	);

	// Generic numeric suffix pattern (ClassName$$1, method$1)
	private static final Pattern NUMERIC_SUFFIX_PATTERN = Pattern.compile(
			".*\\$\\$?\\d+$" // Ends with $1 or $$1
	);

	// Lambda/synthetic class patterns
	private static final Pattern LAMBDA_PATTERN = Pattern.compile(
			".*\\$\\$Lambda\\$.*" // Lambda classes
					+ "|.*\\-\\$\\$.*" // Synthetic bridge
	);

	// Hexadecimal/hash-like patterns
	private static final Pattern HEX_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8,}$" // Long hex string
					+ "|^_0x[0-9a-fA-F]+$" // _0x prefix
	);

	// Unicode escape patterns used by some obfuscators
	private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile(
			".*\\u[0-9a-fA-F]{4}.*");

	// Method/field with underscore prefix (common in obfuscated code)
	private static final Pattern UNDERSCORE_PREFIX_PATTERN = Pattern.compile(
			"^_{2,}[A-Za-z0-9]*$" // Multiple underscore prefix
					+ "|^[A-Za-z0-9]*_{2,}$" // Multiple underscore suffix
	);

	// Random-looking patterns with mixed case and numbers
	private static final Pattern RANDOM_PATTERN = Pattern.compile(
			"^[a-zA-Z][0-9][a-zA-Z][0-9].*$" // Alternating letters/numbers
					+ "|^[A-Z]{3,}[0-9]+$" // Uppercase + numbers
	);

	@Override
	public Action check(PackageNode pkg) {
		String name = pkg.getName();
		if (name == null || name.isEmpty()) {
			return Action.NO_ACTION;
		}
		if (matchesObfuscatorPattern(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		String name = cls.getName();
		if (name == null || name.isEmpty()) {
			return Action.NO_ACTION;
		}

		// Skip lambda and synthetic classes - they have their own handling
		if (LAMBDA_PATTERN.matcher(name).matches()) {
			return Action.NO_ACTION;
		}

		if (matchesObfuscatorPattern(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		String name = fld.getName();
		if (name == null || name.isEmpty()) {
			return Action.NO_ACTION;
		}
		if (matchesObfuscatorPattern(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(MethodNode mth) {
		String name = mth.getName();
		if (name == null || name.isEmpty()) {
			return Action.NO_ACTION;
		}

		// Skip special methods
		if (name.equals("<init>") || name.equals("<clinit>")) {
			return Action.NO_ACTION;
		}

		if (matchesObfuscatorPattern(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	/**
	 * Check if the name matches any known obfuscator pattern.
	 */
	private boolean matchesObfuscatorPattern(String name) {
		return PROGUARD_PATTERN.matcher(name).matches()
				|| ALLATORI_PATTERN.matcher(name).matches()
				|| ZELIX_PATTERN.matcher(name).matches()
				|| HEX_PATTERN.matcher(name).matches()
				|| UNDERSCORE_PREFIX_PATTERN.matcher(name).matches()
				|| RANDOM_PATTERN.matcher(name).matches()
				|| DEXGUARD_PATTERN.matcher(name).find()
				|| UNICODE_ESCAPE_PATTERN.matcher(name).find();
	}

	/**
	 * Detect which obfuscator was likely used based on patterns.
	 * Returns the name of the detected obfuscator or null if unknown.
	 */
	public static String detectObfuscatorType(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}

		if (PROGUARD_PATTERN.matcher(name).matches()) {
			return "ProGuard/R8";
		}
		if (ALLATORI_PATTERN.matcher(name).matches()) {
			return "Allatori";
		}
		if (ZELIX_PATTERN.matcher(name).matches()) {
			return "Zelix KlassMaster";
		}
		if (DEXGUARD_PATTERN.matcher(name).find()) {
			return "DexGuard";
		}
		if (HEX_PATTERN.matcher(name).matches()) {
			return "Hash-based obfuscator";
		}

		return null;
	}

	/**
	 * Get a confidence score (0-100) for how likely a name is obfuscated.
	 */
	public static int getObfuscationConfidence(String name) {
		if (name == null || name.isEmpty()) {
			return 0;
		}

		int score = 0;

		// Length-based scoring
		if (name.length() == 1) {
			score += 40;
		} else if (name.length() == 2) {
			score += 30;
		} else if (name.length() <= 4) {
			score += 15;
		}

		// Pattern-based scoring
		if (PROGUARD_PATTERN.matcher(name).matches()) {
			score += 35;
		}
		if (ALLATORI_PATTERN.matcher(name).matches()) {
			score += 50;
		}
		if (HEX_PATTERN.matcher(name).matches()) {
			score += 40;
		}
		if (UNDERSCORE_PREFIX_PATTERN.matcher(name).matches()) {
			score += 25;
		}
		if (DEXGUARD_PATTERN.matcher(name).find()) {
			score += 45;
		}

		// Entropy-based adjustment
		double entropy = EntropyBasedCondition.calculateShannonEntropy(name);
		if (entropy > 4.0) {
			score += 20;
		} else if (entropy > 3.5) {
			score += 10;
		}

		return Math.min(100, score);
	}
}
