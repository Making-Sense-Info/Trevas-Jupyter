/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoadAssignmentContextTest {

	@AfterEach
	void cleanup() {
		LoadAssignmentContext.clear();
	}

	@Test
	void pollForOnlyConsumesMatchingOperation() {
		LoadAssignmentContext.prepare(
				"m := showMetadata(taxi); d := show(taxi); taxi := loadParquet(\"out\");");

		assertThat(LoadAssignmentContext.pollFor("showMetadata")).contains("m");
		assertThat(LoadAssignmentContext.pollFor("loadParquet")).isEmpty();
		assertThat(LoadAssignmentContext.pollFor("show")).contains("d");
		assertThat(LoadAssignmentContext.pollFor("loadParquet")).contains("taxi");
	}

	@Test
	void loadMessageWithoutVariableNameUsesGenericFormat() {
		assertThat(
						LoadAssignmentContext.formatLoadMessage(
								null, "src/test/resources/ds1.csv", "csv", 2))
				.isEqualTo("Dataset loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)");
	}

	@Test
	void writeMessageWithVariableNameIncludesName() {
		assertThat(
						LoadAssignmentContext.formatWriteMessage(
								"w", "output", "parquet"))
				.isEqualTo("Dataset 'w' written to 'output' (parquet)");
	}

	@Test
	void formatCalculatedMessageUsesVariableName() {
		assertThat(LoadAssignmentContext.formatCalculatedMessage("m")).isEqualTo("m calculated");
	}
}
