import { describe, expect, it } from 'vitest';

import {
	AIR,
	ceilingDepth,
	compartmentLoadings,
	loadSegment,
	nitrox,
	noDecompressionLimit,
	planDive,
	profileFromLoggedSegments,
	resolveSettings,
	surfaceGradientFactor,
	surfaceInterval,
	surfaceSaturatedTissues,
	ZHL16C_N2,
} from '../../src/diving';

const settings = resolveSettings({ gfLow: 100, gfHigh: 100 });
const surface = surfaceSaturatedTissues(settings);

describe('tissue loading', () => {
	it('a compartment reaches half of the gradient after one half-time at constant depth', () => {
		const loaded = loadSegment(surface, { startDepthM: 30, endDepthM: 30, durationMin: ZHL16C_N2[0].halfTimeMin, gas: AIR }, settings);
		const saturated = loadSegment(surface, { startDepthM: 30, endDepthM: 30, durationMin: 100_000, gas: AIR }, settings);

		const expected = surface.n2[0] + (saturated.n2[0] - surface.n2[0]) / 2;
		expect(loaded.n2[0]).toBeCloseTo(expected, 6);
	});

	it('all compartments converge to the inspired pressure after a very long time', () => {
		const saturated = loadSegment(surface, { startDepthM: 20, endDepthM: 20, durationMin: 100_000, gas: AIR }, settings);
		const inspired = (settings.surfacePressureBar + 2 - 0.0627) * 0.79;

		saturated.n2.forEach((pressure) => expect(pressure).toBeCloseTo(inspired, 4));
	});

	it('Schreiner over a very slow change matches Haldane at the average depth', () => {
		const schreiner = loadSegment(surface, { startDepthM: 20, endDepthM: 20.0001, durationMin: 30, gas: AIR }, settings);
		const haldane = loadSegment(surface, { startDepthM: 20, endDepthM: 20, durationMin: 30, gas: AIR }, settings);

		schreiner.n2.forEach((pressure, index) => expect(pressure).toBeCloseTo(haldane.n2[index], 4));
	});

	it('surface interval off-gasses towards surface saturation', () => {
		const loaded = loadSegment(surface, { startDepthM: 30, endDepthM: 30, durationMin: 25, gas: AIR }, settings);
		const afterInterval = surfaceInterval(loaded, 60, settings);
		const afterLongInterval = surfaceInterval(loaded, 100_000, settings);

		expect(Math.max(...afterInterval.n2)).toBeLessThan(Math.max(...loaded.n2));
		afterLongInterval.n2.forEach((pressure, index) => expect(pressure).toBeCloseTo(surface.n2[index], 4));
	});

	it('converts logged segments with linear transitions from the previous depth', () => {
		const profile = profileFromLoggedSegments(
			[
				{ type: 'DESCENT', depthM: 18, durationSeconds: 120 },
				{ type: 'BOTTOM', depthM: 18, durationSeconds: 1800 },
				{ type: 'ASCENT', depthM: 5, durationSeconds: 120 },
				{ type: 'SAFETY_STOP', depthM: 5, durationSeconds: 180 },
				{ type: 'SURFACE', depthM: 0, durationSeconds: 60 },
			],
			AIR,
		);

		expect(profile.map((segment) => [segment.startDepthM, segment.endDepthM])).toEqual([[0, 18], [18, 18], [18, 5], [5, 5], [5, 0]]);
		expect(profile[1].durationMin).toBe(30);
	});
});

