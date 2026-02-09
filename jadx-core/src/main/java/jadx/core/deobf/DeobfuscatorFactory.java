package jadx.core.deobf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.args.DeobfuscationMode;
import jadx.api.deobf.IAliasProvider;
import jadx.api.deobf.IRenameCondition;
import jadx.core.deobf.conditions.JadxRenameConditions;
import jadx.core.dex.nodes.RootNode;

/**
 * Factory for creating deobfuscation components based on the selected mode.
 * This class helps configure the deobfuscator with appropriate conditions
 * and alias providers based on the user's preferences.
 */
public class DeobfuscatorFactory {

	private static final Logger LOG = LoggerFactory.getLogger(DeobfuscatorFactory.class);

	/**
	 * Create the appropriate rename condition based on the deobfuscation mode.
	 */
	public static IRenameCondition createRenameCondition(DeobfuscationMode mode) {
		switch (mode) {
			case DISABLED:
				return null;
			case CONSERVATIVE:
				return JadxRenameConditions.buildConservative();
			case ENHANCED:
				return JadxRenameConditions.buildEnhanced();
			case AGGRESSIVE:
				return JadxRenameConditions.buildAggressive();
			case AUTO:
				// For AUTO mode, we'll start with enhanced and may adjust later
				return JadxRenameConditions.buildEnhanced();
			case DEFAULT:
			default:
				return JadxRenameConditions.buildDefault();
		}
	}

	/**
	 * Create the appropriate alias provider based on the deobfuscation mode.
	 */
	public static IAliasProvider createAliasProvider(DeobfuscationMode mode) {
		switch (mode) {
			case ENHANCED:
			case AGGRESSIVE:
			case AUTO:
				EnhancedAliasProvider enhanced = new EnhancedAliasProvider();
				enhanced.setUseSemanticNaming(true);
				enhanced.setPreserveOriginalHint(mode != DeobfuscationMode.AGGRESSIVE);
				return enhanced;
			case CONSERVATIVE:
			case DEFAULT:
			default:
				return new DeobfAliasProvider();
		}
	}

	/**
	 * Create a rename condition based on automatic analysis of the codebase.
	 * This should be called after initial load when the RootNode is available.
	 */
	public static IRenameCondition createAutoCondition(RootNode root) {
		DeobfuscationAnalyzer analyzer = new DeobfuscationAnalyzer(root);
		DeobfuscationAnalyzer.DeobfStats stats = analyzer.analyze();

		LOG.info("Auto-detecting deobfuscation mode...");
		if (analyzer.getDetectedObfuscator() != null) {
			LOG.info("Detected obfuscator: {}", analyzer.getDetectedObfuscator());
		}
		LOG.info("Overall obfuscation rate: {:.1f}%", stats.overallObfuscationRate);

		DeobfuscationMode selectedMode;
		if (stats.isHeavilyObfuscated()) {
			LOG.info("Codebase is heavily obfuscated, using AGGRESSIVE mode");
			selectedMode = DeobfuscationMode.AGGRESSIVE;
		} else if (stats.isLightlyObfuscated()) {
			LOG.info("Codebase is lightly obfuscated, using ENHANCED mode");
			selectedMode = DeobfuscationMode.ENHANCED;
		} else {
			LOG.info("Codebase has minimal obfuscation, using CONSERVATIVE mode");
			selectedMode = DeobfuscationMode.CONSERVATIVE;
		}

		return createRenameCondition(selectedMode);
	}

	/**
	 * Create an alias provider based on automatic analysis of the codebase.
	 */
	public static IAliasProvider createAutoAliasProvider(RootNode root) {
		DeobfuscationAnalyzer analyzer = new DeobfuscationAnalyzer(root);
		DeobfuscationAnalyzer.DeobfStats stats = analyzer.analyze();

		if (stats.isHeavilyObfuscated()) {
			return createAliasProvider(DeobfuscationMode.AGGRESSIVE);
		} else if (stats.isLightlyObfuscated()) {
			return createAliasProvider(DeobfuscationMode.ENHANCED);
		} else {
			return createAliasProvider(DeobfuscationMode.CONSERVATIVE);
		}
	}

	/**
	 * Get a description of what the specified mode will do.
	 */
	public static String getModeDescription(DeobfuscationMode mode) {
		StringBuilder sb = new StringBuilder();
		sb.append("Mode: ").append(mode.name()).append("\n");
		sb.append("Description: ").append(mode.getDescription()).append("\n");
		sb.append("\nConditions used:\n");

		switch (mode) {
			case DISABLED:
				sb.append("  - No conditions (disabled)\n");
				break;
			case CONSERVATIVE:
				sb.append("  - Base condition (skip flagged/renamed)\n");
				sb.append("  - Whitelist preservation\n");
				sb.append("  - TLD exclusion\n");
				sb.append("  - Android R class exclusion\n");
				sb.append("  - Name collision avoidance\n");
				sb.append("  - Common words preservation\n");
				sb.append("  - Obfuscator pattern detection\n");
				break;
			case DEFAULT:
				sb.append("  - Base condition (skip flagged/renamed)\n");
				sb.append("  - Whitelist preservation\n");
				sb.append("  - TLD exclusion\n");
				sb.append("  - Android R class exclusion\n");
				sb.append("  - Name collision avoidance\n");
				sb.append("  - Length-based detection\n");
				break;
			case ENHANCED:
				sb.append("  - Base condition (skip flagged/renamed)\n");
				sb.append("  - Whitelist preservation\n");
				sb.append("  - TLD exclusion\n");
				sb.append("  - Android R class exclusion\n");
				sb.append("  - Name collision avoidance\n");
				sb.append("  - Common words preservation\n");
				sb.append("  - Obfuscator pattern detection\n");
				sb.append("  - Entropy-based detection\n");
				sb.append("  - Length-based detection\n");
				break;
			case AGGRESSIVE:
				sb.append("  - Base condition only\n");
				sb.append("  - Android R class exclusion\n");
				sb.append("  - Name collision avoidance\n");
				sb.append("  - Obfuscator pattern detection\n");
				sb.append("  - Entropy-based detection\n");
				sb.append("  - Length-based detection\n");
				break;
			case AUTO:
				sb.append("  - Automatically selected based on codebase analysis\n");
				break;
		}

		sb.append("\nAlias provider: ");
		switch (mode) {
			case ENHANCED:
			case AGGRESSIVE:
			case AUTO:
				sb.append("Enhanced (semantic naming, type-aware)\n");
				break;
			default:
				sb.append("Default (index-based naming)\n");
				break;
		}

		return sb.toString();
	}
}
