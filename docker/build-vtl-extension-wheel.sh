#!/usr/bin/env bash
# Build the prebuilt jupyterlab-vtl-2-1 wheel (JS bundle + Python packaging).
# Used by CI before docker build and for local Docker smoke tests.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXT_DIR="${ROOT_DIR}/extensions/jupyterlab-vtl-2.1"
PYTHON="${PYTHON:-python3}"

cd "${EXT_DIR}"

ensure_pip() {
	if "${PYTHON}" -m pip --version >/dev/null 2>&1; then
		return 0
	fi
	echo "pip missing in ${PYTHON}; bootstrapping..." >&2
	if command -v uv >/dev/null 2>&1; then
		uv pip install --python "${PYTHON}" pip
	elif "${PYTHON}" -m ensurepip --upgrade >/dev/null 2>&1; then
		:
	elif command -v pip3 >/dev/null 2>&1; then
		# Last resort: install into the active interpreter via pip3.
		pip3 install pip
	else
		echo "error: no pip for ${PYTHON} (try: uv pip install pip, or python3 -m ensurepip)" >&2
		exit 1
	fi
	if ! "${PYTHON}" -m pip --version >/dev/null 2>&1; then
		echo "error: could not enable pip for ${PYTHON}" >&2
		exit 1
	fi
}

pip_install() {
	ensure_pip
	"${PYTHON}" -m pip install "$@"
}

pip_wheel() {
	ensure_pip
	"${PYTHON}" -m pip wheel "$@"
}

ensure_node22() {
	if command -v node >/dev/null 2>&1; then
		local major
		major="$(node -p "process.versions.node.split('.')[0]")"
		if [[ "${major}" == "22" ]]; then
			return 0
		fi
	fi

	# nvm is bash-only; pick an installed Node 22 from ~/.nvm when PATH has another major.
	local nvm_root="${NVM_DIR:-${HOME}/.nvm}"
	local candidate
	candidate="$(ls -d "${nvm_root}/versions/node"/v22.* 2>/dev/null | sort -V | tail -1 || true)"
	if [[ -n "${candidate}" && -x "${candidate}/bin/node" ]]; then
		export PATH="${candidate}/bin:${PATH}"
		echo "Using Node from ${candidate}" >&2
	fi

	if ! command -v node >/dev/null 2>&1; then
		echo "error: Node.js 22.x is required (not found on PATH)" >&2
		exit 1
	fi

	local major
	major="$(node -p "process.versions.node.split('.')[0]")"
	if [[ "${major}" != "22" ]]; then
		echo "error: Node.js 22.x is required (got $(node --version))" >&2
		echo "hint: export PATH=\"\${HOME}/.nvm/versions/node/v22.22.2/bin:\${PATH}\"" >&2
		exit 1
	fi
}

ensure_node22

pip_install --upgrade pip
pip_install \
	"jupyterlab>=4.4.0,<5" \
	build \
	hatchling \
	hatch-jupyter-builder \
	hatch-nodejs-version

if ! command -v jlpm >/dev/null 2>&1; then
	JLPM_BIN="$("${PYTHON}" -c 'import sysconfig; print(sysconfig.get_path("scripts"))')"
	export PATH="${JLPM_BIN}:${PATH}"
fi

if ! command -v jlpm >/dev/null 2>&1; then
	echo "error: jlpm not found after installing JupyterLab" >&2
	exit 1
fi

rm -rf dist
jlpm install --immutable
if [[ "${SKIP_VTL_TESTS:-0}" != "1" ]]; then
	jlpm test
else
	echo "Skipping jlpm test (SKIP_VTL_TESTS=1)" >&2
fi
jlpm run build:lib
jlpm run build:labextension
pip_wheel . -w dist --no-deps

WHEEL=(dist/jupyterlab_vtl_2_1-*.whl)
if [[ ! -f "${WHEEL[0]}" ]]; then
	echo "error: wheel was not produced under ${EXT_DIR}/dist" >&2
	exit 1
fi

# install.json must be packaged (Dockerfile verifies it after pip install).
if ! python3 -m zipfile -l "${WHEEL[0]}" | grep -q 'labextensions/jupyterlab-vtl-2-1/install.json'; then
	echo "error: wheel is missing install.json — commit extensions/jupyterlab-vtl-2.1/install.json" >&2
	exit 1
fi

echo "Built ${WHEEL[0]}"
