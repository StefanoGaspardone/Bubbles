import { describe, expect, it } from 'vitest';

import { ambientPressureBar, bestMixO2Percent, eadM, modM, ppO2Bar } from '../../src/diving/physics';

describe('physics', () => {
	it('ambient pressure', () => {
		expect(ambientPressureBar(0)).toBeCloseTo(1, 9);
		expect(ambientPressureBar(30)).toBeCloseTo(4, 9);
		expect(ambientPressureBar(10.3, 'FRESH')).toBeCloseTo(2, 9);
	});

	it('MOD', () => {
		expect(modM(32)).toBeCloseTo(33.75, 2);
		expect(modM(36)).toBeCloseTo(28.89, 2);
		expect(modM(21)).toBeCloseTo(56.67, 2);
		expect(modM(32, 1.6)).toBeCloseTo(40, 2);
	});

	it('ppO2', () => {
		expect(ppO2Bar(32, 30)).toBeCloseTo(1.28, 9);
		expect(ppO2Bar(21, 0)).toBeCloseTo(0.21, 9);
	});

	it('EAD', () => {
		expect(eadM(21, 30)).toBeCloseTo(30, 6);
		expect(eadM(32, 30)).toBeCloseTo(24.43, 2);
	});

	it('best mix', () => {
		expect(bestMixO2Percent(30)).toBe(35);
		expect(bestMixO2Percent(0)).toBe(100);
	});
});