import { linter, type Diagnostic } from '@codemirror/lint';
import type { Extension, Text } from '@codemirror/state';

import { extraLintDelayMs } from './vtl-editor-state';
import { collectVtlLintErrors } from './vtl-lint-collect';
import { VTL_ERROR_MARK_CLASS } from './vtl-lint-theme';
import { isVtlEditorActive } from './vtl-mime';
import type { VtlSyntaxError } from './vtl-parser';

function toDiagnostic(doc: Text, error: VtlSyntaxError): Diagnostic {
	const startLine = doc.line(Math.min(error.startLine, doc.lines));
	const endLine = doc.line(Math.min(error.endLine, doc.lines));
	const from = Math.min(startLine.from + error.startCol, startLine.to);
	const to = Math.max(
		Math.min(endLine.from + error.endCol, endLine.to),
		from + 1,
	);

	return {
		from,
		to,
		severity: 'error',
		message: error.message,
		markClass: VTL_ERROR_MARK_CLASS,
	};
}

function wait(ms: number): Promise<void> {
	return new Promise(resolve => {
		setTimeout(resolve, ms);
	});
}

/**
 * CodeMirror linter — debounced ANTLR validation with post-paste deferral.
 */
export function vtlLinter(isActive?: () => boolean): Extension {
	return linter(
		async view => {
			if (isActive && !isActive()) {
				return [];
			}

			const lineCount = view.state.doc.lines;
			const extra = extraLintDelayMs(lineCount);
			if (extra > 0) {
				await wait(extra);
			}

			const text = view.state.doc.toString();
			if (!text.trim()) {
				return [];
			}

			const errors = await collectVtlLintErrors(text, lineCount);

			if (view.state.doc.toString() !== text) {
				return [];
			}

			return errors.map(error => toDiagnostic(view.state.doc, error));
		},
		{
			delay: 400,
			needsRefresh: update => update.docChanged,
		},
	);
}

export function vtlLinterForModel(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): Extension {
	return vtlLinter(() => isVtlEditorActive(model));
}
