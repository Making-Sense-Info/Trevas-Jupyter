#!/bin/bash
# Installs a coherent Elyra 4.x stack for JupyterLab 4.x (launcher deduplication requires
# python-editor alongside pipeline-editor + theme).
set -euo pipefail

ELYRA_VERSION="${1:-4.1.1}"

# Onyxia images ship Python under /opt/python (see images-datascience).
export PATH="/opt/python/bin:${PATH}"
export UV_HTTP_TIMEOUT="${UV_HTTP_TIMEOUT:-300}"

LABEXT_ROOT="/opt/python/share/jupyter/labextensions"

PACKAGES=(
	"elyra-pipeline-editor-extension==${ELYRA_VERSION}"
	"elyra-python-editor-extension==${ELYRA_VERSION}"
	"elyra-code-snippet-extension==${ELYRA_VERSION}"
)

if command -v uv >/dev/null 2>&1; then
	uv pip install --system --no-cache-dir --upgrade "${PACKAGES[@]}"
else
	python3 -m pip install --no-cache-dir --upgrade "${PACKAGES[@]}"
fi

required_extensions=(
	"@elyra/pipeline-editor-extension"
	"@elyra/python-editor-extension"
	"@elyra/theme-extension"
)

for ext in "${required_extensions[@]}"; do
	if [ ! -f "${LABEXT_ROOT}/${ext}/package.json" ]; then
		echo "ERROR: missing labextension ${ext} under ${LABEXT_ROOT}" >&2
		ls -la "${LABEXT_ROOT}" >&2 || true
		exit 1
	fi
done

echo "Elyra ${ELYRA_VERSION} installed and verified"
