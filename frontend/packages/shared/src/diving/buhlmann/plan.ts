import { type GasMix, n2Fraction } from '../gas';
import { ceilingDepth, noDecompressionLimit } from './limits';
import { ambientPressure, type BuhlmannSettings, resolveSettings } from './settings';
import { loadSegment, type ProfileSegment, surfaceSaturatedTissues, type TissueState } from './tissues';

const MAX_STOP_MIN = 999;
const SAFETY_STOP_DEPTH_M = 5;
const SAFETY_STOP_MIN = 3;

export interface PlanInput {
	depthM: number;
	bottomTimeMin: number;
	gas: GasMix;
	settings?: Partial<BuhlmannSettings>;
	initialTissues?: TissueState;
	safetyStop?: boolean;
}

export type PlanPhase = 'DESCENT' | 'BOTTOM' | 'ASCENT' | 'DECO_STOP' | 'SAFETY_STOP';

export interface PlanStep {
	phase: PlanPhase;
	startDepthM: number;
	endDepthM: number;
	durationMin: number;
	runtimeMin: number;
}

export interface DecoStop {
	depthM: number;
	durationMin: number;
}

export interface DivePlan {
	settings: BuhlmannSettings;
	ndlAtBottomMin: number;
	isDecompression: boolean;
	decoStops: DecoStop[];
	steps: PlanStep[];
	totalRuntimeMin: number;
	ttsMin: number;
	tissuesAtSurface: TissueState;
	bottomPpO2Bar: number;
}

export const planDive = (input: PlanInput): DivePlan => {
	const settings = resolveSettings(input.settings);
	const { depthM, bottomTimeMin, gas } = input;

	if(depthM <= 0) throw new RangeError('Depth must be positive');
	if(n2Fraction(gas) < 0) throw new RangeError('Invalid gas mix');

	const descentMin = depthM / settings.descentRateMPerMin;
	if(bottomTimeMin < descentMin) {
		throw new RangeError(`Bottom time must be at least the descent time (${descentMin.toFixed(1)} min)`);
	}

	const steps: PlanStep[] = [];
	let tissues = input.initialTissues ?? surfaceSaturatedTissues(settings);
	let runtime = 0;

	const addStep = (phase: PlanPhase, startDepthM: number, endDepthM: number, durationMin: number): void => {
		if(durationMin <= 0) return;

		const segment: ProfileSegment = { startDepthM, endDepthM, durationMin, gas };
		tissues = loadSegment(tissues, segment, settings);
		runtime += durationMin;

		const last = steps.at(-1);
		if(last?.phase === phase && last.endDepthM === startDepthM && startDepthM === endDepthM) {
			last.durationMin += durationMin;
			last.runtimeMin = runtime;
		} else {
			steps.push({ phase, startDepthM, endDepthM, durationMin, runtimeMin: runtime });
		}
	}

	addStep('DESCENT', 0, depthM, descentMin);
	const ndlAtBottomMin = noDecompressionLimit(tissues, depthM, gas, settings);
	addStep('BOTTOM', depthM, depthM, bottomTimeMin - descentMin);

	const bottomEndRuntime = runtime;

	const { stopIntervalM, lastStopDepthM, ascentRateMPerMin, gfLow, gfHigh } = settings;
	const decoStops: DecoStop[] = [];

	const needsDecompression = ceilingDepth(tissues, gfHigh, settings) > 0;
	const firstStopDepth = needsDecompression
		? Math.max(lastStopDepthM, Math.ceil(ceilingDepth(tissues, gfLow, settings) / stopIntervalM) * stopIntervalM)
		: 0;

	if(firstStopDepth > 0) {
		const gfAt = (stopDepth: number): number => gfHigh + ((gfLow - gfHigh) * stopDepth) / firstStopDepth;

		let currentDepth = depthM;
		let stopDepth = Math.min(firstStopDepth, depthM);

		while(stopDepth > 0) {
			addStep('ASCENT', currentDepth, stopDepth, (currentDepth - stopDepth) / ascentRateMPerMin);
			currentDepth = stopDepth;

			const nextDepth = stopDepth <= lastStopDepthM ? 0 : Math.max(lastStopDepthM, stopDepth - stopIntervalM);
			let stopMin = 0;

			while(ceilingDepth(tissues, gfAt(nextDepth), settings) > nextDepth) {
				if(stopMin >= MAX_STOP_MIN) {
					throw new RangeError(`Decompression stop at ${stopDepth} m exceeds ${MAX_STOP_MIN} minutes`);
				}

				addStep('DECO_STOP', stopDepth, stopDepth, 1);
				stopMin += 1;
			}

			if(stopMin > 0) {
				decoStops.push({ depthM: stopDepth, durationMin: stopMin });
			}

			stopDepth = nextDepth;
		}

		addStep('ASCENT', currentDepth, 0, currentDepth / ascentRateMPerMin);
	} else if((input.safetyStop ?? true) && depthM > SAFETY_STOP_DEPTH_M) {
		addStep('ASCENT', depthM, SAFETY_STOP_DEPTH_M, (depthM - SAFETY_STOP_DEPTH_M) / ascentRateMPerMin);
		addStep('SAFETY_STOP', SAFETY_STOP_DEPTH_M, SAFETY_STOP_DEPTH_M, SAFETY_STOP_MIN);
		addStep('ASCENT', SAFETY_STOP_DEPTH_M, 0, SAFETY_STOP_DEPTH_M / ascentRateMPerMin);
	} else {
		addStep('ASCENT', depthM, 0, depthM / ascentRateMPerMin);
	}

	return {
		settings,
		ndlAtBottomMin,
		isDecompression: decoStops.length > 0,
		decoStops,
		steps,
		totalRuntimeMin: runtime,
		ttsMin: runtime - bottomEndRuntime,
		tissuesAtSurface: tissues,
		bottomPpO2Bar: (gas.o2Percent / 100) * ambientPressure(depthM, settings),
	}
}