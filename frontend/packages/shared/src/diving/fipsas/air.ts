export interface FipsasAirInput {
	cylinderL: number;
	pressureBar: number;
	reserveBar?: number;
	sacLPerMin?: number;
	marginPercent?: number;
}

export type FipsasAirPhase = 'BOTTOM' | 'ASCENT' | 'DEEP_STOP' | 'SAFETY_STOP' | 'DECO_STOP' | 'SURFACING';

export interface FipsasAirLine {
	phase: FipsasAirPhase;
	pressureBar: number;
	durationMin: number;
	liters: number;
}

export interface FipsasAirResult {
	lines: FipsasAirLine[];
	marginLiters: number;
	requiredLiters: number;
	availableLiters: number;
	sufficient: boolean;
}

export interface FipsasAirProfile {
	depthM: number;
	bottomMin: number;
	ascentMin: number;
	stopDepthM: number;
	deepStopMin: number;
	decoMin: number;
	safetyStopMin: number;
	surfacingMin: number;
}

export const DEFAULT_RESERVE_BAR = 50;
export const DEFAULT_SAC_L_PER_MIN = 20;
export const DEFAULT_AIR_MARGIN_PERCENT = 10;

export const coursePressureBar = (depthM: number): number => 
	depthM / 10 + 1;

const ceilLiters = (liters: number): number => Math.ceil(liters - 1e-9);

export const calculateAir = (profile: FipsasAirProfile, input: FipsasAirInput): FipsasAirResult => {
	const sac = input.sacLPerMin ?? DEFAULT_SAC_L_PER_MIN;
	const reserve = input.reserveBar ?? DEFAULT_RESERVE_BAR;
	const marginPercent = input.marginPercent ?? DEFAULT_AIR_MARGIN_PERCENT;

	if(input.cylinderL <= 0 || input.pressureBar <= 0 || sac <= 0 || reserve < 0 || marginPercent < 0) {
		throw new RangeError('Invalid air parameters');
	}

	const { depthM, stopDepthM } = profile;

	const line = (phase: FipsasAirPhase, pressureBar: number, durationMin: number): FipsasAirLine => ({
		phase,
		pressureBar,
		durationMin,
		liters: ceilLiters(pressureBar * durationMin * sac),
	});

	const lines = [
		line('BOTTOM', coursePressureBar(depthM), profile.bottomMin),
		line('ASCENT', coursePressureBar((depthM + stopDepthM) / 2), profile.ascentMin),
		line('DEEP_STOP', coursePressureBar(depthM / 2), profile.deepStopMin),
		line('DECO_STOP', coursePressureBar(stopDepthM), profile.decoMin),
		line('SAFETY_STOP', coursePressureBar(stopDepthM), profile.safetyStopMin),
		line('SURFACING', coursePressureBar(stopDepthM), profile.surfacingMin),
	].filter((entry) => entry.durationMin > 0);

	const subtotal = lines.reduce((sum, entry) => sum + entry.liters, 0);
	const marginLiters = (subtotal * marginPercent) / 100;
	const requiredLiters = ceilLiters(subtotal + marginLiters);
	const availableLiters = input.cylinderL * (input.pressureBar - reserve);

	return { lines, marginLiters, requiredLiters, availableLiters, sufficient: requiredLiters <= availableLiters };
}