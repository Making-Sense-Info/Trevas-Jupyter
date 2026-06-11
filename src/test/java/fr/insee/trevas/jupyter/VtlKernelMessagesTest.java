/* (C)2024 */
package fr.insee.trevas.jupyter;

import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VtlKernelMessagesTest {

	private VtlKernel kernel;

	@BeforeAll
	void setup() throws Exception {
		kernel = new VtlKernel();
	}

	@Test
	void loadCsvWithAssignmentShowsVariableName() throws Exception {
		DisplayData output =
				kernel.eval("ds := loadCSV(\"src/test/resources/ds1.csv\");");

		assertThat(text(output))
				.isEqualTo("Dataset 'ds' loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)");
	}

	@Test
	void multipleLoadsShowOneMessagePerLine() throws Exception {
		DisplayData output =
				kernel.eval(
						"a := loadCSV(\"src/test/resources/ds1.csv\"); "
								+ "b := loadCSV(\"src/test/resources/ds2.csv?delimiter=%7C&quote=%27\");");

		assertThat(text(output))
				.isEqualTo(
						"Dataset 'a' loaded from 'src/test/resources/ds1.csv' (csv, 2 columns)\n"
								+ "Dataset 'b' loaded from"
								+ " 'src/test/resources/ds2.csv?delimiter=%7C&quote=%27' (csv, 2"
								+ " columns)");
	}

	@Test
	void writeCsvShowsConfirmation(@TempDir Path tempDir) throws Exception {
		String outputPath = tempDir.resolve("out.csv").toString().replace("\\", "/");
		DisplayData output =
				kernel.eval(
						"writeDs := loadCSV(\"src/test/resources/ds1.csv\"); writeResult :="
								+ " writeCSV(\""
								+ outputPath
								+ "\", writeDs);");

		assertThat(text(output))
				.isEqualTo(
						"Dataset 'writeDs' loaded from 'src/test/resources/ds1.csv' (csv, 2"
								+ " columns)\n"
								+ "Dataset written to '"
								+ outputPath
								+ "' (csv)");
		assertThat(Files.exists(tempDir.resolve("out.csv"))).isTrue();
	}

	@Test
	void getSizeShowsConfirmation() throws Exception {
		DisplayData output =
				kernel.eval(
						"countDs <- loadCSV(\"src/test/resources/ds1.csv\"); nRows :="
								+ " getSize(countDs);");

		assertThat(text(output))
				.isEqualTo(
						"Dataset 'countDs' loaded from 'src/test/resources/ds1.csv' (csv, 2"
								+ " columns)\n"
								+ "Dataset size: 5");
	}

	private static String text(DisplayData displayData) {
		return (String) displayData.getData(MIMEType.TEXT_PLAIN);
	}
}
