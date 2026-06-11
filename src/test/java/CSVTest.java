/* (C)2024 */
import static org.assertj.core.api.Assertions.assertThat;

import fr.insee.trevas.jupyter.SparkUtils;
import fr.insee.trevas.jupyter.Utils;
import fr.insee.vtl.model.Dataset;
import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class CSVTest {
	@Test
	void testURI() {
		URI uri;

		uri = Utils.uri("src/test/resources/ds1.csv");
		URI expected = Path.of("src/test/resources/ds1.csv").toUri();
		assertThat(uri).isEqualTo(expected);

		uri = Utils.uri("src/test/resources/ds1.csv#fragment");
		assertThat(uri.getHost()).isEqualTo(expected.getHost());
		assertThat(uri.getAuthority()).isEqualTo(expected.getAuthority());
		assertThat(uri.getFragment()).isEqualTo("fragment");

		uri = Utils.uri("src/test/resources/ds1.csv?foo=bar&foo=baz");
		assertThat(uri.getHost()).isEqualTo(expected.getHost());
		assertThat(uri.getAuthority()).isEqualTo(expected.getAuthority());
		assertThat(uri.getQuery()).isEqualTo("foo=bar&foo=baz");

		uri = Utils.uri("src/test/resources/ds1.csv?sep=%7C&del=%3B");
		assertThat(uri.getHost()).isEqualTo(expected.getHost());
		assertThat(uri.getAuthority()).isEqualTo(expected.getAuthority());
		assertThat(uri.getQuery()).isEqualTo("sep=|&del=;");
	}

	@Test
	void resolveDataLocationKeepsRemoteSchemes() {
		assertThat(Utils.resolveDataLocation("s3://my-bucket/path/file.csv"))
				.isEqualTo("s3a://my-bucket/path/file.csv");
		assertThat(Utils.resolveDataLocation("s3://l4tu7k/public/nyc_taxi.sas7bdat"))
				.isEqualTo("s3a://l4tu7k/public/nyc_taxi.sas7bdat");
		assertThat(Utils.resolveDataLocation("s3a://my-bucket/path/file.csv?delimiter=%2C"))
				.isEqualTo("s3a://my-bucket/path/file.csv");
		assertThat(Utils.resolveDataLocation("https://example.com/data/file.csv"))
				.isEqualTo("https://example.com/data/file.csv");
	}

	@Test
	void resolveDataLocationNormalizesLocalPaths() {
		Path local = Path.of("src/test/resources/ds1.csv").toAbsolutePath().normalize();
		assertThat(Utils.resolveDataLocation("src/test/resources/ds1.csv")).isEqualTo(local.toString());
	}

	@Test
	public void readCSVDatasetTest() throws Exception {
		var spark = SparkUtils.buildSparkSession();
		Dataset ds1 = SparkUtils.readCSVDataset(spark, "src/test/resources/ds1.csv");
		assertThat(ds1.getDataPoints().get(1).get("name")).isEqualTo("B");
		Dataset ds2 =
				SparkUtils.readCSVDataset(
						spark, "src/test/resources/ds2.csv?delimiter=%7C&quote=%27");
		assertThat(ds2.getDataPoints().get(1).get("name")).isEqualTo("G");
	}
}
