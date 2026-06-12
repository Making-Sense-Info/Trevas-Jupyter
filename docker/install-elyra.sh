#!/bin/bash
# Installs a coherent Elyra 4.x stack for JupyterLab 4.x (launcher deduplication requires
# python-editor alongside pipeline-editor + theme).
set -euo pipefail

ELYRA_VERSION="${1:-4.1.1}"

pip3 install --no-cache-dir --upgrade \
	"elyra-pipeline-editor-extension==${ELYRA_VERSION}" \
	"elyra-python-editor-extension==${ELYRA_VERSION}" \
	"elyra-code-snippet-extension==${ELYRA_VERSION}"

jupyter labextension list >/tmp/labextensions.txt 2>&1

required_extensions=(
	"@elyra/pipeline-editor-extension"
	"@elyra/python-editor-extension"
	"@elyra/theme-extension"
	"@elyra/code-snippet-extension"
)

for ext in "${required_extensions[@]}"; do
	if ! grep -F "${ext}" /tmp/labextensions.txt | grep -q "enabled OK"; then
		echo "ERROR: ${ext} is missing or not enabled" >&2
		cat /tmp/labextensions.txt >&2
		exit 1
	fi
done

echo "Elyra ${ELYRA_VERSION} installed and verified"
