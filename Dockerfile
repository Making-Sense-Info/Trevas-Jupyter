FROM inseefrlab/onyxia-jupyter-pyspark:py3.10.9-spark3.3.1

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX "/opt/hadoop/etc/hadoop:/opt/spark/conf"

COPY target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel-unix.json /usr/local/share/jupyter/kernels/trevas/kernel.json

COPY target/appassembler/repo/vtl-spark-*.jar /vtl-spark.jar
COPY target/appassembler/repo/vtl-model-*.jar /vtl-model.jar
COPY target/appassembler/repo/vtl-engine-*.jar /vtl-engine.jar
COPY target/appassembler/repo/vtl-parser-*.jar /vtl-parser.jar

RUN mamba install -y -c conda-forge "elyra[all]"

CMD ["jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
