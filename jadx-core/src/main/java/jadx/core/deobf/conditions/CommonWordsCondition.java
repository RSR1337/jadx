package jadx.core.deobf.conditions;

import java.util.HashSet;
import java.util.Set;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;

/**
 * Condition that preserves names which are common programming terms or
 * dictionary words.
 * This helps avoid renaming meaningful names that are short but valid.
 */
public class CommonWordsCondition extends AbstractDeobfCondition {

	/**
	 * Common programming terms, API names, and design patterns that should not be
	 * renamed.
	 * These are typically short but meaningful names used in Android/Java
	 * development.
	 */
	private static final Set<String> COMMON_TERMS = new HashSet<>();

	static {
		// Common method names
		addTerms("get", "set", "is", "has", "can", "add", "put", "remove", "clear", "size",
				"init", "start", "stop", "run", "call", "execute", "invoke", "apply",
				"read", "write", "load", "save", "open", "close", "create", "destroy",
				"show", "hide", "enable", "disable", "update", "refresh", "reset",
				"parse", "format", "convert", "encode", "decode", "encrypt", "decrypt",
				"send", "receive", "post", "fetch", "request", "response",
				"bind", "unbind", "attach", "detach", "connect", "disconnect",
				"register", "unregister", "subscribe", "unsubscribe",
				"validate", "verify", "check", "test", "compare", "equals",
				"copy", "clone", "merge", "split", "join", "concat",
				"find", "search", "filter", "sort", "reverse", "shuffle",
				"log", "debug", "info", "warn", "error", "trace");

		// Common field names
		addTerms("id", "key", "value", "name", "type", "data", "text", "title", "label",
				"url", "uri", "path", "file", "dir", "root", "home", "base",
				"min", "max", "count", "total", "sum", "avg", "index", "offset", "length",
				"width", "height", "size", "x", "y", "z", "top", "left", "right", "bottom",
				"red", "green", "blue", "alpha", "color", "font", "style",
				"enabled", "visible", "active", "valid", "ready", "done", "busy",
				"parent", "child", "next", "prev", "first", "last", "current",
				"input", "output", "source", "target", "origin", "dest",
				"user", "admin", "guest", "owner", "author", "creator",
				"time", "date", "year", "month", "day", "hour", "minute", "second",
				"tag", "flag", "mode", "state", "status", "code", "result",
				"list", "map", "set", "array", "queue", "stack", "tree", "graph",
				"config", "setting", "option", "param", "arg");

		// Common Android terms
		addTerms("view", "layout", "widget", "button", "image", "icon", "drawable",
				"activity", "fragment", "service", "receiver", "provider",
				"intent", "bundle", "cursor", "adapter", "holder",
				"context", "app", "application", "system", "manager",
				"handler", "thread", "task", "job", "worker", "async",
				"listener", "callback", "observer", "event", "action",
				"menu", "item", "dialog", "toast", "snackbar", "popup",
				"recycler", "scroll", "pager", "tab", "toolbar", "fab",
				"notification", "alarm", "broadcast", "permission");

		// Common class name suffixes/prefixes
		addTerms("Activity", "Fragment", "Service", "Receiver", "Provider",
				"Adapter", "Holder", "Manager", "Helper", "Util", "Utils",
				"Factory", "Builder", "Parser", "Handler", "Listener", "Callback",
				"Impl", "Base", "Abstract", "Default", "Custom", "Simple",
				"Model", "Entity", "Bean", "Dto", "Vo", "Po", "Dao",
				"Repository", "Controller", "Presenter", "ViewModel",
				"Module", "Component", "Inject", "Scope", "Qualifier",
				"Test", "Mock", "Stub", "Fake", "Spy");

		// Common patterns
		addTerms("on", "do", "new", "old", "tmp", "temp", "obj", "ref", "ptr",
				"ctx", "msg", "cmd", "evt", "err", "ex", "e", "i", "j", "k", "n", "m",
				"sb", "db", "io", "ui", "rx", "tx");
	}

	private static void addTerms(String... terms) {
		for (String term : terms) {
			COMMON_TERMS.add(term.toLowerCase());
		}
	}

	/**
	 * Check if a name is a common term that should be preserved.
	 */
	private static boolean isCommonTerm(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		return COMMON_TERMS.contains(name.toLowerCase());
	}

	/**
	 * Check if a name contains common terms as prefix/suffix.
	 */
	private static boolean containsCommonPattern(String name) {
		if (name == null || name.length() < 4) {
			return false;
		}

		String lowerName = name.toLowerCase();

		// Check common prefixes
		String[] prefixes = { "get", "set", "is", "has", "can", "on", "do", "new" };
		for (String prefix : prefixes) {
			if (lowerName.startsWith(prefix) && lowerName.length() > prefix.length()) {
				// Check if next char is uppercase (camelCase)
				char nextChar = name.charAt(prefix.length());
				if (Character.isUpperCase(nextChar)) {
					return true;
				}
			}
		}

		// Check common suffixes
		String[] suffixes = { "Listener", "Callback", "Handler", "Adapter", "Helper",
				"Manager", "Factory", "Builder", "Impl", "Activity", "Fragment",
				"Service", "Receiver", "Provider", "View", "Layout", "Model" };
		for (String suffix : suffixes) {
			if (name.endsWith(suffix) && name.length() > suffix.length()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if name follows CamelCase convention (likely meaningful).
	 */
	private static boolean isCamelCase(String name) {
		if (name == null || name.length() < 3) {
			return false;
		}

		boolean hasLower = false;
		boolean hasUpper = false;
		boolean hasTransition = false;

		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isLowerCase(c)) {
				hasLower = true;
				if (i > 0 && Character.isUpperCase(name.charAt(i - 1))) {
					hasTransition = true;
				}
			} else if (Character.isUpperCase(c)) {
				hasUpper = true;
				if (i > 0 && Character.isLowerCase(name.charAt(i - 1))) {
					hasTransition = true;
				}
			}
		}

		return hasLower && hasUpper && hasTransition;
	}

	@Override
	public Action check(PackageNode pkg) {
		String name = pkg.getName();
		if (isCommonTerm(name)) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(ClassNode cls) {
		String name = cls.getName();
		if (isCommonTerm(name) || containsCommonPattern(name) || isCamelCase(name)) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(FieldNode fld) {
		String name = fld.getName();
		if (isCommonTerm(name) || containsCommonPattern(name)) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	@Override
	public Action check(MethodNode mth) {
		String name = mth.getName();
		if (isCommonTerm(name) || containsCommonPattern(name)) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}
}
