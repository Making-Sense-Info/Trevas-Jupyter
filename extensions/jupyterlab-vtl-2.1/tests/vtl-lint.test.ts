import { describe, expect, it, vi } from 'vitest';

import { collectVtlLintErrors } from '../src/vtl-lint-collect';

describe('collectVtlLintErrors', () => {
	it('returns no errors for empty input', async () => {
		await expect(collectVtlLintErrors('   ', 1)).resolves.toEqual([]);
	});

	it('returns ANTLR syntax errors for invalid VTL', async () => {
		const errors = await collectVtlLintErrors('this @@@', 1);

		expect(errors.length).toBeGreaterThan(0);
		expect(errors[0].message).toMatch(/no viable alternative/i);
	});

	it('schedules large cells on requestIdleCallback when available', async () => {
		const idle = vi.fn((cb: IdleRequestCallback) => {
			cb({ didTimeout: false, timeRemaining: () => 50 } as IdleDeadline);
			return 0;
		});
		vi.stubGlobal('requestIdleCallback', idle);

		const errors = await collectVtlLintErrors('this @@@', 150);

		expect(idle).toHaveBeenCalledOnce();
		expect(errors.length).toBeGreaterThan(0);

		vi.unstubAllGlobals();
	});
});
