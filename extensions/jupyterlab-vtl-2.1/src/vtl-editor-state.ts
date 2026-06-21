/** Shared editor timing flags (paste burst, completion cooldown). */

let lastPasteAt = 0;
let completionSuppressedUntil = 0;
let lintDeferredUntil = 0;

const MIN_COMPLETION_SUPPRESS_MS = 2000;
const COMPLETION_SUPPRESS_PER_LINE_MS = 100;
const MIN_LINT_DEFER_MS = 1200;
const LINT_DEFER_PER_LINE_MS = 60;

export function markPaste(text: string): void {
	const lines = Math.max(1, text.split('\n').length);
	const now = Date.now();
	lastPasteAt = now;

	const suppressMs = Math.max(
		MIN_COMPLETION_SUPPRESS_MS,
		500 + lines * COMPLETION_SUPPRESS_PER_LINE_MS,
	);
	completionSuppressedUntil = now + suppressMs;

	const lintDeferMs = Math.max(
		MIN_LINT_DEFER_MS,
		400 + lines * LINT_DEFER_PER_LINE_MS,
	);
	lintDeferredUntil = now + lintDeferMs;
}

export function msSincePaste(): number {
	return Date.now() - lastPasteAt;
}

export function isCompletionSuppressed(): boolean {
	return Date.now() < completionSuppressedUntil;
}

/** True while the editor should stay in post-paste quiet mode. */
export function isPasteBurst(): boolean {
	return isCompletionSuppressed();
}

/** Extra lint wait for large documents (includes paste deferral). */
export function extraLintDelayMs(lineCount: number): number {
	let extra = Math.max(0, lintDeferredUntil - Date.now());
	if (lineCount > 60) {
		extra = Math.max(extra, 800);
	} else if (lineCount > 30) {
		extra = Math.max(extra, 400);
	}
	return extra;
}
