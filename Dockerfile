# Python & R spark support
FROM jupyter/all-spark-notebook:latest

# Allows the kernel to load the Spark and Hadoop config.
ENV CLASSPATH_PREFIX "/opt/hadoop/etc/hadoop:/opt/spark/conf"

COPY target/appassembler/ /usr/local/share/jupyter/kernels/trevas/
COPY kernel-unix.json /usr/local/share/jupyter/kernels/trevas/kernel.json

COPY target/appassembler/repo/vtl-spark-*.jar /vtl-spark.jar
COPY target/appassembler/repo/vtl-model-*.jar /vtl-model.jar
COPY target/appassembler/repo/vtl-engine-*.jar /vtl-engine.jar
COPY target/appassembler/repo/vtl-parser-*.jar /vtl-parser.jar

USER root

# Install OpenJDK-13
ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN apt-get update && \
    apt-get install -y openjdk-11-jre-headless && \
    apt-get clean;

# Install Elyra

ENV MAMBA_DIR="/opt/mamba"
ENV PATH="${MAMBA_DIR}/bin:${PATH}"

COPY ./docker-config/conda-env.yml .

RUN wget -q https://github.com/conda-forge/miniforge/releases/latest/download/Mambaforge-Linux-x86_64.sh -O mambaforge.sh && \
    # Install mambaforge latest version
    /bin/bash mambaforge.sh -b -p "${MAMBA_DIR}" && \
    # Activate custom Conda env by default in shell
    echo ". ${MAMBA_DIR}/etc/profile.d/conda.sh && conda activate" >> ${HOME}/.bashrc && \
    # Fix permissions
    chown -R ${USERNAME}:${GROUPNAME} ${HOME} ${MAMBA_DIR} && \
    # Clean
    rm mambaforge.sh conda-env.yml && \ 
    mamba clean --all -f -y && \
    rm -rf /var/lib/apt/lists/*

RUN mamba install -y -c conda-forge elyra-pipeline-editor-extension

USER 1000

CMD ["jupyter", "lab", "--no-browser", "--ip", "0.0.0.0"]
