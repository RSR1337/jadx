package jadx.core.deobf;

import jadx.api.deobf.IAliasProvider;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;

/**
 * Enhanced alias provider that generates more meaningful and context-aware
 * names
 * for deobfuscated elements. This provider uses semantic analysis to create
 * names that better reflect the purpose of the code elements.
 */
public class EnhancedAliasProvider implements IAliasProvider {

	private int pkgIndex = 0;
	private int clsIndex = 0;
	private int fldIndex = 0;
	private int mthIndex = 0;

	private int maxLength;
	private boolean useSemanticNaming = true;
	private boolean preserveOriginalHint = true;

	@Override
	public void init(RootNode root) {
		this.maxLength = root.getArgs().getDeobfuscationMaxLength();
	}

	@Override
	public void initIndexes(int pkg, int cls, int fld, int mth) {
		pkgIndex = pkg;
		clsIndex = cls;
		fldIndex = fld;
		mthIndex = mth;
	}

	@Override
	public String forPackage(PackageNode pkg) {
		String baseName = extractMeaningfulPart(pkg.getPkgInfo().getName());
		return String.format("pkg%03d%s", pkgIndex++, formatNamePart(baseName));
	}

	@Override
	public String forClass(ClassNode cls) {
		String prefix = makeClsPrefix(cls);
		String semantic = useSemanticNaming ? extractClassSemantics(cls) : "";
		String hint = preserveOriginalHint ? formatNamePart(cls.getName()) : "";

		return String.format("%sC%04d%s%s", prefix, clsIndex++, semantic, hint);
	}

	@Override
	public String forField(FieldNode fld) {
		String typeHint = getTypeBasedHint(fld.getType());
		String originalHint = preserveOriginalHint ? formatNamePart(fld.getName()) : "";

		return String.format("%s%d%s", typeHint, fldIndex++, originalHint);
	}

	@Override
	public String forMethod(MethodNode mth) {
		String prefix = getMethodPrefix(mth);
		String returnHint = getReturnTypeHint(mth);
		String originalHint = preserveOriginalHint ? formatNamePart(mth.getName()) : "";

		return String.format("%s%d%s%s", prefix, mthIndex++, returnHint, originalHint);
	}

	/**
	 * Generate a prefix for a class name based on class properties and hierarchy.
	 */
	private String makeClsPrefix(ClassNode cls) {
		StringBuilder result = new StringBuilder();

		if (cls.isEnum()) {
			return "Enum";
		}

		if (cls.getAccessFlags().isInterface()) {
			result.append("I");
		} else if (cls.getAccessFlags().isAbstract()) {
			result.append("Abstract");
		}

		result.append(getBaseName(cls));
		return result.toString();
	}

	/**
	 * Extract semantic information from class structure to generate meaningful
	 * names.
	 */
	private String extractClassSemantics(ClassNode cls) {
		StringBuilder semantic = new StringBuilder();

		// Check for common patterns based on implemented interfaces
		for (ArgType interfaceType : cls.getInterfaces()) {
			String name = interfaceType.getObject();

			if (name.contains("Serializable")) {
				semantic.append("Ser");
				break;
			}
			if (name.contains("Parcelable")) {
				semantic.append("Parcel");
				break;
			}
			if (name.contains("Comparable")) {
				semantic.append("Cmp");
				break;
			}
			if (name.contains("Cloneable")) {
				semantic.append("Clone");
				break;
			}
			if (name.contains("Iterable")) {
				semantic.append("Iter");
				break;
			}
			if (name.contains("Collection")) {
				semantic.append("Coll");
				break;
			}
		}

		// Check for singleton pattern
		if (hasSingletonPattern(cls)) {
			if (semantic.length() == 0) {
				semantic.append("Single");
			}
		}

		// Check for builder pattern
		if (hasBuilderPattern(cls)) {
			if (semantic.length() == 0) {
				semantic.append("Builder");
			}
		}

		// Check for callback/listener pattern
		if (hasCallbackPattern(cls)) {
			if (semantic.length() == 0) {
				semantic.append("Cb");
			}
		}

		return semantic.toString();
	}

