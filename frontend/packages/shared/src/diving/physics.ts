import type { WaterType } from '../types';

export const SURFACE_PRESSURE_BAR = 1.0;
export const DEFAULT_MAX_PPO2_BAR = 1.4;

const SALT_WATER_METERS_PER_BAR = 10.0;
const FRESH_WATER_METERS_PER_BAR = 10.3;

export const metersPerBar = (waterType: WaterType = 'SALT'): number =>
	waterType === 'FRESH' ? FRESH_WATER_METERS_PER_BAR : SALT_WATER_METERS_PER_BAR;

export const ambientPressureBar = (depthM: number, waterType: WaterType = 'SALT', surfacePressureBar: number = SURFACE_PRESSURE_BAR): number => 
    surfacePressureBar + depthM / metersPerBar(waterType);

export const depthFromPressureM = (pressureBar: number, waterType: WaterType = 'SALT', surfacePressureBar: number = SURFACE_PRESSURE_BAR): number => 
    Math.max(0, (pressureBar - surfacePressureBar) * metersPerBar(waterType));

export const ppO2Bar = (o2Percent: number, depthM: number, waterType: WaterType = 'SALT'): number =>
	(o2Percent / 100) * ambientPressureBar(depthM, waterType);

export const modM = (o2Percent: number, maxPpO2Bar: number = DEFAULT_MAX_PPO2_BAR, waterType: WaterType = 'SALT'): number => {
	if(o2Percent <= 0) throw new RangeError('O2 percentage must be positive');
	return (maxPpO2Bar / (o2Percent / 100) - SURFACE_PRESSURE_BAR) * metersPerBar(waterType);
}

export const eadM = (o2Percent: number, depthM: number, waterType: WaterType = 'SALT'): number => {
	const n2Fraction = 1 - o2Percent / 100;
	const ead = ((n2Fraction / 0.79) * ambientPressureBar(depthM, waterType) - SURFACE_PRESSURE_BAR) * metersPerBar(waterType);
	
    return Math.max(0, ead);
}

export const bestMixO2Percent = (depthM: number, maxPpO2Bar: number = DEFAULT_MAX_PPO2_BAR, waterType: WaterType = 'SALT'): number =>
	Math.min(100, Math.floor((maxPpO2Bar / ambientPressureBar(depthM, waterType)) * 100));