import { AIR, type GasMix, n2Fraction } from '../gas';
import { WATER_VAPOUR_PRESSURE_BAR, ZHL16C_N2 } from './constants';
import { ambientPressure, type BuhlmannSettings } from './settings';

export interface TissueState {
	readonly n2: readonly number[];
}

export interface ProfileSegment {
	startDepthM: number;
	endDepthM: number;
	durationMin: number;
	gas: GasMix;
}

const LN2 = Math.log(2);

const inspiredN2 = (ambientPressureBar: number, gas: GasMix): number =>
	Math.max(0, ambientPressureBar - WATER_VAPOUR_PRESSURE_BAR) * n2Fraction(gas);

const ensureSupportedGas = (gas: GasMix): void => {
	if(gas.hePercent > 0) {
		throw new RangeError('Trimix is not supported yet: only air and nitrox');
	}
}

export const surfaceSaturatedTissues = (settings: BuhlmannSettings): TissueState => {
	const pressure = inspiredN2(settings.surfacePressureBar, AIR);
	return { n2: ZHL16C_N2.map(() => pressure) };
}

const haldane = (state: TissueState, depthM: number, durationMin: number, gas: GasMix, settings: BuhlmannSettings): TissueState => {
	const alveolar = inspiredN2(ambientPressure(depthM, settings), gas);

	return {
		n2: state.n2.map((pressure, index) => {
			const factor = 1 - Math.pow(2, -durationMin / ZHL16C_N2[index].halfTimeMin);
			return pressure + (alveolar - pressure) * factor;
		}),
	}
}

const schreiner = (state: TissueState, segment: ProfileSegment, settings: BuhlmannSettings): TissueState => {
	const { startDepthM, endDepthM, durationMin, gas } = segment;
	const alveolarStart = inspiredN2(ambientPressure(startDepthM, settings), gas);
	const alveolarEnd = inspiredN2(ambientPressure(endDepthM, settings), gas);
	const rate = (alveolarEnd - alveolarStart) / durationMin;

	return {
		n2: state.n2.map((pressure, index) => {
			const k = LN2 / ZHL16C_N2[index].halfTimeMin;
			return alveolarStart + rate * (durationMin - 1 / k) - (alveolarStart - pressure - rate / k) * Math.exp(-k * durationMin);
		}),
	}
}

export const loadSegment = (state: TissueState, segment: ProfileSegment, settings: BuhlmannSettings): TissueState => {
	ensureSupportedGas(segment.gas);

	if(segment.durationMin <= 0) return state;

	return segment.startDepthM === segment.endDepthM
		? haldane(state, segment.startDepthM, segment.durationMin, segment.gas, settings)
		: schreiner(state, segment, settings);
}

export const loadProfile = (state: TissueState, segments: readonly ProfileSegment[], settings: BuhlmannSettings): TissueState =>
	segments.reduce((current, segment) => loadSegment(current, segment, settings), state);

export const surfaceInterval = (state: TissueState, durationMin: number, settings: BuhlmannSettings): TissueState =>
	loadSegment(state, { startDepthM: 0, endDepthM: 0, durationMin, gas: AIR }, settings);

export interface LoggedSegment {
	type: 'DESCENT' | 'BOTTOM' | 'DEEP_STOP' | 'ASCENT' | 'DECO_STOP' | 'SAFETY_STOP' | 'SURFACE';
	depthM: number;
	durationSeconds: number;
}

const TRANSITION_TYPES = new Set<LoggedSegment['type']>(['DESCENT', 'ASCENT', 'SURFACE']);

export const profileFromLoggedSegments = (segments: readonly LoggedSegment[], gas: GasMix): ProfileSegment[] => {
	let previousDepth = 0;

	return segments.map((segment) => {
		const startDepthM = TRANSITION_TYPES.has(segment.type) ? previousDepth : segment.depthM;
		previousDepth = segment.depthM;

		return {
			startDepthM,
			endDepthM: segment.depthM,
			durationMin: segment.durationSeconds / 60,
			gas,
		}
	});
}