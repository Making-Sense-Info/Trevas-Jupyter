# Onyxia Spark image (linux/amd64). For Apple Silicon local builds use:
#   docker buildx build --platform linux/amd64 ...
FROM inseefrlab/onyxia-jupyter-pyspark:py3.13.8-spark3.5.7-2025.11.03

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX="/opt/hadoop/etc/hadoop:/opt/spark/conf"

COPY target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel.json /usr/local/share/jupyter/kernels/trevas/

COPY target/appassembler/repo/fr/insee/trevas/vtl-spark/*/vtl-spark-*.jar /vtl-spark.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-model/*/vtl-model-*.jar /vtl-model.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-engine/*/vtl-engine-*.jar /vtl-engine.jar
COPY target/appassembler/repo/fr/insee/trevas/vtl-parser/*/vtl-parser-*.jar /vtl-parser.jar

USER root

# JupyterLab extensions — Elyra (pipelines) + Arbalister (Parquet/CSV viewer).
# Arbalister needs JupyterLab >= 4.5 (Onyxia base is 4.4.x); upgraded in install script.
ARG JUPYTERLAB_VERSION=4.5.8
ARG ELYRA_VERSION=4.1.1
ARG ARBALISTER_VERSION=0.2.1
COPY docker/install-jupyter-extensions.sh /opt/install-jupyter-extensions.sh
RUN chmod +x /opt/install-jupyter-extensions.sh && \
	JUPYTERLAB_VERSION="${JUPYTERLAB_VERSION}" \
	ELYRA_VERSION="${ELYRA_VERSION}" \
	ARBALISTER_VERSION="${ARBALISTER_VERSION}" \
	/opt/install-jupyter-extensions.sh

# Add your entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh

# Make the entrypoint script executable
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

USER 1000

CMD ["jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
