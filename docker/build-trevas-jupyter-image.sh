#!/usr/bin/env bash
# Build the VTL extension wheel (host / CI) then the Trevas-Jupyter Docker image.
# Node 22 + jlpm run on the host — never inside the image (QEMU / webpack issues on Mac).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_TAG="${IMAGE_TAG:-trevas-jupyter:local}"
DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}"

cd "${ROOT_DIR}"

chmod +x docker/build-vtl-extension-wheel.sh
./docker/build-vtl-extension-wheel.sh

WHEEL=(extensions/jupyterlab-vtl-2.1/dist/jupyterlab_vtl_2_1-*.whl)
if [[ ! -f "${WHEEL[0]}" ]]; then
	echo "error: VTL wheel missing after build: ${WHEEL[0]}" >&2
	exit 1
fi
echo "Using wheel: ${WHEEL[0]}"

exec docker buildx build \
	--platform "${DOCKER_PLATFORM}" \
	-t "${IMAGE_TAG}" \
	--load \
	"$@" \
	.