	/**
	 * Check if class has singleton pattern.
	 */
	private boolean hasSingletonPattern(ClassNode cls) {
		boolean hasPrivateConstructor = false;
		boolean hasStaticInstance = false;
		boolean hasGetInstance = false;

		for (MethodNode mth : cls.getMethods()) {
			if (mth.isConstructor() && mth.getAccessFlags().isPrivate()) {
				hasPrivateConstructor = true;
			}
			if (mth.getName().toLowerCase().contains("instance")
					&& mth.getAccessFlags().isStatic()) {
				hasGetInstance = true;
			}
		}

		for (FieldNode fld : cls.getFields()) {
			if (fld.getAccessFlags().isStatic()
					&& fld.getType().equals(cls.getClassInfo().getType())) {
				hasStaticInstance = true;
			}
		}

		return hasPrivateConstructor && (hasStaticInstance || hasGetInstance);
	}

	/**
	 * Check if class has builder pattern.
	 */
	private boolean hasBuilderPattern(ClassNode cls) {
		int builderMethods = 0;
		boolean hasBuildMethod = false;

		for (MethodNode mth : cls.getMethods()) {
			// Builder methods typically return the class itself
			if (mth.getReturnType().equals(cls.getClassInfo().getType())) {
				builderMethods++;
			}
			if (mth.getName().equals("build") || mth.getName().equals("create")) {
				hasBuildMethod = true;
			}
		}

		return builderMethods >= 3 && hasBuildMethod;
	}

