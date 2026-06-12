#!/bin/bash
# Installs a coherent Elyra 4.x stack for JupyterLab 4.x (launcher deduplication requires
# python-editor alongside pipeline-editor + theme).
set -euo pipefail

ELYRA_VERSION="${1:-4.1.1}"

# Onyxia images ship Python under /opt/python (see images-datascience).
export PATH="/opt/python/bin:${PATH}"

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

# labextension list may exit non-zero while still printing valid output (e.g. lockedExtensions migration).
set +e
jupyter labextension list >/tmp/labextensions.txt 2>&1
set -e

required_extensions=(
	"@elyra/pipeline-editor-extension"
	"@elyra/python-editor-extension"
	"@elyra/theme-extension"
)

for ext in "${required_extensions[@]}"; do
	if ! grep -F "${ext}" /tmp/labextensions.txt | grep -q "enabled OK"; then
		echo "ERROR: ${ext} is missing or not enabled" >&2
		cat /tmp/labextensions.txt >&2
		exit 1
	fi
done

if grep -F "@elyra/code-snippet-extension" /tmp/labextensions.txt | grep -q "enabled OK"; then
	echo "Elyra ${ELYRA_VERSION} installed (pipeline, python editor, code snippets, theme)"
else
	echo "WARNING: @elyra/code-snippet-extension not enabled; core Elyra stack is OK" >&2
	echo "Elyra ${ELYRA_VERSION} installed (pipeline, python editor, theme)"
fi
