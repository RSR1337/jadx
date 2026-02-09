package jadx.core.deobf;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.deobf.conditions.EntropyBasedCondition;
import jadx.core.deobf.conditions.ObfuscatorPatternCondition;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

/**
 * Utility class for analyzing obfuscation patterns and collecting statistics
 * about the codebase to help with deobfuscation decisions.
 */
public class DeobfuscationAnalyzer {

	private static final Logger LOG = LoggerFactory.getLogger(DeobfuscationAnalyzer.class);

	private final RootNode root;
	private DeobfStats stats;
	private String detectedObfuscator;

	public DeobfuscationAnalyzer(RootNode root) {
		this.root = root;
	}

	/**
	 * Analyze the codebase and collect obfuscation statistics.
	 */
	public DeobfStats analyze() {
		stats = new DeobfStats();

		Map<String, Integer> obfuscatorVotes = new HashMap<>();

		for (ClassNode cls : root.getClasses()) {
			analyzeClass(cls, obfuscatorVotes);
		}

		// Determine most likely obfuscator
		int maxVotes = 0;
		for (Map.Entry<String, Integer> entry : obfuscatorVotes.entrySet()) {
			if (entry.getValue() > maxVotes) {
				maxVotes = entry.getValue();
				detectedObfuscator = entry.getKey();
			}
		}

		stats.detectedObfuscator = detectedObfuscator;
		stats.calculatePercentages();

		return stats;
	}

	private void analyzeClass(ClassNode cls, Map<String, Integer> obfuscatorVotes) {
		stats.totalClasses++;

		String className = cls.getName();
		if (isLikelyObfuscated(className)) {
			stats.obfuscatedClasses++;

			String obfType = ObfuscatorPatternCondition.detectObfuscatorType(className);
			if (obfType != null) {
				obfuscatorVotes.merge(obfType, 1, Integer::sum);
			}
		}

		// Analyze fields
		for (FieldNode fld : cls.getFields()) {
			stats.totalFields++;
			if (isLikelyObfuscated(fld.getName())) {
				stats.obfuscatedFields++;
			}
		}

		// Analyze methods
		for (MethodNode mth : cls.getMethods()) {
			// Skip constructors and synthetic methods
			if (mth.isConstructor() || mth.getAccessFlags().isSynthetic()) {
				continue;
			}
			stats.totalMethods++;
			if (isLikelyObfuscated(mth.getName())) {
				stats.obfuscatedMethods++;
			}
		}
	}

	/**
	 * Check if a name is likely obfuscated using multiple heuristics.
	 */
	private boolean isLikelyObfuscated(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		// Check for obfuscator patterns
		int confidence = ObfuscatorPatternCondition.getObfuscationConfidence(name);
		if (confidence >= 50) {
			return true;
		}

		// Check for high entropy
		double entropy = EntropyBasedCondition.calculateShannonEntropy(name);
		if (entropy > 4.0 && name.length() > 3) {
			return true;
		}

		// Check for obfuscator-specific patterns
		if (EntropyBasedCondition.hasObfuscatorPattern(name)) {
			return true;
		}

		return false;
	}

