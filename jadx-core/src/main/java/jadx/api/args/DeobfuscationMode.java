package jadx.api.args;

/**
 * Deobfuscation mode that controls how aggressively the deobfuscator renames
 * elements.
 */
public enum DeobfuscationMode {
	/**
	 * Disabled - no automatic deobfuscation is performed.
	 */
	DISABLED,

	/**
	 * Conservative mode - minimal renaming to avoid false positives.
	 * Only renames obvious obfuscator patterns.
	 * Best for codebases with minimal obfuscation.
	 */
	CONSERVATIVE,

	/**
	 * Default mode - balanced approach between detection and preservation.
	 * Uses standard length-based and pattern-based detection.
	 */
	DEFAULT,

	/**
	 * Enhanced mode - uses advanced pattern detection and entropy analysis.
	 * Includes common programming word preservation.
	 * Good for typical obfuscated Android apps.
	 */
	ENHANCED,

	/**
	 * Aggressive mode - maximum deobfuscation.
	 * May produce some false positives.
	 * Best for heavily obfuscated codebases.
	 */
	AGGRESSIVE,

	/**
	 * Auto mode - automatically detects obfuscation level and chooses appropriate
	 * mode.
	 * Analyzes the codebase first to determine the best deobfuscation strategy.
	 */
	AUTO;

	/**
	 * Get the default deobfuscation mode.
	 */
	public static DeobfuscationMode getDefault() {
		return DEFAULT;
	}

	/**
	 * Check if deobfuscation is enabled in this mode.
	 */
	public boolean isEnabled() {
		return this != DISABLED;
	}

	/**
	 * Get a human-readable description of this mode.
	 */
	public String getDescription() {
		switch (this) {
			case DISABLED:
				return "Deobfuscation disabled";
			case CONSERVATIVE:
				return "Conservative - minimal renaming, fewer false positives";
			case DEFAULT:
				return "Default - balanced detection and preservation";
			case ENHANCED:
				return "Enhanced - advanced pattern detection with word preservation";
			case AGGRESSIVE:
				return "Aggressive - maximum renaming, may have false positives";
			case AUTO:
				return "Auto - automatically selects mode based on obfuscation analysis";
			default:
				return name();
		}
	}
}
