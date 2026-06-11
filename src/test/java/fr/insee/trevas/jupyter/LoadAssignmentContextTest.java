/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadAssignmentContextTest {

	@Test
	void extractsSingleTemporaryAssignment() {
		assertThat(
						LoadAssignmentContext.extractAssignmentTargets(
								"ds := loadCSV(\"src/test/resources/ds1.csv\");"))
				.containsExactly("ds");
	}

	@Test
	void extractsPersistentAssignment() {
		assertThat(
						LoadAssignmentContext.extractAssignmentTargets(
								"ds <- loadParquet(\"/data/out.parquet\");"))
				.containsExactly("ds");
	}

	@Test
	void extractsMultipleAssignmentsInOrder() {
		List<String> targets =
				LoadAssignmentContext.extractAssignmentTargets(
						"a := loadCSV(\"a.csv\"); b := loadParquet(\"b.parquet\");");
		assertThat(targets).containsExactly("a", "b");
	}

	@Test
	void ignoresLoadWithoutAssignment() {
		assertThat(LoadAssignmentContext.extractAssignmentTargets("loadCSV(\"a.csv\");"))
				.isEmpty();
	}

	@Test
	void loadMessageWithoutVariableNameUsesGenericFormat() {
		assertThat(
						LoadAssignmentContext.formatLoadMessage(
								null, "src/test/resources/ds1.csv", "csv", 2))
				.isEqualTo("Dataset loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)");
	}

	@Test
	void extractsSdmxLoadAssignments() {
		assertThat(
						LoadAssignmentContext.extractAssignmentTargets(
								"empty := loadSDMXEmptySource(\"msg.xml\", \"ID1\"); "
										+ "full <- loadSDMXSource(\"msg.xml\", \"ID1\", \"data.csv\");"))
				.containsExactly("empty", "full");
	}
}
