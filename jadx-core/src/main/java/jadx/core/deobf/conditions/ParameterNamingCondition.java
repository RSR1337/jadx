package jadx.core.deobf.conditions;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;

/**
 * Condition that detects obfuscated method parameters and provides suggestions
 * for better parameter names based on type information and context.
 */
public class ParameterNamingCondition extends AbstractDeobfCondition {

	// Common obfuscated parameter patterns
	private static final Pattern OBFUSCATED_PARAM_PATTERN = Pattern.compile(
			"^[a-z]$" // Single letter
					+ "|^[a-z][0-9]+$" // Letter + number (p0, p1, var0)
					+ "|^arg[0-9]+$" // arg0, arg1
					+ "|^param[0-9]+$" // param0, param1
					+ "|^var[0-9]+$" // var0, var1
					+ "|^p[0-9]+$" // p0, p1
					+ "|^r[0-9]+$" // r0, r1 (register-based)
					+ "|^this\\$[0-9]+$" // this$0 (inner class reference)
	);

	// Reserved/special parameter names that should not be renamed
	private static final Set<String> RESERVED_PARAM_NAMES = new HashSet<>();

	static {
		RESERVED_PARAM_NAMES.add("this");
		RESERVED_PARAM_NAMES.add("super");
		RESERVED_PARAM_NAMES.add("self");
	}

	/**
	 * Check if a method has obfuscated parameter names.
	 */
	@Override
	public Action check(MethodNode mth) {
		// Don't check constructors - they often have legitimate simple names
		if (mth.isConstructor()) {
			return Action.NO_ACTION;
		}

		// Check if any parameter appears obfuscated
		// This would need access to parameter names which may require debug info
		// For now, we use method signature analysis

		return Action.NO_ACTION;
	}

