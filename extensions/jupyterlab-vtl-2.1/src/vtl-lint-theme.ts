/**
 * ANTLR lint styling from VTL_COLORS.error — mirrors @codemirror/lint baseTheme.
 */
import { EditorView } from '@codemirror/view';

import { VTL_COLORS } from './vtl-colors';

/** Extra class on lint marks (see vtl-lint.ts markClass). */
export const VTL_ERROR_MARK_CLASS = 'cm-vtl-error';

type ThemeSpec = Record<string, Record<string, unknown>>;

/** SVG wavy underline — same technique as @codemirror/lint underline(). */
function errorSquiggleUrl(color: string): string {
	const stroke = encodeURIComponent(color);
	return `url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="6" height="3" viewBox="0 0 40 40"><path d="m0 2.5 l2 -1.5 l1 0 l2 1.5 l1 0" stroke="${stroke}" fill="none" stroke-width=".7"/></svg>')`;
}

/** High-specificity selectors — beats Elyra/Onyxia theme overrides. */
function notebookScoped(
	baseSelector: string,
	props: Record<string, unknown>,
): ThemeSpec {
	const rule = { ...props };
	return {
		[baseSelector]: rule,
		[`.jp-Notebook .jp-CodeCell .cm-content ${baseSelector}`]: rule,
		[`.cm-editor .cm-content ${baseSelector}`]: rule,
	};
}

function mergeSpecs(...parts: ThemeSpec[]): ThemeSpec {
	return Object.assign({}, ...parts);
}

function buildLintThemeSpec(errorColor: string): ThemeSpec {
	const squiggle = errorSquiggleUrl(errorColor);
	const squiggleRule = {
		backgroundImage: `${squiggle} !important`,
		backgroundRepeat: 'repeat-x !important',
		backgroundPosition: 'left bottom !important',
		backgroundColor: 'transparent !important',
		paddingBottom: '0.7px',
		textDecoration: `underline wavy ${errorColor} !important`,
		textDecorationSkipInk: 'none',
	};

	return mergeSpecs(
		notebookScoped('.cm-editor', {
			'--vtl-error-color': errorColor,
		}),
		// Required base — CM applies cm-lintRange + cm-lintRange-error
		notebookScoped('.cm-lintRange', {
			backgroundPosition: 'left bottom !important',
			backgroundRepeat: 'repeat-x !important',
			paddingBottom: '0.7px',
		}),
		notebookScoped('.cm-lintRange-error', squiggleRule),
		notebookScoped(`.cm-lintRange.${VTL_ERROR_MARK_CLASS}`, squiggleRule),
		notebookScoped(`.${VTL_ERROR_MARK_CLASS}`, squiggleRule),
		notebookScoped('.cm-diagnostic-error', {
			borderLeft: `5px solid ${errorColor} !important`,
			color: `${errorColor} !important`,
		}),
		notebookScoped('.cm-tooltip.cm-tooltip-lint', {
			padding: '0',
			margin: '0',
		}),
		{
			'.cm-lintPoint': {
				position: 'relative',
				'&:after': {
					content: '""',
					position: 'absolute',
					bottom: '0',
					left: '-2px',
					borderLeft: '3px solid transparent',
					borderRight: '3px solid transparent',
					borderBottom: `4px solid ${errorColor}`,
				},
			},
			'.jp-Notebook .jp-CodeCell .cm-content .cm-lintPoint': {
				position: 'relative',
				'&:after': {
					content: '""',
					position: 'absolute',
					bottom: '0',
					left: '-2px',
					borderLeft: '3px solid transparent',
					borderRight: '3px solid transparent',
					borderBottom: `4px solid ${errorColor}`,
				},
			},
			'.cm-editor .cm-content .cm-lintPoint': {
				position: 'relative',
				'&:after': {
					content: '""',
					position: 'absolute',
					bottom: '0',
					left: '-2px',
					borderLeft: '3px solid transparent',
					borderRight: '3px solid transparent',
					borderBottom: `4px solid ${errorColor}`,
				},
			},
		},
	);
}

/** Lint squiggles + tooltips — registered from index.ts (plugin bootstrap). */
export const vtlLintTheme = EditorView.baseTheme(
	buildLintThemeSpec(VTL_COLORS.error) as Parameters<
		typeof EditorView.baseTheme
	>[0],
);
