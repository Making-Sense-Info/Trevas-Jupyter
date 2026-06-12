/* (C)2024 */
package fr.insee.trevas.jupyter;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for re-running notebook cells without Trevas reassignment errors. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VtlKernelCellRerunTest {

	private static final String DS1 = "src/test/resources/ds1.csv";

	private VtlKernel kernel;

	@BeforeAll
	void setup() throws Exception {
		kernel = new VtlKernel();
	}

	@Test
	void rerunningShowCellAllowsReassignment() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");
		kernel.eval("m := showMetadata(taxi); d := show(taxi);");

		DisplayData output = kernel.eval("m := showMetadata(taxi); d := show(taxi);");

		assertThat(text(output)).isEqualTo("m calculated\n" + "d calculated");
	}

	@Test
	void rerunningLoadCellWithTemporaryAssignment() throws Exception {
		kernel.eval("ds := loadCSV(\"" + DS1 + "\");");

		DisplayData output = kernel.eval("ds := loadCSV(\"" + DS1 + "\");");

		assertThat(text(output))
				.isEqualTo("Dataset 'ds' loaded from '" + DS1 + "' (csv, 2 columns)");
	}

	@Test
	void rerunningLoadCellWithPersistentAssignment() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");

		DisplayData output = kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");

		assertThat(text(output))
				.isEqualTo("Dataset 'taxi' loaded from '" + DS1 + "' (csv, 2 columns)");
	}

	@Test
	void rerunningScalarAssignment() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");
		kernel.eval("nRows := getSize(taxi);");

		DisplayData output = kernel.eval("nRows := getSize(taxi);");

		assertThat(text(output)).isEqualTo("nRows calculated");
	}

	@Test
	void rerunningDisplayCellPreservesVariablesFromOtherCells() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");
		kernel.eval("other := getSize(taxi);");
		kernel.eval("m := showMetadata(taxi);");

		DisplayData output = kernel.eval("m := showMetadata(taxi);");

		assertThat(text(output)).isEqualTo("m calculated");
		// other and taxi still available for use in a follow-up cell
		DisplayData followUp = kernel.eval("d := show(taxi); size := other;");
		assertThat(text(followUp)).isEqualTo("d calculated\n" + "size calculated");
	}

	@Test
	void rerunningShowCellAfterWriteCellDoesNotShuffleMessages() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");
		kernel.eval("w := writeParquet(\"output\", taxi);");
		kernel.eval("m := showMetadata(taxi); d := show(taxi);");

		DisplayData output = kernel.eval("m := showMetadata(taxi); d := show(taxi);");

		assertThat(text(output)).isEqualTo("m calculated\n" + "d calculated");
	}

	@Test
	void pureVtlAssignmentsShowCalculatedMessages() throws Exception {
		kernel.eval("taxi <- loadCSV(\"" + DS1 + "\");");

		DisplayData output =
				kernel.eval("taxi_meta := taxi [keep name]; " + "n := 1 + 2;");

		assertThat(text(output)).isEqualTo("taxi_meta calculated\n" + "n calculated");
	}

	@Test
	void mixedWriteAndShowInSameCellKeepsMessageOrder() throws Exception {
		DisplayData output =
				kernel.eval(
						"taxi <- loadCSV(\""
								+ DS1
								+ "\"); "
								+ "w := writeParquet(\"output\", taxi); "
								+ "m := showMetadata(taxi); "
								+ "d := show(taxi);");

		assertThat(text(output))
				.isEqualTo(
						"Dataset 'taxi' loaded from '"
								+ DS1
								+ "' (csv, 2 columns)\n"
								+ "Dataset 'w' written to 'output' (parquet)\n"
								+ "m calculated\n"
								+ "d calculated");
	}

	private static String text(DisplayData displayData) {
		return (String) displayData.getData(io.github.spencerpark.jupyter.kernel.display.mime.MIMEType.TEXT_PLAIN);
	}
}
