/* (C)2024 */
package fr.insee.trevas.jupyter;

import org.apache.spark.SparkConf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SparkUtilsS3ConfigTest {

	@Test
	void appliesOnyxiaS3EnvironmentToSparkConf() {
		SparkConf conf = new SparkConf();
		SparkUtils.applyS3Environment(
				conf,
				Map.of(
						"AWS_ACCESS_KEY_ID", "key",
						"AWS_SECRET_ACCESS_KEY", "secret",
						"AWS_SESSION_TOKEN", "token",
						"AWS_DEFAULT_REGION", "us-east-1",
						"AWS_S3_ENDPOINT", "https://minio.example.com"));

		assertThat(conf.get("spark.hadoop.fs.s3a.access.key")).isEqualTo("key");
		assertThat(conf.get("spark.hadoop.fs.s3a.secret.key")).isEqualTo("secret");
		assertThat(conf.get("spark.hadoop.fs.s3a.session.token")).isEqualTo("token");
		assertThat(conf.get("spark.hadoop.fs.s3a.endpoint.region")).isEqualTo("us-east-1");
		assertThat(conf.get("spark.hadoop.fs.s3a.endpoint")).isEqualTo("https://minio.example.com");
		assertThat(conf.get("spark.hadoop.fs.s3a.path.style.access")).isEqualTo("true");
	}

	@Test
	void doesNothingWhenS3EnvironmentIsAbsent() {
		SparkConf conf = new SparkConf();
		SparkUtils.applyS3Environment(conf, Map.of());

		assertThat(conf.get("spark.hadoop.fs.s3a.endpoint", null)).isNull();
		assertThat(conf.get("spark.hadoop.fs.s3a.access.key", null)).isNull();
	}

	@Test
	void appliesCredentialsWithoutCustomEndpointForRealAws() {
		SparkConf conf = new SparkConf();
		SparkUtils.applyS3Environment(
				conf,
				Map.of(
						"AWS_ACCESS_KEY_ID", "key",
						"AWS_SECRET_ACCESS_KEY", "secret",
						"AWS_DEFAULT_REGION", "eu-west-1"));

		assertThat(conf.get("spark.hadoop.fs.s3a.access.key")).isEqualTo("key");
		assertThat(conf.get("spark.hadoop.fs.s3a.endpoint", null)).isNull();
		assertThat(conf.get("spark.hadoop.fs.s3a.path.style.access", null)).isNull();
	}

	@Test
	void doesNotForcePathStyleForAwsEndpoint() {
		SparkConf conf = new SparkConf();
		SparkUtils.applyS3Environment(
				conf, Map.of("AWS_S3_ENDPOINT", "https://s3.eu-west-1.amazonaws.com"));

		assertThat(conf.get("spark.hadoop.fs.s3a.path.style.access", null)).isNull();
	}

	@Test
	void disablesSslForPlainHttpEndpoint() {
		SparkConf conf = new SparkConf();
		SparkUtils.applyS3Environment(
				conf, Map.of("AWS_S3_ENDPOINT", "http://minio.example.com:9000"));

		assertThat(conf.get("spark.hadoop.fs.s3a.connection.ssl.enabled")).isEqualTo("false");
	}
}
