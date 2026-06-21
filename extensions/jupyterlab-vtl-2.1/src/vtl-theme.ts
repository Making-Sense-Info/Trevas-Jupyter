/**
 * CodeMirror syntax theme from VTL_COLORS — beats Elyra/Onyxia overrides.
 */
import { EditorView } from '@codemirror/view';

import { VTL_COLORS } from './vtl-colors';

type CssProps = Record<string, string>;

function vtlClassRules(
	classNames: string | string[],
	color: string,
	extra: CssProps = {},
): Record<string, CssProps> {
	const names = Array.isArray(classNames) ? classNames : [classNames];
	const rule: CssProps = { color: `${color} !important`, ...extra };
	const out: Record<string, CssProps> = {};
	for (const name of names) {
		out[`.${name}`] = rule;
		out[`.jp-Notebook .jp-CodeCell .cm-content .${name}`] = rule;
		out[`.cm-editor .cm-content .${name}`] = rule;
	}
	return out;
}

function mergeRules(
	...parts: Record<string, CssProps>[]
): Record<string, CssProps> {
	return Object.assign({}, ...parts);
}

/** Syntax + assigned-variable colors — register with vtlLanguageSupport(). */
export const vtlEditorTheme = EditorView.baseTheme(
	mergeRules(
		vtlClassRules('cm-vtl-comment', VTL_COLORS.comment, {
			fontStyle: 'italic',
		}),
		vtlClassRules('cm-vtl-string', VTL_COLORS.string),
		vtlClassRules('cm-vtl-quoted-id', VTL_COLORS.quotedId, {
			fontStyle: 'italic',
		}),
		vtlClassRules(
			['cm-vtl-number', 'cm-vtl-integer', 'cm-vtl-float'],
			VTL_COLORS.number,
			{ fontWeight: '500' },
		),
		vtlClassRules('cm-vtl-bool', VTL_COLORS.bool, { fontWeight: '700' }),
		vtlClassRules('cm-vtl-null', VTL_COLORS.null, {
			fontStyle: 'italic',
			fontWeight: '600',
		}),
		vtlClassRules(
			[
				'cm-vtl-control',
				'cm-vtl-define',
				'cm-vtl-logic',
				'cm-vtl-join',
				'cm-vtl-keyword',
			],
			VTL_COLORS.keyword,
			{ fontWeight: '700' },
		),
		vtlClassRules('cm-vtl-type', VTL_COLORS.type, {
			fontWeight: '600',
			fontStyle: 'italic',
		}),
		vtlClassRules('cm-vtl-function', VTL_COLORS.function, {
			fontWeight: '600',
		}),
		vtlClassRules('cm-vtl-trevas', VTL_COLORS.function, {
			fontWeight: '600',
		}),
		vtlClassRules(
			['cm-vtl-assign', 'cm-vtl-compare', 'cm-vtl-operator'],
			VTL_COLORS.operator,
			{ fontWeight: '700' },
		),
		vtlClassRules('cm-vtl-punct', VTL_COLORS.punct),
		vtlClassRules('cm-vtl-identifier', VTL_COLORS.identifier),
		vtlClassRules('cm-vtl-assigned-var', VTL_COLORS.assignedVar, {
			fontWeight: '600',
		}),
	),
);
