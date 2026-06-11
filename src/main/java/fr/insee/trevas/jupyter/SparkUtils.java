/* (C)2024 */
package fr.insee.trevas.jupyter;

import fr.insee.vtl.engine.VtlScriptEngine;
import fr.insee.vtl.spark.SparkDataset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

public class SparkUtils {

	public static VtlScriptEngine buildSparkEngine(SparkSession spark) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByExtension("vtl");
		engine.put("$vtl.engine.processing_engine_names", "spark");
		engine.put("$vtl.spark.session", spark);
		return (VtlScriptEngine) engine;
	}

	public static SparkSession buildSparkSession() {
		SparkSession.Builder sparkBuilder = SparkSession.builder();
		SparkConf conf = new SparkConf(true);
		String sparkHome = System.getenv("SPARK_HOME");
		if (sparkHome == null) {
			sparkBuilder.master("local").appName("trevas").config("spark.ui.enabled", "false");
		} else {
			Path path = Path.of(sparkHome, "conf", "spark-defaults.conf");
			if (Files.exists(path)) {
				org.apache.spark.util.Utils.loadDefaultSparkProperties(
						conf, path.normalize().toAbsolutePath().toString());
				if (conf.get("spark.jars", "").isEmpty()) {
					conf.set(
							"spark.jars",
							String.join(
									",",
									"/vtl-spark.jar",
									"/vtl-model.jar",
									"/vtl-parser.jar",
									"/vtl-engine.jar"));
				}
			} else {
				sparkBuilder.master("local");
			}
		}
		configureCloudFilesystem(conf);
		applyS3Environment(conf, System.getenv());
		SparkSession spark = sparkBuilder.config(conf).getOrCreate();
		applyS3Environment(spark.sparkContext().hadoopConfiguration(), System.getenv());
		spark.sparkContext().setLogLevel("WARN");
		return spark;
	}

	private static void configureCloudFilesystem(SparkConf conf) {
		conf.setIfMissing("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
		conf.setIfMissing("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
	}

	/**
	 * Maps standard {@code AWS_*} environment variables to Hadoop S3A settings when present.
	 *
	 * <p>No-op when variables are absent (local standalone with filesystem paths only). Used on
	 * Onyxia and for local development against S3/MinIO when credentials are exported in the shell.
	 */
	static void applyS3Environment(SparkConf conf, Map<String, String> env) {
		if (!hasS3Environment(env)) {
			return;
		}
		setSparkHadoopFromEnv(conf, env, "fs.s3a.access.key", "AWS_ACCESS_KEY_ID");
		setSparkHadoopFromEnv(conf, env, "fs.s3a.secret.key", "AWS_SECRET_ACCESS_KEY");
		setSparkHadoopFromEnv(conf, env, "fs.s3a.session.token", "AWS_SESSION_TOKEN");
		setSparkHadoopFromEnv(conf, env, "fs.s3a.endpoint.region", "AWS_DEFAULT_REGION");
		applyS3EndpointToSparkConf(conf, env.get("AWS_S3_ENDPOINT"));
	}

	static void applyS3Environment(org.apache.hadoop.conf.Configuration hconf, Map<String, String> env) {
		if (!hasS3Environment(env)) {
			return;
		}
		setHadoopFromEnv(hconf, env, "fs.s3a.access.key", "AWS_ACCESS_KEY_ID");
		setHadoopFromEnv(hconf, env, "fs.s3a.secret.key", "AWS_SECRET_ACCESS_KEY");
		setHadoopFromEnv(hconf, env, "fs.s3a.session.token", "AWS_SESSION_TOKEN");
		setHadoopFromEnv(hconf, env, "fs.s3a.endpoint.region", "AWS_DEFAULT_REGION");
		applyS3EndpointToHadoopConf(hconf, env.get("AWS_S3_ENDPOINT"));
	}

	static boolean hasS3Environment(Map<String, String> env) {
		return isSet(env, "AWS_S3_ENDPOINT")
				|| isSet(env, "AWS_ACCESS_KEY_ID")
				|| isSet(env, "AWS_SECRET_ACCESS_KEY")
				|| isSet(env, "AWS_SESSION_TOKEN");
	}

	private static void applyS3EndpointToSparkConf(SparkConf conf, String endpoint) {
		if (!isSet(endpoint)) {
			return;
		}
		conf.set("spark.hadoop.fs.s3a.endpoint", endpoint);
		if (isThirdPartyS3Endpoint(endpoint)) {
			conf.set("spark.hadoop.fs.s3a.path.style.access", "true");
		}
		if (endpoint.startsWith("http://")) {
			conf.set("spark.hadoop.fs.s3a.connection.ssl.enabled", "false");
		}
	}

	private static void applyS3EndpointToHadoopConf(
			org.apache.hadoop.conf.Configuration hconf, String endpoint) {
		if (!isSet(endpoint)) {
			return;
		}
		hconf.set("fs.s3a.endpoint", endpoint);
		if (isThirdPartyS3Endpoint(endpoint)) {
			hconf.set("fs.s3a.path.style.access", "true");
		}
		if (endpoint.startsWith("http://")) {
			hconf.set("fs.s3a.connection.ssl.enabled", "false");
		}
	}

	private static boolean isThirdPartyS3Endpoint(String endpoint) {
		return !endpoint.contains("amazonaws.com");
	}

	private static boolean isSet(Map<String, String> env, String key) {
		return isSet(env.get(key));
	}

	private static boolean isSet(String value) {
		return value != null && !value.isBlank();
	}

	private static void setSparkHadoopFromEnv(
			SparkConf conf, Map<String, String> env, String hadoopKey, String envKey) {
		String value = env.get(envKey);
		if (value != null && !value.isBlank()) {
			conf.set("spark.hadoop." + hadoopKey, value);
		}
	}

	private static void setHadoopFromEnv(
			org.apache.hadoop.conf.Configuration hconf,
			Map<String, String> env,
			String hadoopKey,
			String envKey) {
		String value = env.get(envKey);
		if (value != null && !value.isBlank()) {
			hconf.set(hadoopKey, value);
		}
	}

	public static SparkDataset readParquetDataset(SparkSession spark, String path)
			throws Exception {
		Dataset<Row> dataset;
		try {
			dataset = spark.read().parquet(Utils.resolveDataLocation(path));
		} catch (Exception e) {
			throw new Exception("Bad format", e);
		}
		return new SparkDataset(dataset);
	}

	public static SparkDataset readSasDataset(SparkSession spark, String path) throws Exception {
		Dataset<Row> dataset;
		try {
			dataset =
					spark.read()
							.format("com.github.saurfang.sas.spark")
							.load(Utils.resolveDataLocation(path));
		} catch (Exception e) {
			throw new Exception(e);
		}
		return new SparkDataset(dataset);
	}

	public static SparkDataset readCSVDataset(SparkSession spark, String path) throws Exception {
		Dataset<Row> dataset;
		var uri = Utils.uri(path);
		var params = new Utils.QueryParam(uri);
		try {
			dataset =
					spark.read()
							.option("delimiter", params.getValue("delimiter").orElse(";"))
							.option("quote", params.getValue("quote").orElse("\""))
							.option("header", params.getValue("header").orElse("true"))
							.options(params.flatten())
							.csv(Utils.resolveDataLocation(uri));
		} catch (Exception e) {
			throw new Exception(e);
		}
		return new SparkDataset(dataset);
	}

	public static void writeParquetDataset(String location, SparkDataset dataset) {
		org.apache.spark.sql.Dataset<Row> sparkDataset = dataset.getSparkDataset();
		sparkDataset.write().mode(SaveMode.Overwrite).parquet(Utils.resolveDataLocation(location));
	}

	public static void writeCSVDataset(String location, SparkDataset dataset) {
		org.apache.spark.sql.Dataset<Row> sparkDataset = dataset.getSparkDataset();
		var uri = Utils.uri(location);
		var params = new Utils.QueryParam(uri);
		sparkDataset
				.write()
				.option("delimiter", params.getValue("delimiter").orElse(";"))
				.option("quote", params.getValue("quote").orElse("\""))
				.option("header", params.getValue("header").orElse("true"))
				.options(params.flatten())
				.mode(SaveMode.Overwrite)
				.csv(Utils.resolveDataLocation(uri));
	}
}
