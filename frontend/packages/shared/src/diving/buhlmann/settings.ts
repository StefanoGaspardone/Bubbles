import type { WaterType } from '../../types';
import { SEA_LEVEL_PRESSURE_BAR } from './constants';

export interface BuhlmannSettings {
	gfLow: number;
	gfHigh: number;
	waterType: WaterType;
	surfacePressureBar: number;
	descentRateMPerMin: number;
	ascentRateMPerMin: number;
	stopIntervalM: number;
	lastStopDepthM: number;
}

export const DEFAULT_BUHLMANN_SETTINGS: BuhlmannSettings = {
	gfLow: 40,
	gfHigh: 85,
	waterType: 'SALT',
	surfacePressureBar: SEA_LEVEL_PRESSURE_BAR,
	descentRateMPerMin: 18,
	ascentRateMPerMin: 9,
	stopIntervalM: 3,
	lastStopDepthM: 3,
}

export const resolveSettings = (settings?: Partial<BuhlmannSettings>): BuhlmannSettings => {
	const resolved = { ...DEFAULT_BUHLMANN_SETTINGS, ...settings };

	if(resolved.gfLow <= 0 || resolved.gfHigh <= 0 || resolved.gfLow > resolved.gfHigh || resolved.gfHigh > 100) {
		throw new RangeError('Gradient factors must satisfy 0 < GF Low <= GF High <= 100');
	}

	if(resolved.ascentRateMPerMin <= 0 || resolved.descentRateMPerMin <= 0) {
		throw new RangeError('Ascent and descent rates must be positive');
	}
	
    if(resolved.stopIntervalM <= 0 || resolved.lastStopDepthM <= 0) {
		throw new RangeError('Stop interval and last stop depth must be positive');
	}

	return resolved;
}

export const ambientPressure = (depthM: number, settings: BuhlmannSettings): number =>
	settings.surfacePressureBar + depthM / (settings.waterType === 'FRESH' ? 10.3 : 10.0);

export const depthAtPressure = (pressureBar: number, settings: BuhlmannSettings): number =>
	Math.max(0, (pressureBar - settings.surfacePressureBar) * (settings.waterType === 'FRESH' ? 10.3 : 10.0));