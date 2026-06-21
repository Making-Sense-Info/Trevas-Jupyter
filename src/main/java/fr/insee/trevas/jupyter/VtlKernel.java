/* (C)2024 */
package fr.insee.trevas.jupyter;

import fr.insee.vtl.engine.VtlScriptEngine;
import fr.insee.vtl.model.Dataset;
import fr.insee.vtl.model.InMemoryDataset;
import fr.insee.vtl.model.PersistentDataset;
import fr.insee.vtl.model.Structured;
import fr.insee.vtl.sdmx.SDMXVTLWorkflow;
import fr.insee.vtl.sdmx.TrevasSDMXUtils;
import fr.insee.vtl.spark.SparkDataset;
import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;
import io.sdmx.api.io.ReadableDataLocation;
import io.sdmx.utils.core.io.ReadableDataLocationTmp;
import org.apache.spark.sql.SparkSession;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class VtlKernel extends BaseKernel {

	private static final String VTL_MIME = "text/x-vtl";

	private static DisplayData displayData = new DisplayData();
	private static SparkSession spark;
	private static VtlScriptEngine engine;
	private static boolean globalMethodsRegistered;
	private final LanguageInfo info;

	public VtlKernel() {
		this.info =
				new LanguageInfo.Builder("VTL")
						.version("2.3")
						.mimetype(VTL_MIME)
						.fileExtension(".vtl")
						.codemirror("vtl")
						.build();
	}

	static void bootstrapForTests() throws Exception {
		ensureEngineReady();
	}

	private static synchronized void ensureEngineReady() throws Exception {
		if (engine != null) {
			return;
		}
		spark = SparkUtils.buildSparkSession();
		engine = SparkUtils.buildSparkEngine(spark);
		if (!globalMethodsRegistered) {
			registerGlobalMethods();
			globalMethodsRegistered = true;
		}
	}

	private static Map<String, Dataset.Role> getRoleMap(
			Collection<Structured.Component> components) {
		return components.stream()
				.collect(
						Collectors.toMap(
								Structured.Component::getName, Structured.Component::getRole));
	}

	private static Map<String, Dataset.Role> getRoleMap(fr.insee.vtl.model.Dataset dataset) {
		return getRoleMap(dataset.getDataStructure().values());
	}

	private static SparkDataset asSparkDataset(Dataset dataset) {
		if (dataset instanceof SparkDataset) {
			return (SparkDataset) dataset;
		}
		if (dataset instanceof PersistentDataset) {
			fr.insee.vtl.model.Dataset ds = ((PersistentDataset) dataset).getDelegate();
			if (ds instanceof SparkDataset) {
				return (SparkDataset) ds;
			} else {
				return new SparkDataset(ds, getRoleMap(dataset), spark);
			}
		}
		throw new IllegalArgumentException("Unknow dataset type");
	}

	public static SparkDataset loadParquet(String path) throws Exception {
		ensureEngineReady();
		SparkDataset dataset = SparkUtils.readParquetDataset(spark, path);
		notifyLoad("loadParquet", dataset, "parquet", path);
		return dataset;
	}

	public static SparkDataset loadCSV(String path) throws Exception {
		ensureEngineReady();
		SparkDataset dataset = SparkUtils.readCSVDataset(spark, path);
		notifyLoad("loadCSV", dataset, "csv", path);
		return dataset;
	}

	public static SparkDataset loadSas(String path) throws Exception {
		ensureEngineReady();
		SparkDataset dataset = SparkUtils.readSasDataset(spark, path);
		notifyLoad("loadSas", dataset, "sas", path);
		return dataset;
	}

	public static String writeParquet(String path, Dataset ds) throws Exception {
		ensureEngineReady();
		SparkUtils.writeParquetDataset(path, asSparkDataset(ds));
		notifyWrite("writeParquet", "parquet", path);
		return "Dataset written to '" + path + "' (parquet)";
	}

	public static String writeCSV(String path, Dataset ds) throws Exception {
		ensureEngineReady();
		SparkUtils.writeCSVDataset(path, asSparkDataset(ds));
		notifyWrite("writeCSV", "csv", path);
		return "Dataset written to '" + path + "' (csv)";
	}

	public static long getSize(Dataset ds) throws Exception {
		ensureEngineReady();
		SparkDataset sparkDataset = asSparkDataset(ds);
		long rowCount = sparkDataset.getDataPoints().size();
		LoadAssignmentContext.pollFor("getSize")
				.ifPresentOrElse(
						var -> notify(LoadAssignmentContext.formatCalculatedMessage(var)),
						() -> notify("Dataset size: " + rowCount));
		return rowCount;
	}

	public static Object show(Object o) throws Exception {
		ensureEngineReady();
		if (o instanceof Dataset dataset) {
			showDataset(asSparkDataset(dataset));
		} else {
			displayData.putText(o.toString());
		}
		notifyCalculated("show");
		return o;
	}

	private static void showDataset(SparkDataset dataset) throws ClassNotFoundException {
		displayData.putHTML(DatasetUtils.datasetToDisplay(dataset));
	}

	public static Object showMetadata(Object o) throws Exception {
		ensureEngineReady();
		if (o instanceof Dataset) {
			displayData.putHTML(DatasetUtils.datasetMetadataToDisplay((Dataset) o));
		} else {
			displayData.putText(o.toString());
		}
		notifyCalculated("showMetadata");
		return o;
	}

	public static Dataset loadSDMXEmptySource(String path, String id) {
		Structured.DataStructure structure = TrevasSDMXUtils.buildStructureFromSDMX3(path, id);
		Dataset dataset = new InMemoryDataset(List.of(List.of()), structure);
		notifySdmxLoad("loadSDMXEmptySource", dataset, path, "empty SDMX source '" + id + "'");
		return dataset;
	}

	public static Dataset loadSDMXSource(String path, String id, String dataPath) throws Exception {
		ensureEngineReady();
		Structured.DataStructure structure = TrevasSDMXUtils.buildStructureFromSDMX3(path, id);
		Dataset dataset =
				new SparkDataset(
						spark.read()
								.option("header", "true")
								.option("delimiter", ";")
								.option("quote", "\"")
								.csv(dataPath),
						structure);
		notifySdmxLoad(
				"loadSDMXSource",
				dataset,
				dataPath,
				"SDMX source '" + id + "', structure from '" + path + "'");
		return dataset;
	}

	private static void clearWorkflowBindings() {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.keySet().stream()
				.filter(key -> !key.startsWith("$"))
				.toList()
				.forEach(bindings::remove);
	}

	public static void runSDMXPreview(String path) throws Exception {
		ensureEngineReady();
		ReadableDataLocation rdl = new ReadableDataLocationTmp(path);

		SDMXVTLWorkflow sdmxVtlWorkflow = new SDMXVTLWorkflow(engine, rdl, Map.of());

		clearWorkflowBindings();
		Map<String, Dataset> emptyDatasets = sdmxVtlWorkflow.getMappedEmptyDatasets();
		engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(emptyDatasets);

		Map<String, PersistentDataset> results = sdmxVtlWorkflow.run();

		notify("SDMX preview completed (" + results.size() + " dataset(s))");

		var result = new StringBuilder();

		results.forEach(
				(k, v) -> {
					result.append("<h2>")
							.append(k)
							.append("</h2>")
							.append(DatasetUtils.datasetMetadataToDisplay(v));
				});

		displayData.putHTML(result.toString());
	}

	public static void runSDMX(String path, String data) throws Exception {
		ensureEngineReady();
		String[] dataList = data.split(",");
		if (dataList.length % 2 != 0) {
			throw new IllegalArgumentException("Data params length invalid: " + dataList.length);
		}
		Map<String, String> dataBindings = new HashMap<>();
		for (int i = 0; i < dataList.length; i = i + 2) {
			dataBindings.put(dataList[i].trim(), dataList[i + 1].trim());
		}
		Map<String, Dataset> inputs =
				dataBindings.entrySet().stream()
						.collect(
								Collectors.toMap(
										Map.Entry::getKey,
										e -> {
											Structured.DataStructure structure =
													TrevasSDMXUtils.buildStructureFromSDMX3(
															path, e.getKey());
											return new SparkDataset(
													spark.read()
															.option("header", "true")
															.option("delimiter", ";")
															.option("quote", "\"")
															.csv(e.getValue()),
													structure);
										}));

		ReadableDataLocation rdl = new ReadableDataLocationTmp(path);
		SDMXVTLWorkflow sdmxVtlWorkflow = new SDMXVTLWorkflow(engine, rdl, inputs);
		clearWorkflowBindings();
		Map<String, PersistentDataset> results = sdmxVtlWorkflow.run();

		notify("SDMX workflow completed (" + results.size() + " dataset(s))");

		var result = new StringBuilder();

		results.forEach(
				(k, v) -> {
					try {
						result.append("<h2>")
								.append(k)
								.append("</h2>")
								.append(DatasetUtils.datasetToDisplay(v));
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				});

		displayData.putHTML(result.toString());
	}

	public static void getTransformationsVTL(String path) throws Exception {
		ensureEngineReady();
		ReadableDataLocation rdl = new ReadableDataLocationTmp(path);
		SDMXVTLWorkflow sdmxVtlWorkflow = new SDMXVTLWorkflow(engine, rdl, Map.of());
		String vtl = sdmxVtlWorkflow.getTransformationsVTL();

		displayData.putText(vtl);
	}

	public static void getRulesetsVTL(String path) throws Exception {
		ensureEngineReady();
		ReadableDataLocation rdl = new ReadableDataLocationTmp(path);
		SDMXVTLWorkflow sdmxVtlWorkflow = new SDMXVTLWorkflow(engine, rdl, Map.of());
		String dprs = sdmxVtlWorkflow.getRulesetsVTL();

		displayData.putText(dprs);
	}

	public static void main(String[] args) throws Exception {
		configureQuietLogging();

		if (args.length < 1) throw new IllegalArgumentException("Missing connection file argument");

		Path connectionFile = Paths.get(args[0]);

		if (!Files.isRegularFile(connectionFile))
			throw new IllegalArgumentException(
					"Connection file '" + connectionFile + "' isn't a file.");

		String contents = new String(Files.readAllBytes(connectionFile));

		KernelConnectionProperties connProps = KernelConnectionProperties.parse(contents);
		JupyterConnection connection = new JupyterConnection(connProps);

		VtlKernel kernel = new VtlKernel();
		kernel.becomeHandlerForConnection(connection);
		connection.connect();

		Thread sparkInit =
				new Thread(
						() -> {
							try {
								ensureEngineReady();
							} catch (Exception e) {
								throw new RuntimeException("Failed to initialize Spark/Trevas engine", e);
							}
						},
						"trevas-spark-init");
		sparkInit.setDaemon(true);
		sparkInit.start();

		connection.waitUntilClose();
	}

	private static void notify(String message) {
		if (displayData.hasDataForType(MIMEType.TEXT_PLAIN)) {
			String existing = (String) displayData.getData(MIMEType.TEXT_PLAIN);
			displayData.putText(existing + "\n" + message);
		} else {
			displayData.putText(message);
		}
	}

	private static void notifyLoad(
			String operation, Dataset dataset, String format, String location) {
		String variable = LoadAssignmentContext.pollFor(operation).orElse(null);
		notify(
				LoadAssignmentContext.formatLoadMessage(
						variable, location, format, dataset.getDataStructure().size()));
	}

	private static void notifyWrite(String operation, String format, String location) {
		String variable = LoadAssignmentContext.pollFor(operation).orElse(null);
		notify(LoadAssignmentContext.formatWriteMessage(variable, location, format));
	}

	private static void notifyCalculated(String operation) {
		LoadAssignmentContext.pollFor(operation)
				.ifPresent(var -> notify(LoadAssignmentContext.formatCalculatedMessage(var)));
	}

	private static void notifySdmxLoad(
			String operation, Dataset dataset, String location, String details) {
		String variable = LoadAssignmentContext.pollFor(operation).orElse(null);
		notify(
				LoadAssignmentContext.formatSdmxLoadMessage(
						variable, location, details, dataset.getDataStructure().size()));
	}

	/**
	 * Emits "{@code var calculated}" for top-level VTL assignments that did not already produce a
	 * kernel feedback message (load, show, write, etc.).
	 */
	private static void notifyTopLevelAssignments(String expression) {
		var kernelFeedbackVariables =
				VtlAssignmentTargets.feedbackIn(expression).stream()
						.map(VtlAssignmentTargets.FeedbackTarget::variable)
						.collect(Collectors.toSet());
		VtlAssignmentTargets.topLevelIn(expression).stream()
				.filter(variable -> !kernelFeedbackVariables.contains(variable))
				.map(LoadAssignmentContext::formatCalculatedMessage)
				.forEach(VtlKernel::notify);
	}

	private static void configureQuietLogging() {
		JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);
		System.setProperty("log.level", "WARN");
		java.util.logging.Logger.getLogger("org.apache").setLevel(Level.WARNING);
		java.util.logging.Logger.getLogger("org.sparkproject").setLevel(Level.WARNING);
	}

	private static void registerGlobalMethods() throws NoSuchMethodException {
		engine.registerGlobalMethod(
				"loadParquet", VtlKernel.class.getMethod("loadParquet", String.class));
		engine.registerGlobalMethod(
				"loadCSV", VtlKernel.class.getMethod("loadCSV", String.class));
		engine.registerGlobalMethod(
				"loadSas", VtlKernel.class.getMethod("loadSas", String.class));
		engine.registerGlobalMethod(
				"writeParquet",
				VtlKernel.class.getMethod("writeParquet", String.class, Dataset.class));
		engine.registerGlobalMethod(
				"writeCSV", VtlKernel.class.getMethod("writeCSV", String.class, Dataset.class));
		engine.registerGlobalMethod("show", VtlKernel.class.getMethod("show", Object.class));
		engine.registerGlobalMethod(
				"showMetadata", VtlKernel.class.getMethod("showMetadata", Object.class));
		engine.registerGlobalMethod(
				"size", VtlKernel.class.getMethod("getSize", Dataset.class));
		engine.registerGlobalMethod(
				"getSize", VtlKernel.class.getMethod("getSize", Dataset.class));

		// SDMX
		engine.registerGlobalMethod(
				"loadSDMXEmptySource",
				VtlKernel.class.getMethod("loadSDMXEmptySource", String.class, String.class));
		engine.registerGlobalMethod(
				"loadSDMXSource",
				VtlKernel.class.getMethod(
						"loadSDMXSource", String.class, String.class, String.class));
		engine.registerGlobalMethod(
				"runSDMXPreview", VtlKernel.class.getMethod("runSDMXPreview", String.class));
		engine.registerGlobalMethod(
				"runSDMX", VtlKernel.class.getMethod("runSDMX", String.class, String.class)
		);
		engine.registerGlobalMethod(
				"getTransformationsVTL",
				VtlKernel.class.getMethod("getTransformationsVTL", String.class));
		engine.registerGlobalMethod(
				"getRulesetsVTL", VtlKernel.class.getMethod("getRulesetsVTL", String.class));
	}

	@Override
	public synchronized DisplayData eval(String expr) throws Exception {
		ensureEngineReady();
		displayData = new DisplayData();
		try {
			VtlAssignmentTargets.clearFrom(engine.getBindings(ScriptContext.ENGINE_SCOPE), expr);
			LoadAssignmentContext.prepare(expr);
			engine.eval(expr);
			notifyTopLevelAssignments(expr);
			return displayData;
		} finally {
			LoadAssignmentContext.clear();
		}
	}

	@Override
	public LanguageInfo getLanguageInfo() {
		return this.info;
	}
}
