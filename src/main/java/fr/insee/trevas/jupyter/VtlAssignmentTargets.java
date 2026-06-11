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

	private static final Pattern KERNEL_FEEDBACK_TARGET =
			Pattern.compile(
					"([A-Za-z_][A-Za-z0-9_]*)\\s*(?::=|<-)\\s*"
							+ "(load(?:Parquet|CSV|Sas|SDMX(?:EmptySource|Source))"
							+ "|showMetadata|show)\\s*\\(",
					Pattern.CASE_INSENSITIVE);

	private VtlAssignmentTargets() {}

	static List<String> allIn(String expression) {
		return match(expression, TARGET);
	}

	static List<String> withKernelFeedbackIn(String expression) {
		return match(expression, KERNEL_FEEDBACK_TARGET);
	}

	/**
	 * Removes assignment targets from bindings so a notebook cell can be re-run without Trevas
	 * rejecting reassignment of existing variables.
	 */
	static void clearFrom(Bindings bindings, String expression) {
		allIn(expression).forEach(bindings::remove);
	}

	private static List<String> match(String expression, Pattern pattern) {
		List<String> targets = new ArrayList<>();
		Matcher matcher = pattern.matcher(expression);
		while (matcher.find()) {
			targets.add(matcher.group(1));
		}
		return targets;
	}
}