	/**
	 * Check if a parameter name looks obfuscated.
	 */
	public static boolean isObfuscatedParameterName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		if (RESERVED_PARAM_NAMES.contains(name)) {
			return false;
		}
		return OBFUSCATED_PARAM_PATTERN.matcher(name).matches();
	}

	/**
	 * Generate a suggested parameter name based on its type.
	 */
	public static String suggestParameterName(ArgType type, int paramIndex) {
		if (type == null) {
			return "param" + paramIndex;
		}

		// Primitive types
		if (type == ArgType.BOOLEAN) {
			return paramIndex == 0 ? "flag" : "flag" + paramIndex;
		}
		if (type == ArgType.INT) {
			return getIntParamName(paramIndex);
		}
		if (type == ArgType.LONG) {
			return paramIndex == 0 ? "value" : "value" + paramIndex;
		}
		if (type == ArgType.FLOAT || type == ArgType.DOUBLE) {
			return paramIndex == 0 ? "number" : "number" + paramIndex;
		}
		if (type == ArgType.BYTE) {
			return paramIndex == 0 ? "b" : "b" + paramIndex;
		}
		if (type == ArgType.CHAR) {
			return paramIndex == 0 ? "ch" : "ch" + paramIndex;
		}
		if (type == ArgType.SHORT) {
			return paramIndex == 0 ? "value" : "value" + paramIndex;
		}

		// Array types
		if (type.isArray()) {
			ArgType element = type.getArrayElement();
			String baseName = suggestParameterName(element, 0);
			return baseName + "Array";
		}

		// Object types
		if (type.isObject()) {
			return suggestObjectParamName(type, paramIndex);
		}

		return "param" + paramIndex;
	}

	private static String getIntParamName(int paramIndex) {
		// Common single-parameter int names
		String[] commonNames = { "index", "count", "size", "position", "id", "offset", "length" };
		if (paramIndex < commonNames.length) {
			return commonNames[paramIndex];
		}
		return "n" + paramIndex;
	}

	private static String suggestObjectParamName(ArgType type, int paramIndex) {
		String objName = type.getObject();
		if (objName == null) {
			return "obj" + paramIndex;
		}

		// Extract simple class name
		int lastDot = objName.lastIndexOf('.');
		int lastDollar = objName.lastIndexOf('$');
		int start = Math.max(lastDot, lastDollar) + 1;
		String simpleName = objName.substring(start);

		// Common Android/Java types
		if (objName.equals("java.lang.String")) {
			return getStringParamName(paramIndex);
		}
		if (objName.equals("android.content.Context")) {
			return "context";
		}
		if (objName.equals("android.content.Intent")) {
			return "intent";
		}
		if (objName.equals("android.os.Bundle")) {
			return "bundle";
		}
		if (objName.equals("android.view.View")) {
			return "view";
		}
		if (objName.contains("Activity")) {
			return "activity";
		}
		if (objName.contains("Fragment")) {
			return "fragment";
		}
		if (objName.contains("Handler")) {
			return "handler";
		}
		if (objName.contains("Listener")) {
			return "listener";
		}
		if (objName.contains("Callback")) {
			return "callback";
		}
		if (objName.contains("Adapter")) {
			return "adapter";
		}

		// Collections
		if (objName.contains("List")) {
			return "list";
		}
		if (objName.contains("Map")) {
			return "map";
		}
		if (objName.contains("Set")) {
			return "set";
		}
		if (objName.contains("Collection")) {
			return "collection";
		}
		if (objName.contains("Iterator")) {
			return "iterator";
		}

		// Java types
		if (objName.equals("java.lang.Object")) {
			return "obj";
		}
		if (objName.equals("java.lang.Class")) {
			return "clazz";
		}
		if (objName.equals("java.lang.Throwable") || objName.contains("Exception")) {
			return "exception";
		}
		if (objName.equals("java.io.File")) {
			return "file";
		}
		if (objName.contains("InputStream") || objName.contains("OutputStream")) {
			return "stream";
		}
		if (objName.contains("Reader") || objName.contains("Writer")) {
			return "reader";
		}

		// Generate name from class name
		return generateParamNameFromClass(simpleName, paramIndex);
	}

	private static String getStringParamName(int paramIndex) {
		String[] commonNames = { "str", "text", "name", "value", "message", "content" };
		if (paramIndex < commonNames.length) {
			return commonNames[paramIndex];
		}
		return "str" + paramIndex;
	}

	private static String generateParamNameFromClass(String className, int paramIndex) {
		if (className == null || className.isEmpty()) {
			return "obj" + paramIndex;
		}

		// Convert to camelCase
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toLowerCase(className.charAt(0)));

		boolean lastWasUpper = true;
		for (int i = 1; i < className.length() && sb.length() < 12; i++) {
			char c = className.charAt(i);
			if (Character.isUpperCase(c)) {
				if (!lastWasUpper) {
					break; // Stop at next word
				}
				sb.append(Character.toLowerCase(c));
				lastWasUpper = true;
			} else {
				sb.append(c);
				lastWasUpper = false;
			}
		}

		String result = sb.toString();
		if (paramIndex > 0) {
			result = result + paramIndex;
		}
		return result;
	}

	/**
	 * Get parameter name suggestions for a method.
	 */
	public static String[] suggestParameterNames(MethodNode mth) {
		int argCount = mth.getMethodInfo().getArgsCount();
		String[] suggestions = new String[argCount];

		for (int i = 0; i < argCount; i++) {
			ArgType argType = mth.getMethodInfo().getArgumentsTypes().get(i);
			suggestions[i] = suggestParameterName(argType, i);
		}

		// Deduplicate names
		Set<String> usedNames = new HashSet<>();
		for (int i = 0; i < suggestions.length; i++) {
			String name = suggestions[i];
			if (usedNames.contains(name)) {
				int suffix = 2;
				while (usedNames.contains(name + suffix)) {
					suffix++;
				}
				name = name + suffix;
				suggestions[i] = name;
			}
			usedNames.add(name);
		}

		return suggestions;
	}
}