	/**
	 * Check if class has callback/listener pattern.
	 */
	private boolean hasCallbackPattern(ClassNode cls) {
		if (!cls.getAccessFlags().isInterface()) {
			return false;
		}

		for (MethodNode mth : cls.getMethods()) {
			String name = mth.getName().toLowerCase();
			if (name.startsWith("on")
					|| name.contains("callback")
					|| name.contains("listener")
					|| name.contains("handler")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get a hint based on the field type.
	 */
	private String getTypeBasedHint(ArgType type) {
		if (type == null) {
			return "f";
		}

		// Primitive types
		if (type == ArgType.BOOLEAN) {
			return "is";
		}
		if (type == ArgType.INT) {
			return "i";
		}
		if (type == ArgType.LONG) {
			return "l";
		}
		if (type == ArgType.FLOAT) {
			return "f";
		}
		if (type == ArgType.DOUBLE) {
			return "d";
		}
		if (type == ArgType.BYTE) {
			return "b";
		}
		if (type == ArgType.SHORT) {
			return "s";
		}
		if (type == ArgType.CHAR) {
			return "c";
		}

		// Array types
		if (type.isArray()) {
			return "arr";
		}

		// Object types
		if (type.isObject()) {
			String objName = type.getObject();
			if (objName != null) {
				if (objName.equals("java.lang.String")) {
					return "str";
				}
				if (objName.contains("List")) {
					return "list";
				}
				if (objName.contains("Map")) {
					return "map";
				}
				if (objName.contains("Set")) {
					return "set";
				}
				if (objName.contains("View")) {
					return "view";
				}
				if (objName.contains("Context")) {
					return "ctx";
				}
				if (objName.contains("Handler")) {
					return "handler";
				}
				if (objName.contains("Intent")) {
					return "intent";
				}
				if (objName.contains("Bundle")) {
					return "bundle";
				}
			}
			return "obj";
		}

		return "f";
	}

	/**
	 * Get method prefix based on method characteristics.
	 */
	private String getMethodPrefix(MethodNode mth) {
		if (mth.contains(AType.METHOD_OVERRIDE)) {
			return "mo";
		}

		// Check return type for hints
		ArgType returnType = mth.getReturnType();
		if (returnType == ArgType.BOOLEAN) {
			return "check";
		}
		if (returnType == ArgType.VOID) {
			return "do";
		}

		// Check if this might be a getter
		int argCount = mth.getMethodInfo().getArgsCount();
		if (argCount == 0 && returnType != ArgType.VOID) {
			return "get";
		}

		// Check if this might be a setter
		if (argCount == 1 && returnType == ArgType.VOID) {
			return "set";
		}

		return "m";
	}

	/**
	 * Get a hint based on return type.
	 */
	private String getReturnTypeHint(MethodNode mth) {
		ArgType returnType = mth.getReturnType();
		if (returnType == null || returnType == ArgType.VOID) {
			return "";
		}

		if (returnType == ArgType.BOOLEAN) {
			return "Bool";
		}
		if (returnType.isObject()) {
			String objName = returnType.getObject();
			if (objName != null) {
				if (objName.equals("java.lang.String")) {
					return "Str";
				}
				if (objName.contains("List")) {
					return "List";
				}
				if (objName.contains("Map")) {
					return "Map";
				}
			}
		}

		return "";
	}

	/**
	 * Process current class and all super classes to get meaningful parent name.
	 */
	private static String getBaseName(ClassNode cls) {
		ClassNode currentCls = cls;
		while (currentCls != null) {
			ArgType superCls = currentCls.getSuperClass();
			if (superCls != null) {
				String superClsName = superCls.getObject();

				// Android framework classes
				if (superClsName.startsWith("android.app.")) {
					return getClsName(superClsName);
				}
				if (superClsName.startsWith("android.os.")) {
					return getClsName(superClsName);
				}
				if (superClsName.startsWith("android.view.")) {
					return getClsName(superClsName);
				}
				if (superClsName.startsWith("android.widget.")) {
					return getClsName(superClsName);
				}
				if (superClsName.startsWith("android.content.")) {
					return getClsName(superClsName);
				}
				if (superClsName.startsWith("androidx.")) {
					return getClsName(superClsName);
				}

				// Java framework classes
				if (superClsName.startsWith("java.lang.Thread")) {
					return "Thread";
				}
				if (superClsName.startsWith("java.lang.Exception")) {
					return "Exception";
				}
				if (superClsName.contains("Exception")) {
					return "Exception";
				}
			}

			// Check interfaces
			for (ArgType interfaceType : cls.getInterfaces()) {
				String name = interfaceType.getObject();
				if (name.equals("java.lang.Runnable")) {
					return "Runnable";
				}
				if (name.startsWith("java.util.concurrent.")) {
					return getClsName(name);
				}
				if (name.startsWith("android.view.")) {
					return getClsName(name);
				}
				if (name.startsWith("android.content.")) {
					return getClsName(name);
				}
			}

			if (superCls == null) {
				break;
			}
			currentCls = cls.root().resolveClass(superCls);
		}
		return "";
	}

	private static String getClsName(String name) {
		int pkgEnd = name.lastIndexOf('.');
		String clsName = name.substring(pkgEnd + 1);
		return StringUtils.removeChar(clsName, '$');
	}

	/**
	 * Format name part for inclusion in alias.
	 */
	private String formatNamePart(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}
		if (name.length() > maxLength) {
			return 'x' + Integer.toHexString(name.hashCode());
		}
		return NameMapper.removeInvalidCharsMiddle(name);
	}

	/**
	 * Extract meaningful part from an obfuscated name.
	 */
	private String extractMeaningfulPart(String name) {
		if (name == null || name.isEmpty()) {
			return "";
		}

		// Remove common obfuscation patterns
		String cleaned = name.replaceAll("[0-9]+$", ""); // Remove trailing numbers
		cleaned = cleaned.replaceAll("^[_$]+", ""); // Remove leading _ or $
		cleaned = cleaned.replaceAll("[_$]+$", ""); // Remove trailing _ or $

		if (cleaned.isEmpty() || cleaned.length() <= 2) {
			return "";
		}

		return cleaned;
	}

	// Getters and setters for configuration

	public boolean isUseSemanticNaming() {
		return useSemanticNaming;
	}

	public void setUseSemanticNaming(boolean useSemanticNaming) {
		this.useSemanticNaming = useSemanticNaming;
	}

	public boolean isPreserveOriginalHint() {
		return preserveOriginalHint;
	}

	public void setPreserveOriginalHint(boolean preserveOriginalHint) {
		this.preserveOriginalHint = preserveOriginalHint;
	}
}