	/**
	 * Get a summary report of the analysis.
	 */
	public String getReport() {
		if (stats == null) {
			analyze();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("=== Deobfuscation Analysis Report ===\n\n");

		if (detectedObfuscator != null) {
			sb.append("Detected Obfuscator: ").append(detectedObfuscator).append("\n\n");
		}

		sb.append("Classes:\n");
		sb.append(String.format("  Total: %d\n", stats.totalClasses));
		sb.append(String.format("  Obfuscated: %d (%.1f%%)\n", stats.obfuscatedClasses, stats.classObfuscationRate));

		sb.append("\nMethods:\n");
		sb.append(String.format("  Total: %d\n", stats.totalMethods));
		sb.append(String.format("  Obfuscated: %d (%.1f%%)\n", stats.obfuscatedMethods, stats.methodObfuscationRate));

		sb.append("\nFields:\n");
		sb.append(String.format("  Total: %d\n", stats.totalFields));
		sb.append(String.format("  Obfuscated: %d (%.1f%%)\n", stats.obfuscatedFields, stats.fieldObfuscationRate));

		sb.append("\nOverall Obfuscation Rate: ");
		sb.append(String.format("%.1f%%\n", stats.overallObfuscationRate));

		sb.append("\nRecommendation: ");
		if (stats.overallObfuscationRate > 70) {
			sb.append("Use AGGRESSIVE deobfuscation mode\n");
		} else if (stats.overallObfuscationRate > 30) {
			sb.append("Use ENHANCED deobfuscation mode\n");
		} else if (stats.overallObfuscationRate > 10) {
			sb.append("Use DEFAULT deobfuscation mode\n");
		} else {
			sb.append("Use CONSERVATIVE deobfuscation mode or disable\n");
		}

		return sb.toString();
	}

	/**
	 * Log analysis results.
	 */
	public void logAnalysis() {
		if (stats == null) {
			analyze();
		}

		LOG.info("Deobfuscation Analysis:");
		if (detectedObfuscator != null) {
			LOG.info("  Detected obfuscator: {}", detectedObfuscator);
		}
		LOG.info("  Classes: {} total, {} obfuscated ({:.1f}%)",
				stats.totalClasses, stats.obfuscatedClasses, stats.classObfuscationRate);
		LOG.info("  Methods: {} total, {} obfuscated ({:.1f}%)",
				stats.totalMethods, stats.obfuscatedMethods, stats.methodObfuscationRate);
		LOG.info("  Fields: {} total, {} obfuscated ({:.1f}%)",
				stats.totalFields, stats.obfuscatedFields, stats.fieldObfuscationRate);
		LOG.info("  Overall obfuscation rate: {:.1f}%", stats.overallObfuscationRate);
	}

	public DeobfStats getStats() {
		if (stats == null) {
			analyze();
		}
		return stats;
	}

	public String getDetectedObfuscator() {
		if (stats == null) {
			analyze();
		}
		return detectedObfuscator;
	}

	/**
	 * Statistics about obfuscation in the codebase.
	 */
	public static class DeobfStats {
		public int totalClasses = 0;
		public int obfuscatedClasses = 0;
		public int totalMethods = 0;
		public int obfuscatedMethods = 0;
		public int totalFields = 0;
		public int obfuscatedFields = 0;

		public double classObfuscationRate = 0;
		public double methodObfuscationRate = 0;
		public double fieldObfuscationRate = 0;
		public double overallObfuscationRate = 0;

		public String detectedObfuscator = null;

		void calculatePercentages() {
			if (totalClasses > 0) {
				classObfuscationRate = (double) obfuscatedClasses / totalClasses * 100;
			}
			if (totalMethods > 0) {
				methodObfuscationRate = (double) obfuscatedMethods / totalMethods * 100;
			}
			if (totalFields > 0) {
				fieldObfuscationRate = (double) obfuscatedFields / totalFields * 100;
			}

			int total = totalClasses + totalMethods + totalFields;
			int obfuscated = obfuscatedClasses + obfuscatedMethods + obfuscatedFields;
			if (total > 0) {
				overallObfuscationRate = (double) obfuscated / total * 100;
			}
		}

		/**
		 * Check if the codebase appears to be heavily obfuscated.
		 */
		public boolean isHeavilyObfuscated() {
			return overallObfuscationRate > 50;
		}

		/**
		 * Check if the codebase appears to be lightly obfuscated.
		 */
		public boolean isLightlyObfuscated() {
			return overallObfuscationRate > 10 && overallObfuscationRate <= 50;
		}

		/**
		 * Check if the codebase appears to have minimal or no obfuscation.
		 */
		public boolean isMinimallyObfuscated() {
			return overallObfuscationRate <= 10;
		}
	}
}
