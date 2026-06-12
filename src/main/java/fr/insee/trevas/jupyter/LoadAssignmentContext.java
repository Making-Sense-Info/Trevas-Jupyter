/* (C)2024 */
package fr.insee.trevas.jupyter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Tracks VTL assignment targets for kernel calls that emit UI feedback in the current eval. */
final class LoadAssignmentContext {

	private static final ThreadLocal<Deque<VtlAssignmentTargets.FeedbackTarget>> TARGETS =
			ThreadLocal.withInitial(ArrayDeque::new);

	private LoadAssignmentContext() {}

	static void prepare(String expression) {
		Deque<VtlAssignmentTargets.FeedbackTarget> targets = TARGETS.get();
		targets.clear();
		targets.addAll(VtlAssignmentTargets.feedbackIn(expression));
	}

	static Optional<String> pollFor(String operation) {
		Deque<VtlAssignmentTargets.FeedbackTarget> targets = TARGETS.get();
		VtlAssignmentTargets.FeedbackTarget head = targets.peek();
		if (head != null && head.operation().equalsIgnoreCase(operation)) {
			targets.poll();
			return Optional.of(head.variable());
		}
		return Optional.empty();
	}

	static void clear() {
		TARGETS.get().clear();
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

	static String formatWriteMessage(String variable, String location, String format) {
		if (variable == null) {
			return String.format("Dataset written to '%s' (%s)", location, format);
		}
		return String.format("Dataset '%s' written to '%s' (%s)", variable, location, format);
	}

	static String formatCalculatedMessage(String variable) {
		return variable + " calculated";
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
