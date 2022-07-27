package fr.insee.trevas.jupyter;

import fr.insee.vtl.engine.VtlScriptEngine;
import fr.insee.vtl.model.Dataset;
import fr.insee.vtl.model.Structured;
import fr.insee.vtl.spark.SparkDataset;
import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import org.apache.spark.sql.SparkSession;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

public class VtlKernel extends BaseKernel {

    private static final DisplayData displayData = new DisplayData();
    private static SparkSession spark;
    private final VtlScriptEngine engine;
    private final LanguageInfo info;

    public VtlKernel() throws Exception {
        spark = buildSparkSession();
        this.engine = buildSparkEngine(spark);
        System.out.println("Loaded VTL engine " + engine.getFactory().getEngineVersion());
        ScriptEngineFactory factory = engine.getFactory();
        this.info = new LanguageInfo.Builder(factory.getEngineName())
                .version(factory.getEngineVersion())
                .build();
        registerMethods();
    }

    public static SparkDataset loadS3(String path) throws Exception {
        return Utils.readParquetDataset(spark, path);
    }

    public static String writeS3(String path, Dataset ds) {
        // TODO: replace with SparkDataset constructor when available in Trevas
        if (ds instanceof SparkDataset) {
            Utils.writeParquetDataset(spark, path, (SparkDataset) ds);
            return "Dataset writes to " + path;
        } else {
            return "Dataset is not a SparkDataset";
        }
    }

    public static Object show(Object o) {
        if (o instanceof Dataset) {
            SparkDataset ds = (SparkDataset) o;
            // TODO: build "beautiful" html output for each varID (Structure + X lines)
            StringBuilder sb = new StringBuilder();
            Structured.DataStructure dataStructure = ds.getDataStructure();
            dataStructure.forEach((key, value) -> sb.append(key).append("\n"));
            displayData.putText("Columns: \n" + sb);
        } else {
            displayData.putText(o.toString());
        }
        return o;
    }

    public static Object showMetadata(Object o) {
        if (o instanceof Dataset) {
            SparkDataset ds = (SparkDataset) o;
            // TODO: build "beautiful" html output for each varID (Structure + X lines)
            StringBuilder sb = new StringBuilder();
            Structured.DataStructure dataStructure = ds.getDataStructure();
            dataStructure.forEach((key, value) -> sb.append(key).append("\n"));
            displayData.putText("Columns: \n" + sb);
        } else {
            displayData.putText(o.toString());
        }
        return o;
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 1)
            throw new IllegalArgumentException("Missing connection file argument");

        Path connectionFile = Paths.get(args[0]);

        if (!Files.isRegularFile(connectionFile))
            throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");

        String contents = new String(Files.readAllBytes(connectionFile));

        JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);

        KernelConnectionProperties connProps = KernelConnectionProperties.parse(contents);
        JupyterConnection connection = new JupyterConnection(connProps);


        VtlKernel kernel = new VtlKernel();

        kernel.becomeHandlerForConnection(connection);

        connection.connect();
        connection.waitUntilClose();
    }

    private void registerMethods() throws NoSuchMethodException {
        // TODO: insert printMetadata
        // TODO: insert writeS3
        this.engine.registerMethod("loadS3", VtlKernel.class.getMethod("loadS3", String.class));
        this.engine.registerMethod("writeS3", VtlKernel.class.getMethod("writeS3", String.class));
        this.engine.registerMethod("show", VtlKernel.class.getMethod("show", Object.class));
        this.engine.registerMethod("showMetadata", VtlKernel.class.getMethod("showMetadata", Object.class));
    }

    public VtlScriptEngine buildSparkEngine(SparkSession spark) throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByExtension("vtl");
        engine.put("$vtl.engine.processing_engine_names", "spark");
        engine.put("$vtl.spark.session", spark);
        return (VtlScriptEngine) engine;
    }

    private SparkSession buildSparkSession() {
        SparkSession.Builder sparkBuilder = SparkSession.builder()
                .appName("trevas-jupyter")
                .master("local");
        return sparkBuilder.getOrCreate();
    }

    @Override
    public synchronized DisplayData eval(String expr) throws Exception {
        this.engine.eval(expr);
        return new DisplayData(displayData);
    }

    @Override
    public LanguageInfo getLanguageInfo() {
        return this.info;
    }
}
