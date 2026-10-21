import { type GasMix, n2Fraction } from '../gas';
import { WATER_VAPOUR_PRESSURE_BAR, ZHL16C_N2 } from './constants';
import { ambientPressure, type BuhlmannSettings, depthAtPressure } from './settings';
import type { TissueState } from './tissues';

const toleratedAmbientPressure = (tissuePressure: number, a: number, b: number, gf: number): number =>
	(tissuePressure - a * gf) / (gf / b + 1 - gf);

export const ceilingPressure = (state: TissueState, gfPercent: number): number => {
	const gf = gfPercent / 100;

	return Math.max(
		...state.n2.map((pressure, index) => toleratedAmbientPressure(pressure, ZHL16C_N2[index].a, ZHL16C_N2[index].b, gf)),
	);
}

export const ceilingDepth = (state: TissueState, gfPercent: number, settings: BuhlmannSettings): number =>
	depthAtPressure(ceilingPressure(state, gfPercent), settings);

const allowedTissuePressure = (ambientPressureBar: number, a: number, b: number, gf: number): number => {
	const mValue = a + ambientPressureBar / b;
	return ambientPressureBar + gf * (mValue - ambientPressureBar);
}

export const noDecompressionLimit = (state: TissueState, depthM: number, gas: GasMix, settings: BuhlmannSettings): number => {
	const gfHigh = settings.gfHigh / 100;
	const alveolar = Math.max(0, ambientPressure(depthM, settings) - WATER_VAPOUR_PRESSURE_BAR) * n2Fraction(gas);
	let limit = Infinity;

	state.n2.forEach((pressure, index) => {
		const { a, b, halfTimeMin } = ZHL16C_N2[index];
		const allowed = allowedTissuePressure(settings.surfacePressureBar, a, b, gfHigh);

		if(pressure >= allowed) {
			limit = 0;
			return;
		}

		if(alveolar <= allowed) return;

		const time = -(halfTimeMin / Math.LN2) * Math.log((alveolar - allowed) / (alveolar - pressure));
		limit = Math.min(limit, time);
	});

	return limit;
}

export const compartmentLoadings = (state: TissueState, settings: BuhlmannSettings): number[] => {
	const gfHigh = settings.gfHigh / 100;

	return state.n2.map((pressure, index) => {
		const { a, b } = ZHL16C_N2[index];
		return (pressure / allowedTissuePressure(settings.surfacePressureBar, a, b, gfHigh)) * 100;
	});
}

export const surfaceGradientFactor = (state: TissueState, settings: BuhlmannSettings): number => {
	const surface = settings.surfacePressureBar;

	return Math.max(
		...state.n2.map((pressure, index) => {
			const { a, b } = ZHL16C_N2[index];
			const mValue = a + surface / b;
			return ((pressure - surface) / (mValue - surface)) * 100;
		}),
	);
}