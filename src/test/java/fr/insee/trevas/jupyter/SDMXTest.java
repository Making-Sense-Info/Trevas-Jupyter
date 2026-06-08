/* (C)2024 */
package fr.insee.trevas.jupyter;

import fr.insee.vtl.model.Dataset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SDMXTest {

	@BeforeAll
	public void setup() throws Exception {
		new VtlKernel();
	}

	@Test
	public void testLoadSDMXSource() {
		Dataset ds =
				VtlKernel.loadSDMXEmptySource(
						"src/test/resources/sdmx/DSD_BPE_CENSUS.xml", "BPE_DETAIL_VTL");
		assertThat(ds.getDataStructure().size()).isEqualTo(6);
	}

	@Test
	public void testLoadSDMXSourceWithData() throws Exception {
		Dataset ds =
				VtlKernel.loadSDMXSource(
						"src/test/resources/sdmx/DSD_BPE_CENSUS.xml",
						"BPE_DETAIL_VTL",
						"src/test/resources/sdmx/BPE_DETAIL_SAMPLE.csv");
		assertThat(ds.getDataStructure().size()).isEqualTo(6);
	}

	@Test
	public void testRunSDMXPreview() {
		VtlKernel.runSDMXPreview("src/test/resources/sdmx/DSD_BPE_CENSUS.xml");
	}

	@Test
	public void testRunSDMX() {
		VtlKernel.runSDMX("src/test/resources/sdmx/DSD_BPE_CENSUS.xml",
				"BPE_DETAIL_VTL,src/test/resources/sdmx/BPE_DETAIL_SAMPLE.csv," +
						"LEGAL_POP,src/test/resources/sdmx/LEGAL_POP_NUTS3.csv");
	}

	@Test
	public void testGetTransformationsVTL() {
		VtlKernel.getTransformationsVTL("src/test/resources/sdmx/DSD_BPE_CENSUS.xml");
	}

	@Test
	public void testGetRulesetsVTL() {
		VtlKernel.getRulesetsVTL("src/test/resources/sdmx/DSD_BPE_CENSUS.xml");
	}
}
