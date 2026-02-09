package jadx.core.deobf.conditions;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

/**
 * Uses Shannon entropy analysis to detect obfuscated names.
 * High entropy names (random-looking character sequences) are likely
 * obfuscated.
 * Low entropy names (common patterns like "onClick", "getData") are likely
 * original.
 */
public class EntropyBasedCondition extends AbstractDeobfCondition {

	private double entropyThreshold = 3.5; // Default threshold for obfuscation detection
	private int minNameLengthForAnalysis = 3;

	@Override
	public void init(RootNode root) {
		// Could be made configurable via JadxArgs in the future
	}

	@Override
	public Action check(PackageNode pkg) {
		String name = pkg.getName();
		if (name == null || name.length() < minNameLengthForAnalysis) {
			return Action.NO_ACTION;
		}
		if (isHighEntropy(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		String name = cls.getName();
		if (name == null || name.length() < minNameLengthForAnalysis) {
			return Action.NO_ACTION;
		}
		if (isHighEntropy(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		String name = fld.getName();
		if (name == null || name.length() < minNameLengthForAnalysis) {
			return Action.NO_ACTION;
		}
		if (isHighEntropy(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(MethodNode mth) {
		String name = mth.getName();
		if (name == null || name.length() < minNameLengthForAnalysis) {
			return Action.NO_ACTION;
		}
		if (isHighEntropy(name)) {
			return Action.FORCE_RENAME;
		}
		return Action.NO_ACTION;
	}

	/**
	 * Calculate if a name has high entropy indicating possible obfuscation.
	 * Shannon entropy measures the randomness of character distribution.
	 */
	private boolean isHighEntropy(String name) {
		double entropy = calculateShannonEntropy(name);
		return entropy > entropyThreshold;
	}

	/**
	 * Calculate Shannon entropy for the given string.
	 * H = -Σ p(x) * log2(p(x)) for each unique character x
	 */
	public static double calculateShannonEntropy(String str) {
		if (str == null || str.isEmpty()) {
			return 0.0;
		}

		// Count character frequencies
		int[] charCounts = new int[256];
		int totalChars = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < 256) {
				charCounts[c]++;
				totalChars++;
			}
		}

		if (totalChars == 0) {
			return 0.0;
		}

		// Calculate entropy
		double entropy = 0.0;
		for (int count : charCounts) {
			if (count > 0) {
				double probability = (double) count / totalChars;
				entropy -= probability * (Math.log(probability) / Math.log(2));
			}
		}

		return entropy;
	}

	/**
	 * Check if a name follows common obfuscator patterns (single chars, repeated
	 * patterns, etc.)
	 */
	public static boolean hasObfuscatorPattern(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		// Single character names (a, b, c)
		if (name.length() == 1) {
			return true;
		}

		// Double character names (aa, ab, aA)
		if (name.length() == 2 && Character.isLetter(name.charAt(0)) && Character.isLetter(name.charAt(1))) {
			return true;
		}

		// Names that are all the same character repeated (aaaa, ZZZZ)
		if (isRepeatedChar(name)) {
			return true;
		}

		// Names with only letters from limited set (typical obfuscator output like
		// "oO0", "lI1")
		if (isConfusingCharsOnly(name)) {
			return true;
		}

		// Names that look like base64 or hex patterns
		if (looksLikeEncodedString(name)) {
			return true;
		}

		return false;
	}

	private static boolean isRepeatedChar(String name) {
		if (name.length() < 3) {
			return false;
		}
		char first = name.charAt(0);
		for (int i = 1; i < name.length(); i++) {
			if (name.charAt(i) != first) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if name only contains confusing characters commonly used by
	 * obfuscators.
	 * Examples: l/I/1, 0/O/o, combinations like "lIl1", "O0o"
	 */
	private static boolean isConfusingCharsOnly(String name) {
		if (name.length() < 3) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c != 'l' && c != 'I' && c != '1'
					&& c != '0' && c != 'O' && c != 'o'
					&& c != '_') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the name looks like an encoded string (base64, hex).
	 */
	private static boolean looksLikeEncodedString(String name) {
		if (name.length() < 4) {
			return false;
		}

		// Check for hex pattern (all characters are 0-9, a-f, A-F)
		boolean allHex = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
				allHex = false;
				break;
			}
		}
		if (allHex && name.length() >= 6) {
			return true;
		}

		// Check for base64-like pattern (alphanumeric + +/)
		int specialCount = 0;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == '+' || c == '/' || c == '=') {
				specialCount++;
			}
		}
		if (specialCount > 1 && name.length() >= 8) {
			return true;
		}

		return false;
	}

	public void setEntropyThreshold(double threshold) {
		this.entropyThreshold = threshold;
	}

	public double getEntropyThreshold() {
		return entropyThreshold;
	}

	public void setMinNameLengthForAnalysis(int minLength) {
		this.minNameLengthForAnalysis = minLength;
	}

	public int getMinNameLengthForAnalysis() {
		return minNameLengthForAnalysis;
	}
}
