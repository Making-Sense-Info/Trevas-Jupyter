import { describe, expect, it } from 'vitest';

import {
	VTL_MIME,
	isVtlEditorActive,
	isVtlPath,
	looksLikeVtl,
} from '../src/vtl-mime-detect';

describe('vtl-mime', () => {
	it('detects .vtl file paths', () => {
		expect(isVtlPath('notebook.vtl')).toBe(true);
		expect(isVtlPath('notebook.VTL')).toBe(true);
		expect(isVtlPath('notebook.py')).toBe(false);
	});

	it('detects VTL-like content heuristically', () => {
		expect(looksLikeVtl('calc x := 1;')).toBe(true);
		expect(looksLikeVtl('define dataset ds as select * from t;')).toBe(true);
		expect(looksLikeVtl('print("hello")')).toBe(false);
	});

	it('activates editor extensions when mime or content matches VTL', () => {
		expect(
			isVtlEditorActive({
				mimeType: VTL_MIME,
				sharedModel: { getSource: () => '' },
			}),
		).toBe(true);

		expect(
			isVtlEditorActive({
				mimeType: 'text/plain',
				sharedModel: { getSource: () => 'calc x := 1;' },
			}),
		).toBe(true);

		expect(
			isVtlEditorActive({
				mimeType: 'text/plain',
				sharedModel: { getSource: () => 'print("hello")' },
			}),
		).toBe(false);
	});
});
