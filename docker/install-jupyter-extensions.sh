#!/bin/bash
# JupyterLab extensions baked into the Trevas-Jupyter image:
# - Elyra 4.x (pipelines, Python editor, code snippets)
# - Arbalister (Parquet/CSV/Arrow tabular viewer — double-click in the file browser)
set -euo pipefail

ELYRA_VERSION="${ELYRA_VERSION:-4.1.1}"
ARBALISTER_VERSION="${ARBALISTER_VERSION:-0.2.1}"
JUPYTERLAB_VERSION="${JUPYTERLAB_VERSION:-4.5.8}"

# Onyxia images ship Python under /opt/python (see images-datascience).
export PATH="/opt/python/bin:${PATH}"
export UV_HTTP_TIMEOUT="${UV_HTTP_TIMEOUT:-300}"

LABEXT_ROOT="/opt/python/share/jupyter/labextensions"
SERVER_CONFIG_ROOT="/opt/python/etc/jupyter/jupyter_server_config.d"

pip_install() {
	if command -v uv >/dev/null 2>&1; then
		uv pip install --system --no-cache-dir --upgrade "$@"
	else
		python3 -m pip install --no-cache-dir --upgrade "$@"
	fi
}

# Arbalister relies on IContentProvider (contentProviderRegistry), added in JupyterLab 4.5.
# Onyxia base images ship 4.4.x; without this upgrade, .parquet opens in the text editor
# and Jupyter reports "is not UTF-8 encoded".
pip_install "jupyterlab==${JUPYTERLAB_VERSION}"
python3 - <<'PY'
import jupyterlab

major, minor = (int(x) for x in jupyterlab.__version__.split(".")[:2])
if (major, minor) < (4, 5):
	raise SystemExit(
		f"JupyterLab {jupyterlab.__version__} is too old for Arbalister (need >= 4.5.0)"
	)
print(f"JupyterLab {jupyterlab.__version__} OK for Arbalister")
PY

# --- Elyra (launcher deduplication requires python-editor alongside pipeline-editor) ---
elyra_packages=(
	"elyra-pipeline-editor-extension==${ELYRA_VERSION}"
	"elyra-python-editor-extension==${ELYRA_VERSION}"
	"elyra-code-snippet-extension==${ELYRA_VERSION}"
)
pip_install "${elyra_packages[@]}"

elyra_extensions=(
	"@elyra/pipeline-editor-extension"
	"@elyra/python-editor-extension"
	"@elyra/theme-extension"
)
for ext in "${elyra_extensions[@]}"; do
	if [ ! -f "${LABEXT_ROOT}/${ext}/package.json" ]; then
		echo "ERROR: missing Elyra labextension ${ext} under ${LABEXT_ROOT}" >&2
		ls -la "${LABEXT_ROOT}" >&2 || true
		exit 1
	fi
done
echo "Elyra ${ELYRA_VERSION} installed and verified"

# --- Arbalister (QuantStack) — view Parquet/CSV/Avro/ORC via double-click ---
pip_install "arbalister==${ARBALISTER_VERSION}"

if [ ! -f "${LABEXT_ROOT}/arbalister/package.json" ]; then
	echo "ERROR: missing Arbalister labextension under ${LABEXT_ROOT}/arbalister" >&2
	ls -la "${LABEXT_ROOT}" >&2 || true
	exit 1
fi
if [ ! -f "${SERVER_CONFIG_ROOT}/arbalister.json" ]; then
	echo "ERROR: missing Arbalister server config under ${SERVER_CONFIG_ROOT}" >&2
	ls -la "${SERVER_CONFIG_ROOT}" >&2 || true
	exit 1
fi
echo "Arbalister ${ARBALISTER_VERSION} installed and verified"
