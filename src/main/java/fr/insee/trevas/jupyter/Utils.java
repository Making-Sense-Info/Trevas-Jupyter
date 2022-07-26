package fr.insee.trevas.jupyter;

import fr.insee.vtl.model.Structured;
import fr.insee.vtl.spark.SparkDataset;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import javax.script.*;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static ScriptEngine buildSparkEngine() throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByExtension("vtl");
        SparkSession.Builder sparkBuilder = SparkSession.builder()
                .appName("trevas-jupyter")
                .master("local");
        SparkSession spark = sparkBuilder.getOrCreate();
        ScriptContext context = engine.getContext();
        Bindings bindings = new SimpleBindings();
        SparkDataset ds = readParquetDataset(spark, "s3a://projet-vtl/fideli/small");
        bindings.put("ds", ds);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.put("$vtl.engine.processing_engine_names", "spark");
        engine.put("$vtl.spark.session", spark);
        return engine;
    }

    public static SparkDataset readParquetDataset(SparkSession spark, String path) throws Exception {
        Dataset<Row> dataset;
        Dataset<Row> json;
        try {
            dataset = spark.read().parquet(path + "/data");
            json = spark.read()
                    .option("multiLine", "true")
                    .json(path + "/structure");
        } catch (Exception e) {
            throw new Exception("An error has occured while loading: " + path);
        }
        Map<String, fr.insee.vtl.model.Dataset.Role> components = json.collectAsList().stream().map(r -> {
                            String name = r.getAs("name");
                            Class type = r.getAs("type").getClass();
                            fr.insee.vtl.model.Dataset.Role role = fr.insee.vtl.model.Dataset.Role.valueOf(r.getAs("role"));
                            return new Structured.Component(name, type, role);
                        }
                ).collect(Collectors.toList())
                .stream()
                .collect(Collectors.toMap(Structured.Component::getName, Structured.Component::getRole));
        return new SparkDataset(dataset, components);
    }
}