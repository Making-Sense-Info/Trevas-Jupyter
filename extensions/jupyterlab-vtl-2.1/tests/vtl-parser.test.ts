import { describe, expect, it } from 'vitest';

import { validateVtl } from '../src/vtl-parser';

describe('validateVtl', () => {
	it('returns no errors for empty or whitespace-only input', () => {
		expect(validateVtl('')).toEqual([]);
		expect(validateVtl('   \n  ')).toEqual([]);
	});

	it('accepts a simple assignment statement', () => {
		expect(validateVtl('x := 1;')).toEqual([]);
	});

	it('reports syntax errors for invalid tokens', () => {
		const errors = validateVtl('this @@@');

		expect(errors).toHaveLength(1);
		expect(errors[0].startLine).toBe(1);
		expect(errors[0].message).toMatch(/no viable alternative/i);
	});

	it('reports errors when a calc statement is used at script root', () => {
		const errors = validateVtl('calc broken :=');

		expect(errors.length).toBeGreaterThanOrEqual(1);
		expect(errors.some(error => error.message.includes('calc'))).toBe(true);
	});

	it('includes column ranges for the offending symbol', () => {
		const errors = validateVtl('this @@@');
		const error = errors[0];

		expect(error.startCol).toBeLessThan(error.endCol);
	});
});
