import {
	autocompletion,
	acceptCompletion,
	completionStatus,
	startCompletion,
	type Completion,
	type CompletionContext,
	type CompletionResult,
} from '@codemirror/autocomplete';
import type { Extension } from '@codemirror/state';
import { Prec } from '@codemirror/state';
import { keymap } from '@codemirror/view';

import { isCompletionSuppressed } from './vtl-editor-state';
import { isVtlEditorActive } from './vtl-mime';
import { TREVAS_FUNCTIONS, TREVAS_FUNCTION_NAMES } from './trevas-functions';
import { VTL_KEYWORDS, VTL_KEYWORD_SET } from './vtl-keywords';

const KEYWORD_COMPLETIONS: Completion[] = VTL_KEYWORDS.map(keyword => ({
	label: keyword,
	type: 'keyword',
	detail: 'VTL keyword',
}));

const FUNCTION_COMPLETIONS: Completion[] = TREVAS_FUNCTIONS.map(fn => ({
	label: fn.name,
	type: 'function',
	detail: fn.signature,
	info: fn.detail,
}));

const SNIPPET_COMPLETIONS: Completion[] = [
	{
		label: 'define dataset',
		type: 'text',
		detail: 'snippet',
		apply: 'define dataset ${name} as\n  select \n  from input\n;',
	},
	{
		label: 'calc',
		type: 'text',
		detail: 'snippet',
		apply: 'calc ${name} :=\n  ',
	},
];

const STATIC_COMPLETIONS: Completion[] = [
	...KEYWORD_COMPLETIONS,
	...FUNCTION_COMPLETIONS,
	...SNIPPET_COMPLETIONS,
];

const STATIC_LABELS = new Set(
	STATIC_COMPLETIONS.map(option => option.label.toLowerCase()),
);

const ASSIGNMENT_RE =
	/\b(?:calc|define\s+dataset)\s+([A-Za-z_][A-Za-z0-9_]*)/gi;
const IDENTIFIER_RE = /\b([A-Za-z_][A-Za-z0-9_]*)\b/g;

/** Above this line count, skip full-doc identifier scan (paste perf). */
const LARGE_DOC_LINES = 60;

let namesCacheKey = '';
let namesCacheValue: string[] = [];

function cacheKeyFor(text: string): string {
	if (text.length <= 200) {
		return `${text.length}:${text}`;
	}
	return `${text.length}:${text.slice(0, 80)}:${text.slice(-80)}`;
}

function extractLocalNames(text: string, lineCount: number): string[] {
	const key = cacheKeyFor(text);
	if (key === namesCacheKey) {
		return namesCacheValue;
	}

	const names = new Set<string>();

	ASSIGNMENT_RE.lastIndex = 0;
	for (const match of text.matchAll(ASSIGNMENT_RE)) {
		names.add(match[1]);
	}

	if (lineCount <= LARGE_DOC_LINES) {
		IDENTIFIER_RE.lastIndex = 0;
		for (const match of text.matchAll(IDENTIFIER_RE)) {
			const name = match[1];
			const lower = name.toLowerCase();
			if (
				VTL_KEYWORD_SET.has(lower) ||
				TREVAS_FUNCTION_NAMES.has(lower) ||
				lower === 'true' ||
				lower === 'false' ||
				lower === 'input' ||
				lower === 'output'
			) {
				continue;
			}
			names.add(name);
		}
	}

	namesCacheKey = key;
	namesCacheValue = [...names];
	return namesCacheValue;
}

const IDENTIFIER_AT_CURSOR_RE = /[A-Za-z_][A-Za-z0-9_.]*/;

function tokenAtCursor(
	context: CompletionContext,
): { from: number; to: number; text: string } | null {
	const { pos, state } = context;
	const line = state.doc.lineAt(pos);
	const before = state.sliceDoc(line.from, pos);
	const after = state.sliceDoc(pos, line.to);
	const beforeMatch = before.match(/[A-Za-z_][A-Za-z0-9_.]*$/);
	const afterMatch = after.match(/^[A-Za-z_][A-Za-z0-9_.]*/);
	if (!beforeMatch && !afterMatch) {
		return null;
	}
	const from = beforeMatch
		? line.from + before.length - beforeMatch[0].length
		: pos;
	const to = afterMatch ? pos + afterMatch[0].length : pos;
	return { from, to, text: state.sliceDoc(from, to) };
}

function createVtlCompletionSource(
	isActive?: () => boolean,
): (context: CompletionContext) => CompletionResult | null {
	return (context: CompletionContext): CompletionResult | null => {
		if (isActive && !isActive()) {
			return null;
		}
		if (isCompletionSuppressed()) {
			return null;
		}

		const word =
			tokenAtCursor(context) ??
			context.matchBefore(IDENTIFIER_AT_CURSOR_RE);
		if (!word && !context.explicit) {
			return null;
		}

		const from = word ? word.from : context.pos;
		const prefix = (word?.text ?? '').toLowerCase();
		const docText = context.state.doc.toString();
		const lineCount = context.state.doc.lines;
		const localNames = extractLocalNames(docText, lineCount);

		const localCompletions: Completion[] = localNames
			.filter(name => !STATIC_LABELS.has(name.toLowerCase()))
			.filter(name => !prefix || name.toLowerCase().startsWith(prefix))
			.map(name => ({
				label: name,
				type: 'variable',
				detail: 'local name',
			}));

		const staticOptions = STATIC_COMPLETIONS.filter(option => {
			if (!prefix) {
				return true;
			}
			return option.label.toLowerCase().startsWith(prefix);
		});

		const options = [...localCompletions, ...staticOptions];
		if (!options.length) {
			return null;
		}

		return { from, options, validFor: /^[A-Za-z_][A-Za-z0-9_.]*$/ };
	};
}

/** Tab keymap for VTL completion (bundled with vtl-autocomplete extension). */
export const vtlCompletionKeymap = Prec.high(
	keymap.of([
		{
			key: 'Tab',
			run: view => {
				if (isCompletionSuppressed()) {
					return false;
				}
				const status = completionStatus(view.state);
				if (status === 'active') {
					return acceptCompletion(view);
				}
				return startCompletion(view);
			},
		},
	]),
);

export function vtlAutocomplete(isActive?: () => boolean): Extension[] {
	return [
		autocompletion({
			override: [createVtlCompletionSource(isActive)],
			// Typing-only; paste must not open the menu (see vtl-editor-state cooldown).
			activateOnTyping: true,
			activateOnTypingDelay: 300,
			defaultKeymap: true,
			maxRenderedOptions: 24,
		}),
	];
}

export function vtlAutocompleteForModel(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): Extension[] {
	return vtlAutocomplete(() => isVtlEditorActive(model));
}