describe('no-decompression limit', () => {
	const ndl = (depthM: number, gfHigh = 100, gas = AIR) => {
		const custom = resolveSettings({ gfLow: Math.min(gfHigh, 100), gfHigh });
		return noDecompressionLimit(surfaceSaturatedTissues(custom), depthM, gas, custom);
	};

	it('decreases with depth', () => {
		const limits = [12, 18, 24, 30, 40].map((depth) => ndl(depth));
		limits.slice(1).forEach((limit, index) => expect(limit).toBeLessThan(limits[index]));
	});

	it('is shorter with a lower GF High', () => {
		expect(ndl(30, 70)).toBeLessThan(ndl(30, 100));
	});

	it('is longer with nitrox than with air at the same depth', () => {
		expect(ndl(30, 100, nitrox(32))).toBeGreaterThan(ndl(30, 100, AIR));
	});

	it('is unlimited in very shallow water', () => {
		expect(ndl(3)).toBe(Infinity);
	});

	it('matches the ceiling: surfacing is allowed just before the NDL and not just after it', () => {
		const depth = 30;
		const limit = ndl(depth);

		const before = loadSegment(surface, { startDepthM: depth, endDepthM: depth, durationMin: limit - 0.1, gas: AIR }, settings);
		const after = loadSegment(surface, { startDepthM: depth, endDepthM: depth, durationMin: limit + 0.1, gas: AIR }, settings);

		expect(ceilingDepth(before, settings.gfHigh, settings)).toBe(0);
		expect(ceilingDepth(after, settings.gfHigh, settings)).toBeGreaterThan(0);
		expect(surfaceGradientFactor(after, settings)).toBeGreaterThan(100);
		expect(Math.max(...compartmentLoadings(after, settings))).toBeGreaterThan(100);
	});

	it('is reduced by residual nitrogen from a previous dive', () => {
		const firstDive = loadSegment(surface, { startDepthM: 25, endDepthM: 25, durationMin: 30, gas: AIR }, settings);
		const afterInterval = surfaceInterval(firstDive, 60, settings);

		expect(noDecompressionLimit(afterInterval, 25, AIR, settings)).toBeLessThan(noDecompressionLimit(surface, 25, AIR, settings));
	});
});

describe('dive plan', () => {
	it('within the NDL: no decompression stops, safety stop at 5 m', () => {
		const plan = planDive({ depthM: 18, bottomTimeMin: 30, gas: AIR, settings: { gfLow: 40, gfHigh: 85 } });

		expect(plan.isDecompression).toBe(false);
		expect(plan.decoStops).toEqual([]);
		expect(plan.steps.some((step) => step.phase === 'SAFETY_STOP' && step.endDepthM === 5)).toBe(true);
	});

	it('beyond the NDL: stops on multiples of 3 m, deepest first, last one at 3 m', () => {
		const plan = planDive({ depthM: 40, bottomTimeMin: 25, gas: AIR, settings: { gfLow: 40, gfHigh: 85 } });

		expect(plan.isDecompression).toBe(true);
		plan.decoStops.forEach((stop) => expect(stop.depthM % 3).toBe(0));

		const depths = plan.decoStops.map((stop) => stop.depthM);
		expect([...depths].sort((a, b) => b - a)).toEqual(depths);
		expect(depths.at(-1)).toBe(3);
	});

	it('surfaces with tissues within GF High', () => {
		const plan = planDive({ depthM: 40, bottomTimeMin: 25, gas: AIR, settings: { gfLow: 40, gfHigh: 85 } });

		expect(surfaceGradientFactor(plan.tissuesAtSurface, plan.settings)).toBeLessThanOrEqual(85 + 1e-6);
	});

	it('a more conservative GF Low gives a deeper first stop', () => {
		const conservative = planDive({ depthM: 45, bottomTimeMin: 25, gas: AIR, settings: { gfLow: 20, gfHigh: 85 } });
		const liberal = planDive({ depthM: 45, bottomTimeMin: 25, gas: AIR, settings: { gfLow: 70, gfHigh: 85 } });

		expect(conservative.decoStops[0].depthM).toBeGreaterThanOrEqual(liberal.decoStops[0].depthM);
		expect(conservative.ttsMin).toBeGreaterThanOrEqual(liberal.ttsMin);
	});

	it('runtime is the sum of the steps and bottom time includes descent', () => {
		const plan = planDive({ depthM: 30, bottomTimeMin: 20, gas: AIR });
		const sum = plan.steps.reduce((total, step) => total + step.durationMin, 0);

		expect(plan.totalRuntimeMin).toBeCloseTo(sum, 9);
		expect(plan.totalRuntimeMin - plan.ttsMin).toBeCloseTo(20, 9);
	});

	it('rejects a bottom time shorter than the descent', () => {
		expect(() => planDive({ depthM: 40, bottomTimeMin: 1, gas: AIR })).toThrow(RangeError);
	});
});