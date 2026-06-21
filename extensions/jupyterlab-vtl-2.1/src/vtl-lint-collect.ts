import { validateVtl, type VtlSyntaxError } from './vtl-parser';

/** Notebook cells stay well below this; skip pathological pastes. */
const MAX_LINT_LINES = 500;

/** Run ANTLR during idle time once cells get large enough to matter. */
const IDLE_LINT_MIN_LINES = 120;

/**
 * Run ANTLR validation for CodeMirror lint.
 * Small cells: synchronous parse. Larger cells: idle callback to keep typing smooth.
 */
export function collectVtlLintErrors(
	text: string,
	lineCount: number,
): Promise<VtlSyntaxError[]> {
	if (!text.trim() || lineCount > MAX_LINT_LINES) {
		return Promise.resolve([]);
	}

	return new Promise(resolve => {
		const run = (): void => {
			try {
				resolve(validateVtl(text));
			} catch (err) {
				console.warn('[jupyterlab-vtl-2-1] validateVtl failed', err);
				resolve([]);
			}
		};

		if (
			lineCount >= IDLE_LINT_MIN_LINES &&
			typeof requestIdleCallback === 'function'
		) {
			requestIdleCallback(run, { timeout: 2000 });
		} else {
			run();
		}
	});
}
