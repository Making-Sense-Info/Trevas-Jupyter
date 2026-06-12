/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class VtlAssignmentTargetsTest {

	@Test
	void topLevelInExtractsStatementAssignments() {
		assertThat(
						VtlAssignmentTargets.topLevelIn(
								"taxi <- loadCSV(\"a.csv\"); m := showMetadata(taxi); x := 1 + 2;"))
				.containsExactly("taxi", "m", "x");
	}

	@Test
	void topLevelInIgnoresAssignmentsInsideCalc() {
		assertThat(
						VtlAssignmentTargets.topLevelIn(
								"taxi_meta := taxi [calc identifier vendor_id := vendor_id] [keep total_amount]; "
										+ "ratio <- taxi_meta;"))
				.containsExactly("taxi_meta", "ratio");
	}

	@Test
	void feedbackInCapturesOperationType() {
		assertThat(
						VtlAssignmentTargets.feedbackIn(
								"taxi <- loadCSV(\"a.csv\"); "
										+ "w := writeParquet(\"out\", taxi); "
										+ "m := showMetadata(taxi); "
										+ "d := show(taxi);"))
				.extracting(VtlAssignmentTargets.FeedbackTarget::variable, VtlAssignmentTargets.FeedbackTarget::operation)
				.containsExactly(
						tuple("taxi", "loadCSV"),
						tuple("w", "writeParquet"),
						tuple("m", "showMetadata"),
						tuple("d", "show"));
	}

	@Test
	void feedbackInNormalizesSizeToGetSize() {
		assertThat(VtlAssignmentTargets.feedbackIn("n := size(taxi);"))
				.extracting(VtlAssignmentTargets.FeedbackTarget::operation)
				.containsExactly("getSize");
	}

	@Test
	void feedbackInReturnsEmptyWhenNoTrackedCall() {
		assertThat(VtlAssignmentTargets.feedbackIn("x := 1 + 2;")).isEmpty();
	}

	@Test
	void clearFromRemovesOnlyAssignedTargets() {
		Bindings bindings = new SimpleBindings();
		bindings.put("taxi", "dataset");
		bindings.put("m", "metadata");
		bindings.put("other", "kept");

		VtlAssignmentTargets.clearFrom(bindings, "m := showMetadata(taxi); d := show(taxi);");

		assertThat(bindings).containsOnlyKeys("taxi", "other");
	}
}
