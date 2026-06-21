try:
    from ._version import __version__
except ImportError:
    __version__ = "0.0.0"


def _jupyter_labextension_paths():
    return [{
        "src": "labextension",
        "dest": "jupyterlab-vtl-2-1"
    }]
