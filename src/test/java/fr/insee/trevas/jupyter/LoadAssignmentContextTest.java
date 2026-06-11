/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoadAssignmentContextTest {

	@Test
	void loadMessageWithoutVariableNameUsesGenericFormat() {
		assertThat(
						LoadAssignmentContext.formatLoadMessage(
								null, "src/test/resources/ds1.csv", "csv", 2))
				.isEqualTo("Dataset loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)");
	}

	@Test
	void loadMessageWithVariableNameIncludesName() {
		assertThat(
						LoadAssignmentContext.formatLoadMessage(
								"ds", "src/test/resources/ds1.csv", "csv", 2))
				.isEqualTo("Dataset 'ds' loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)");
	}

	@Test
	void formatCalculatedMessageUsesVariableName() {
		assertThat(LoadAssignmentContext.formatCalculatedMessage("m")).isEqualTo("m calculated");
	}

	@Test
	void formatSdmxLoadMessageWithAndWithoutVariableName() {
		assertThat(
						LoadAssignmentContext.formatSdmxLoadMessage(
								null, "msg.xml", "sdmx", 3))
				.isEqualTo("Dataset loaded from 'msg.xml' (sdmx, 3 components)");
		assertThat(
						LoadAssignmentContext.formatSdmxLoadMessage(
								"ds", "msg.xml", "sdmx", 3))
				.isEqualTo("Dataset 'ds' loaded from 'msg.xml' (sdmx, 3 components)");
	}
}
