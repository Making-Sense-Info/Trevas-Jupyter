/**
 * VTL 2.1 StreamLanguage tokenizer for CodeMirror 6.
 * Token classes are styled by vtl-theme.ts (colors from vtl-colors.ts).
 */
import {
	HighlightStyle,
	LanguageSupport,
	StreamLanguage,
	syntaxHighlighting,
	type StreamParser,
	type StringStream,
} from '@codemirror/language';
import { tags as t } from '@lezer/highlight';

import { classifyWord } from './vtl-token-categories';
import { vtlEditorTheme } from './vtl-theme';

function readDoubleQuotedString(stream: StringStream): void {
	stream.next();
	while (!stream.eol()) {
		const ch = stream.peek();
		if (ch === '"') {
			stream.next();
			break;
		}
		if (ch === '\\') {
			stream.next();
			stream.next();
			continue;
		}
		stream.next();
	}
}

function readSingleQuotedString(stream: StringStream): void {
	stream.next();
	while (!stream.eol()) {
		const ch = stream.peek();
		if (ch === "'") {
			stream.next();
			break;
		}
		if (ch === '\\') {
			stream.next();
			stream.next();
			continue;
		}
		stream.next();
	}
}

type VtlStreamState = {
	inBlockComment: boolean;
};

function readBlockComment(stream: StringStream, state: VtlStreamState): void {
	if (stream.skipTo('*/')) {
		stream.match('*/');
		state.inBlockComment = false;
	} else {
		state.inBlockComment = true;
		stream.skipToEnd();
	}
}

const vtlStreamParser: StreamParser<VtlStreamState> = {
	startState: () => ({ inBlockComment: false }),
	copyState: state => ({ inBlockComment: state.inBlockComment }),

	token(stream, state) {
		if (state.inBlockComment) {
			readBlockComment(stream, state);
			return 'comment';
		}

		if (stream.eatSpace()) {
			return null;
		}

		if (stream.match('//')) {
			stream.skipToEnd();
			return 'comment';
		}

		if (stream.match('/*')) {
			readBlockComment(stream, state);
			return 'comment';
		}

		if (stream.peek() === '"') {
			readDoubleQuotedString(stream);
			return 'string';
		}

		if (stream.peek() === "'") {
			readSingleQuotedString(stream);
			return 'quotedId';
		}

		if (stream.match(':=') || stream.match('<-')) {
			return 'assign';
		}

		if (
			stream.match('||') ||
			stream.match('->') ||
			stream.match('<>') ||
			stream.match('<=') ||
			stream.match('>=') ||
			stream.match('==') ||
			stream.match('!=')
		) {
			return 'compare';
		}

		if (stream.match(/^[+-]?\d+\.\d+([eE][+-]?\d+)?/)) {
			return 'float';
		}

		if (stream.match(/^[+-]?\d+([eE][+-]?\d+)?/)) {
			return 'integer';
		}

		if (stream.match(/^[A-Za-z_][A-Za-z0-9_.]*/)) {
			return classifyWord(stream.current());
		}

		if (stream.match(/^[+\-*/%<>=]/)) {
			return 'operator';
		}

		if (stream.match(/^[,:;()[\]{}#]/)) {
			return 'punct';
		}

		stream.next();
		return null;
	},
	tokenTable: {
		comment: t.comment,
		string: t.string,
		quotedId: t.special(t.string),
		assign: t.operator,
		compare: t.operator,
		operator: t.operator,
		punct: t.punctuation,
		integer: t.number,
		float: t.number,
		control: t.controlKeyword,
		define: t.definitionKeyword,
		type: t.typeName,
		logic: t.logicOperator,
		join: t.moduleKeyword,
		function: t.function(t.variableName),
		trevas: t.standard(t.variableName),
		bool: t.bool,
		null: t.null,
		identifier: t.variableName,
	},
	languageData: {
		commentTokens: { line: '//', block: { open: '/*', close: '*/' } },
		closeBrackets: { brackets: ['(', '[', '{', '"', "'"] },
	},
};

/** Token → CSS class mapping; colors in vtl-theme.ts (see vtl-colors.ts). */
export const vtlHighlightStyle = HighlightStyle.define([
	{ tag: t.comment, class: 'cm-vtl-comment' },
	{ tag: t.string, class: 'cm-vtl-string' },
	{ tag: t.special(t.string), class: 'cm-vtl-quoted-id' },
	{ tag: t.number, class: 'cm-vtl-number' },
	{ tag: t.bool, class: 'cm-vtl-bool' },
	{ tag: t.null, class: 'cm-vtl-null' },
	{ tag: t.controlKeyword, class: 'cm-vtl-control' },
	{ tag: t.definitionKeyword, class: 'cm-vtl-define' },
	{ tag: t.typeName, class: 'cm-vtl-type' },
	{ tag: t.logicOperator, class: 'cm-vtl-logic' },
	{ tag: t.moduleKeyword, class: 'cm-vtl-join' },
	{ tag: t.function(t.variableName), class: 'cm-vtl-function' },
	{ tag: t.standard(t.variableName), class: 'cm-vtl-trevas' },
	{ tag: t.operator, class: 'cm-vtl-operator' },
	{ tag: t.punctuation, class: 'cm-vtl-punct' },
	{ tag: t.variableName, class: 'cm-vtl-identifier' },
]);

export const vtlLanguage = StreamLanguage.define(vtlStreamParser);

/**
 * Language compartment only — no lint/autocomplete here (those live in
 * IEditorExtensionRegistry). Duplicating autocompletion `override` breaks
 * CodeMirror with "Config merge conflict for field override".
 */
export function vtlLanguageSupport(): LanguageSupport {
	return new LanguageSupport(vtlLanguage, [
		vtlEditorTheme,
		syntaxHighlighting(vtlHighlightStyle),
	]);
}
