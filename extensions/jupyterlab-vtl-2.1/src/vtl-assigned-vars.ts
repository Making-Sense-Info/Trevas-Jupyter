/**
 * Lightweight assigned-variable highlighting (red).
 * MatchDecorator is viewport-scoped; docChanged updates are debounced.
 */
import { RangeSetBuilder } from '@codemirror/state';
import {
	Decoration,
	EditorView,
	MatchDecorator,
	ViewPlugin,
	type DecorationSet,
	type ViewUpdate,
} from '@codemirror/view';
import type { Extension } from '@codemirror/state';

import { isPasteBurst, msSincePaste } from './vtl-editor-state';
import { isVtlEditorActive } from './vtl-mime';

const DOC_CHANGE_DEBOUNCE_MS = 80;
const PASTE_DEBOUNCE_MS = 500;

/** Color from vtl-colors.ts via vtlEditorTheme (vtl-language.ts). */
const assignedMark = Decoration.mark({ class: 'cm-vtl-assigned-var' });

const calcAssignMatcher = new MatchDecorator({
	regexp: /\b([A-Za-z_][A-Za-z0-9_]*)\s*(?::=|<-)/g,
	decorate: (add, from, _to, match) => {
		add(from, from + match[1].length, assignedMark);
	},
});

const defineNameMatcher = new MatchDecorator({
	regexp:
		/\b(?:dataset|ruleset|structure|variable|component|measure|hierarchy|valuedomain)\s+([A-Za-z_][A-Za-z0-9_]*)/gi,
	decorate: (add, _from, to, match) => {
		const name = match[1];
		add(to - name.length, to, assignedMark);
	},
});

function mergeDecoSets(a: DecorationSet, b: DecorationSet): DecorationSet {
	const builder = new RangeSetBuilder<Decoration>();
	for (const set of [a, b]) {
		set.between(0, Number.MAX_SAFE_INTEGER, (from, to, value) => {
			builder.add(from, to, value);
		});
	}
	return builder.finish();
}

function buildAssignedDecorations(view: EditorView): DecorationSet {
	return mergeDecoSets(
		calcAssignMatcher.createDeco(view),
		defineNameMatcher.createDeco(view),
	);
}

export function createVtlAssignedVarHighlight(isActive: () => boolean): Extension {
	return ViewPlugin.fromClass(
		class {
			decorations: DecorationSet = Decoration.none;
			private active = false;
			private debounceTimer: ReturnType<typeof setTimeout> | null = null;

			constructor(view: EditorView) {
				this.active = isActive();
				if (this.active) {
					this.decorations = buildAssignedDecorations(view);
				}
			}

			destroy(): void {
				if (this.debounceTimer !== null) {
					clearTimeout(this.debounceTimer);
				}
			}

			update(update: ViewUpdate): void {
				if (!update.docChanged && !update.viewportChanged) {
					return;
				}

				const nowActive = isActive();
				if (!nowActive) {
					if (this.active) {
						this.active = false;
						this.decorations = Decoration.none;
					}
					return;
				}
				this.active = true;

				if (update.viewportChanged && !update.docChanged) {
					this.decorations = buildAssignedDecorations(update.view);
					return;
				}

				if (isPasteBurst()) {
					if (this.debounceTimer !== null) {
						clearTimeout(this.debounceTimer);
					}
					this.decorations = Decoration.none;
					return;
				}

				if (this.debounceTimer !== null) {
					clearTimeout(this.debounceTimer);
				}
				const view = update.view;
				const debounceMs =
					msSincePaste() < PASTE_DEBOUNCE_MS
						? PASTE_DEBOUNCE_MS
						: DOC_CHANGE_DEBOUNCE_MS;
				this.debounceTimer = setTimeout(() => {
					this.debounceTimer = null;
					if (this.active && !isPasteBurst()) {
						this.decorations = buildAssignedDecorations(view);
					}
				}, debounceMs);
			}
		},
		{ decorations: value => value.decorations },
	);
}

export function vtlAssignedVarHighlightForModel(model: {
	mimeType: string;
	sharedModel: { getSource: () => string };
}): Extension {
	return createVtlAssignedVarHighlight(() => isVtlEditorActive(model));
}
