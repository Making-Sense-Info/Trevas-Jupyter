/* (C)2024 */
package fr.insee.trevas.jupyter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tracks VTL assignment targets for load* calls in the current eval statement. */
final class LoadAssignmentContext {

	private static final Pattern LOAD_ASSIGNMENT =
			Pattern.compile(
					"([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)\\s*"
							+ "(load(?:Parquet|CSV|Sas|SDMX(?:EmptySource|Source)))\\s*\\(",
					Pattern.CASE_INSENSITIVE);

	private static final ThreadLocal<Deque<String>> TARGETS =
			ThreadLocal.withInitial(ArrayDeque::new);

	private LoadAssignmentContext() {}

	static void prepare(String expression) {
		Deque<String> targets = TARGETS.get();
		targets.clear();
		targets.addAll(extractAssignmentTargets(expression));
	}

	static Optional<String> pollTarget() {
		return Optional.ofNullable(TARGETS.get().poll());
	}

	static void clear() {
		TARGETS.get().clear();
	}

	static List<String> extractAssignmentTargets(String expression) {
		List<String> targets = new ArrayList<>();
		Matcher matcher = LOAD_ASSIGNMENT.matcher(expression);
		while (matcher.find()) {
			targets.add(matcher.group(1));
		}
		return targets;
	}

	static String formatLoadMessage(String variable, String location, String format, int columns) {
		if (variable == null) {
			return String.format(
					"Dataset loaded from '%s' (%s, %d columns)", location, format, columns);
		}
		return String.format(
				"Dataset '%s' loaded from '%s' (%s, %d columns)",
				variable, location, format, columns);
	}

	static String formatSdmxLoadMessage(
			String variable, String location, String details, int components) {
		if (variable == null) {
			return String.format(
					"Dataset loaded from '%s' (%s, %d components)", location, details, components);
		}
		return String.format(
				"Dataset '%s' loaded from '%s' (%s, %d components)",
				variable, location, details, components);
	}
}
