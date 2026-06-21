#!/bin/bash
# Install the prebuilt jupyterlab-vtl-2-1 wheel and verify labextension files.
set -euo pipefail

WHEEL="${1:?usage: install-vtl-labextension.sh /path/to/wheel.whl}"

# Dockerfile may pass a glob; expand to the single matching file.
if [[ "${WHEEL}" == *"*"* ]]; then
	_matches=( ${WHEEL} )
	if [[ ${#_matches[@]} -ne 1 ]]; then
		echo "ERROR: expected exactly one VTL wheel, got: ${_matches[*]:-<none>}" >&2
		exit 1
	fi
	WHEEL="${_matches[0]}"
fi

export PATH="/opt/python/bin:${PATH}"
LABEXT_ROOT="/opt/python/share/jupyter/labextensions"
VTL_EXT="${LABEXT_ROOT}/jupyterlab-vtl-2-1"

pip_install() {
	if command -v uv >/dev/null 2>&1; then
		uv pip install --system --no-cache-dir "$@"
	else
		python3 -m pip install --no-cache-dir "$@"
	fi
}

if [ ! -f "${WHEEL}" ]; then
	echo "ERROR: VTL extension wheel not found: ${WHEEL}" >&2
	exit 1
fi

pip_install "${WHEEL}"
rm -f "${WHEEL}"

for f in package.json install.json; do
	if [ ! -f "${VTL_EXT}/${f}" ]; then
		echo "ERROR: missing jupyterlab-vtl-2-1 labextension file ${VTL_EXT}/${f}" >&2
		ls -la "${LABEXT_ROOT}" >&2 || true
		exit 1
	fi
done

echo "jupyterlab-vtl-2-1 installed and verified under ${VTL_EXT}"
