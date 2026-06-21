# jupyterlab-vtl-2.1

VTL 2.1 syntax highlighting for JupyterLab notebook cells (Trevas kernel).

Lives under `Trevas-Jupyter/extensions/` for now; can be extracted to its own repository later.

## Phase 1 (current)

- CodeMirror 6 `vtl` language registered for kernel `language: vtl`
- Keywords, operators (`:=`, `<-`), strings, numbers, comments

## Phase 2

- Client-side ANTLR syntax validation (`@making-sense/vtl-2-1-antlr-tools-ts`)
- Red squiggles in the editor with ANTLR error messages

## Phase 3

- Autocomplete: VTL keywords, Trevas Jupyter functions, local variable names
- Snippets for `define dataset` and `calc`

## Development

Requires Node.js 22.x, Python 3.10+, and JupyterLab 4.5+.

Node.js 24 currently breaks the `jupyter labextension build` step (`license-webpack-plugin` crash),
so this project is pinned to Node 22 on purpose. Webpack is also pinned to `5.106.2` in
`package.json` (`resolutions`) because webpack `5.107+` triggers the same plugin crash.

### Recommended setup (uv)

```bash
cd extensions/jupyterlab-vtl-2.1

# Python env + deps
uv venv
uv pip install "jupyterlab>=4.5,<5" hatch-jupyter-builder build
uv pip install -e .

# JS toolchain — activate the venv so `jlpm` and `jupyter` are on PATH
nvm use 22
source .venv/bin/activate          # bash/zsh
# source .venv/bin/activate.fish   # fish

jlpm install
jlpm build
jupyter labextension develop jupyterlab_vtl_2_1 --overwrite
jupyter lab
```

If you prefer not to activate the venv, prepend it to `PATH` instead:

```bash
export PATH="$PWD/.venv/bin:$PATH"   # bash/zsh
# set -gx PATH $PWD/.venv/bin $PATH  # fish
nvm use 22
jlpm build
```

Notes:
- `jlpm` is installed by JupyterLab inside `.venv/bin`. If the command is missing, run
  `uv pip install "jupyterlab>=4.5,<5"` first.
- Running `.venv/bin/jlpm build` without activating the venv often fails with
  `command not found: jlpm` or `command not found: jupyter`, because yarn scripts need
  both tools on `PATH`.
- Use `jlpm` (not global `yarn` v1) to avoid lockfile/tooling mismatch.
- `jupyter labextension develop` needs the Python module name (`jupyterlab_vtl_2_1`), not `.`.

Watch mode while editing TypeScript:

```bash
jlpm watch
```

## Production install (later)

```bash
pip install jupyterlab-vtl-2-1
```

Or from this directory:

```bash
pip install .
```

## Verify

```bash
jupyter labextension list | grep vtl
```

Open a Trevas notebook — code cells should show VTL syntax colors instead of plain text.

### Test validation and autocomplete

In a `.vtl` file or Trevas notebook cell:

- **Validation**: introduce a syntax error (e.g. remove a semicolon) — a red underline appears after a short delay; hover for the ANTLR message.
- **Autocomplete**: type `lo` and accept `loadCSV` / `loadParquet`; type `def` for the `define dataset` snippet; `Ctrl+Space` forces suggestions.

**Important:** after pulling or rebuilding the extension, hard-refresh the browser:

```bash
source .venv/bin/activate.fish   # or activate
nvm use 22
jlpm build
jupyter labextension develop jupyterlab_vtl_2_1 --overwrite
jupyter lab
```

In the browser: `Cmd+Shift+R` (hard refresh). Console should log `[jupyterlab-vtl-2-1] v0.1.0 active`. To double-check:

```bash
jupyter labextension list | grep vtl
# should show jupyterlab-vtl-2-1 v0.1.0 enabled OK
```

Close and reopen `.vtl` files created before the fix (they may still be bound to `text/plain`).

The `:=` suggestion alone is JupyterLab bracket completion, not VTL autocomplete — you should see keyword/function lists when typing `lo`, `def`, `calc`, etc.

## Runtime vs build-time note

Node.js is required only to build/develop the frontend extension.
At runtime (already built extension installed in the image), Node is not required.

## Docker image (Trevas-Jupyter)

The extension wheel is baked into the main `Trevas-Jupyter/Dockerfile` (CI builds it before `docker build`).

From the repository root:

```bash
./mvnw package -DskipTests
chmod +x docker/build-vtl-extension-wheel.sh
./docker/build-vtl-extension-wheel.sh
docker buildx build --platform linux/amd64 -t trevas-jupyter:local --load .
docker run --rm -p 8888:8888 trevas-jupyter:local
```

See the root `README.md` for full local test instructions.
