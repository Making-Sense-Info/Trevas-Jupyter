FROM --platform=linux/amd64 inseefrlab/onyxia-jupyter-pyspark:py3.11.6-spark3.5.0

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX "/opt/hadoop/etc/hadoop:/opt/spark/conf"

COPY target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel-unix.json /usr/local/share/jupyter/kernels/trevas/kernel.json

COPY target/appassembler/repo/vtl-spark-*.jar /vtl-spark.jar
COPY target/appassembler/repo/vtl-model-*.jar /vtl-model.jar
COPY target/appassembler/repo/vtl-engine-*.jar /vtl-engine.jar
COPY target/appassembler/repo/vtl-parser-*.jar /vtl-parser.jar

USER root

RUN pip3 install --upgrade elyra-pipeline-editor-extension

USER 1000

CMD ["jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
