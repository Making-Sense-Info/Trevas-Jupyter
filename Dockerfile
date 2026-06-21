# Onyxia Spark image (linux/amd64). For Apple Silicon local builds use:
#   docker buildx build --platform linux/amd64 ...
#
# Trevas JVM kernel is built in the kernel-build stage (patched basekernel, no host mvn required).

FROM eclipse-temurin:17-jdk AS kernel-build
WORKDIR /build
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
	&& rm -rf /var/lib/apt/lists/*
COPY . .
RUN chmod +x docker/build-jvm-basekernel-compat.sh mvnw \
	&& if [ ! -f target/appassembler/bin/trevas ]; then \
		./docker/build-jvm-basekernel-compat.sh \
		&& ./mvnw package -DskipTests -q; \
	else \
		echo "Reusing target/appassembler from build context"; \
	fi \
	&& rm -rf target/appassembler/repo/io/github/spencerpark/jupyter-jvm-basekernel/2.3.0

FROM inseefrlab/onyxia-jupyter-pyspark:py3.13.8-spark3.5.7-2025.11.03

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX="/opt/hadoop/etc/hadoop:/opt/spark/conf"
ENV PATH="/opt/python/bin:${PATH}"

COPY --from=kernel-build /build/target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel.json /usr/local/share/jupyter/kernels/trevas/

COPY --from=kernel-build /build/target/appassembler/repo/fr/insee/trevas/vtl-spark/*/vtl-spark-*.jar /vtl-spark.jar
COPY --from=kernel-build /build/target/appassembler/repo/fr/insee/trevas/vtl-model/*/vtl-model-*.jar /vtl-model.jar
COPY --from=kernel-build /build/target/appassembler/repo/fr/insee/trevas/vtl-engine/*/vtl-engine-*.jar /vtl-engine.jar
COPY --from=kernel-build /build/target/appassembler/repo/fr/insee/trevas/vtl-parser/*/vtl-parser-*.jar /vtl-parser.jar

USER root

# JupyterLab extensions — Elyra (pipelines) + Arbalister (Parquet/CSV viewer).
ARG JUPYTERLAB_VERSION=4.5.8
ARG ELYRA_VERSION=4.1.1
ARG ARBALISTER_VERSION=0.2.1
COPY docker/install-jupyter-extensions.sh /opt/install-jupyter-extensions.sh
RUN chmod +x /opt/install-jupyter-extensions.sh && \
	JUPYTERLAB_VERSION="${JUPYTERLAB_VERSION}" \
	ELYRA_VERSION="${ELYRA_VERSION}" \
	ARBALISTER_VERSION="${ARBALISTER_VERSION}" \
	/opt/install-jupyter-extensions.sh

# VTL 2.1 JupyterLab editor (highlighting, ANTLR lint, autocomplete).
# Wheel is built in CI / locally via docker/build-vtl-extension-wheel.sh before image build.
COPY extensions/jupyterlab-vtl-2.1/dist/jupyterlab_vtl_2_1-*.whl /tmp/
RUN /opt/python/bin/pip install --no-cache-dir /tmp/jupyterlab_vtl_2_1-*.whl && \
	rm -f /tmp/jupyterlab_vtl_2_1-*.whl && \
	test -f /opt/python/share/jupyter/labextensions/jupyterlab-vtl-2-1/package.json && \
	test -f /opt/python/share/jupyter/labextensions/jupyterlab-vtl-2-1/install.json

# Add your entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh

# Make the entrypoint script executable
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set the entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

USER 1000

CMD ["/opt/python/bin/jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
