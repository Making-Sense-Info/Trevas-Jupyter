FROM --platform=linux/amd64 inseefrlab/onyxia-jupyter-pyspark:py3.13.8-spark3.5.7-2025.11.03

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX "/opt/hadoop/etc/hadoop:/opt/spark/conf"

COPY target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel.json /usr/local/share/jupyter/kernels/trevas/

COPY target/appassembler/repo/fr/insee/trevas/vtl-spark/*/vtl-spark-*.jar /vtl-spark.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-model/*/vtl-model-*.jar /vtl-model.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-engine/*/vtl-engine-*.jar /vtl-engine.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-parser/*/vtl-parser-*.jar /vtl-parser.jar

USER root

# Elyra 4.x for JupyterLab 4.x — pin versions together; python-editor is required so the
# Elyra launcher does not duplicate every tile (pipeline-editor alone is not enough).
ARG ELYRA_VERSION=4.1.1
COPY docker/install-elyra.sh /opt/install-elyra.sh
RUN chmod +x /opt/install-elyra.sh && /opt/install-elyra.sh "${ELYRA_VERSION}"

# Add your entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh

# Make the entrypoint script executable
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

USER 1000

CMD ["jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
