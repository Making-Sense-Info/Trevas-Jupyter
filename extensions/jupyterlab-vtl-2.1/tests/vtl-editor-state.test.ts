import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

describe('vtl-editor-state', () => {
	beforeEach(async () => {
		vi.resetModules();
		vi.useFakeTimers();
		vi.setSystemTime(new Date('2026-06-17T12:00:00.000Z'));
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('enters paste burst mode after a large paste', async () => {
		const { isPasteBurst, markPaste } = await import('../src/vtl-editor-state');

		expect(isPasteBurst()).toBe(false);

		markPaste('line\n'.repeat(50));

		expect(isPasteBurst()).toBe(true);
	});

	it('clears paste burst mode after the suppression window', async () => {
		const { isPasteBurst, markPaste } = await import('../src/vtl-editor-state');

		markPaste('line\n'.repeat(50));

		vi.advanceTimersByTime(7000);

		expect(isPasteBurst()).toBe(false);
	});

	it('adds extra lint delay for large documents', async () => {
		const { extraLintDelayMs } = await import('../src/vtl-editor-state');

		expect(extraLintDelayMs(10)).toBe(0);
		expect(extraLintDelayMs(40)).toBeGreaterThanOrEqual(400);
		expect(extraLintDelayMs(80)).toBeGreaterThanOrEqual(800);
	});
});
