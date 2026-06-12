# Multi-platform build for Trevas-Jupyter.
# Onyxia base image is linux/amd64; build arm64 only if the base tag exists for your arch.
#
# Examples:
#   docker buildx bake --load
#   docker buildx bake --push
#   docker buildx bake --set "*.platform=linux/amd64" --load

group "default" {
  targets = ["trevas-jupyter"]
}

variable "REGISTRY" {
  default = "makingsenseinfo"
}

variable "TAG" {
  default = "latest"
}

variable "ELYRA_VERSION" {
  default = "4.1.1"
}

target "trevas-jupyter" {
  context = "."
  dockerfile = "Dockerfile"
  platforms = ["linux/amd64"]
  tags = ["${REGISTRY}/trevas-jupyter:${TAG}"]
  args = {
    ELYRA_VERSION = "${ELYRA_VERSION}"
  }
}
