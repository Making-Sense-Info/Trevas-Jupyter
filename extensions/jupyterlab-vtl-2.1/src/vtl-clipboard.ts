/**
 * Plain-text clipboard handlers for VTL cells.
 *
 * Copy/cut: skip DOM serialization (thousands of highlight spans freeze Jupyter).
 * Paste: insert text/plain only (no rich HTML) and mark paste for lint debounce.
 */
import { closeCompletion } from '@codemirror/autocomplete';
import { Prec, type Extension } from '@codemirror/state';
import { EditorView } from '@codemirror/view';

import { markPaste } from './vtl-editor-state';
import { isVtlEditorActive } from './vtl-mime';

function selectedPlainText(view: EditorView): string {
	const { state } = view;
	const parts: string[] = [];
	let linewise = false;

	for (const range of state.selection.ranges) {
		if (!range.empty) {
			parts.push(state.sliceDoc(range.from, range.to));
		}
	}

	if (parts.length === 0) {
		linewise = true;
		let upto = -1;
		for (const { from } of state.selection.ranges) {
			const line = state.doc.lineAt(from);
			if (line.number > upto) {
				parts.push(line.text);
			}
			upto = line.number;
		}
	}

	return parts.join(linewise ? state.lineBreak : '\n');
}

function writeClipboard(event: ClipboardEvent, text: string): boolean {
	const data = event.clipboardData;
	if (!data) {
		return false;
	}
	data.clearData();
	data.setData('text/plain', text);
	return true;
}

function insertPlainText(view: EditorView, text: string): void {
	view.dispatch({
		...view.state.changeByRange(range => ({
			range,
			changes: { from: range.from, to: range.to, insert: text },
		})),
		scrollIntoView: true,
		userEvent: 'input.paste',
	});
}

/** Close autocomplete whenever a paste transaction lands. */
function pasteCompletionGuard(): Extension {
	return EditorView.updateListener.of(update => {
		if (
			update.transactions.some(
				tr => tr.isUserEvent('input.paste') || tr.isUserEvent('paste'),
			)
		) {
			closeCompletion(update.view);
		}
	});
}

/** High-priority copy/cut/paste — plain text only. */
export function vtlPlainTextClipboardForModel(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): Extension[] {
	const isActive = () => isVtlEditorActive(model);

	return [
		pasteCompletionGuard(),
		Prec.highest(
			EditorView.domEventHandlers({
				copy(event, view) {
					if (!isActive()) {
						return false;
					}
					const text = selectedPlainText(view);
					if (!text && view.state.selection.ranges.every(r => r.empty)) {
						return false;
					}
					if (writeClipboard(event, text)) {
						event.preventDefault();
						return true;
					}
					return false;
				},
				cut(event, view) {
					if (!isActive() || view.state.readOnly) {
						return false;
					}
					const text = selectedPlainText(view);
					if (!writeClipboard(event, text)) {
						return false;
					}
					event.preventDefault();
					const changes = view.state.selection.ranges.filter(r => !r.empty);
					if (changes.length) {
						view.dispatch({
							changes,
							scrollIntoView: true,
							userEvent: 'delete.cut',
						});
					}
					return true;
				},
				paste(event, view) {
					if (!isActive() || view.state.readOnly) {
						return false;
					}
					const data = event.clipboardData;
					if (!data) {
						return false;
					}
					const text = data.getData('text/plain');
					if (!text) {
						return false;
					}
					event.preventDefault();
					event.stopPropagation();
					closeCompletion(view);
					markPaste(text);
					insertPlainText(view, text);
					return true;
				},
			}),
		),
	];
}
