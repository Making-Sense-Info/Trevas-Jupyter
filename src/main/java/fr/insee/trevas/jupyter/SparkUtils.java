/* (C)2024 */
package fr.insee.trevas.jupyter;

import fr.insee.vtl.engine.VtlScriptEngine;
import fr.insee.vtl.spark.SparkDataset;
import java.nio.file.Files;
import java.nio.file.Path;
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
		SparkSession spark = sparkBuilder.config(conf).getOrCreate();
		spark.sparkContext().setLogLevel("WARN");
		return spark;
	}

	private static void configureCloudFilesystem(SparkConf conf) {
		conf.setIfMissing("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
		conf.setIfMissing("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
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
