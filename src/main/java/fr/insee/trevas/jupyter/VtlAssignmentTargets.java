/* (C)2024 */
package fr.insee.trevas.jupyter;

import javax.script.Bindings;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects VTL assignment targets ({@code var := ...} or {@code var <- ...}) in notebook cells. */
final class VtlAssignmentTargets {

	private static final Pattern TARGET =
			Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)");

	private static final Pattern FEEDBACK_TARGET =
			Pattern.compile(
					"([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)\\s*"
							+ "(loadParquet|loadCSV|loadSas|loadSDMXEmptySource|loadSDMXSource"
							+ "|writeParquet|writeCSV|showMetadata|show|getSize|size)\\s*\\(",
					Pattern.CASE_INSENSITIVE);

	private VtlAssignmentTargets() {}

	static List<String> allIn(String expression) {
		return match(expression, TARGET, 1);
	}

	static List<FeedbackTarget> feedbackIn(String expression) {
		List<FeedbackTarget> targets = new ArrayList<>();
		Matcher matcher = FEEDBACK_TARGET.matcher(expression);
		while (matcher.find()) {
			targets.add(new FeedbackTarget(matcher.group(1), normalizeOperation(matcher.group(2))));
		}
		return targets;
	}

	/**
	 * Removes assignment targets from bindings so a notebook cell can be re-run without Trevas
	 * rejecting reassignment of existing variables.
	 */
	static void clearFrom(Bindings bindings, String expression) {
		allIn(expression).forEach(bindings::remove);
	}

	private static List<String> match(String expression, Pattern pattern, int group) {
		List<String> targets = new ArrayList<>();
		Matcher matcher = pattern.matcher(expression);
		while (matcher.find()) {
			targets.add(matcher.group(group));
		}
		return targets;
	}

	private static String normalizeOperation(String operation) {
		return "size".equalsIgnoreCase(operation) ? "getSize" : operation;
	}

	record FeedbackTarget(String variable, String operation) {}
}
