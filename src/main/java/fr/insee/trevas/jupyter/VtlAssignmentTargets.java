/* (C)2024 */
package fr.insee.trevas.jupyter;

import javax.script.Bindings;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects VTL assignment targets ({@code var := ...} or {@code var <- ...}) in notebook cells. */
final class VtlAssignmentTargets {

	private static final Pattern TOP_LEVEL_TARGET =
			Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)");

	private static final Pattern FEEDBACK_TARGET =
			Pattern.compile(
					"([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)\\s*"
							+ "(loadParquet|loadCSV|loadSas|loadSDMXEmptySource|loadSDMXSource"
							+ "|writeParquet|writeCSV|showMetadata|show|getSize|size)\\s*\\(",
					Pattern.CASE_INSENSITIVE);

	private VtlAssignmentTargets() {}

	/** Top-level statement assignments only (ignores {@code :=} inside {@code [calc ...]} blocks). */
	static List<String> topLevelIn(String expression) {
		List<String> targets = new ArrayList<>();
		for (String part : expression.split(";")) {
			String statement = part.trim();
			if (statement.isEmpty()) {
				continue;
			}
			Matcher matcher = TOP_LEVEL_TARGET.matcher(statement);
			if (matcher.find()) {
				targets.add(matcher.group(1));
			}
		}
		return targets;
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
		topLevelIn(expression).forEach(bindings::remove);
	}

	private static String normalizeOperation(String operation) {
		return "size".equalsIgnoreCase(operation) ? "getSize" : operation;
	}

	record FeedbackTarget(String variable, String operation) {}
}
