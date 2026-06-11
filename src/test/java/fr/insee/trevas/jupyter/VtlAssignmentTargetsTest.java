/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import static org.assertj.core.api.Assertions.assertThat;

class VtlAssignmentTargetsTest {

	@Test
	void allInExtractsTemporaryAndPersistentAssignments() {
		assertThat(
						VtlAssignmentTargets.allIn(
								"taxi <- loadCSV(\"a.csv\"); m := showMetadata(taxi); x := 1 + 2;"))
				.containsExactly("taxi", "m", "x");
	}

	@Test
	void allInExtractsCalcAssignments() {
		assertThat(
						VtlAssignmentTargets.allIn(
								"ds <- loadCSV(\"a.csv\"); out <- ds[calc measure \"x\" := 1];"))
				.containsExactly("ds", "out");
	}

	@Test
	void allInReturnsEmptyWhenNoAssignment() {
		assertThat(VtlAssignmentTargets.allIn("show(taxi); getSize(taxi);")).isEmpty();
	}

	@Test
	void withKernelFeedbackInOnlyMatchesLoadAndShowCalls() {
		assertThat(
						VtlAssignmentTargets.withKernelFeedbackIn(
								"taxi <- loadCSV(\"a.csv\"); m := showMetadata(taxi); n := getSize(taxi);"))
				.containsExactly("taxi", "m");
	}

	@Test
	void withKernelFeedbackInMatchesSdmxLoads() {
		assertThat(
						VtlAssignmentTargets.withKernelFeedbackIn(
								"empty := loadSDMXEmptySource(\"msg.xml\", \"ID1\"); "
										+ "full <- loadSDMXSource(\"msg.xml\", \"ID1\", \"data.csv\");"))
				.containsExactly("empty", "full");
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
