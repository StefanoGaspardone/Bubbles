import { calculateAir, type FipsasAirInput, type FipsasAirResult } from './air';
import { decompressionColumn, decompressionRow, farAfterSurfaceInterval, residualNitrogenEntry } from './lookup';
import { type DecompressionRow, FIPSAS_ASCENT_RATE_M_PER_MIN, FIPSAS_DECO_STOP_DEPTH_M } from './tables';
import { compareFar, type FarGroup, FipsasError } from './types';

export const FIPSAS_DEEP_STOP_MIN = 2.5;
export const FIPSAS_DEEP_STOP_OVER_DEPTH_M = 18;
export const FIPSAS_SAFETY_STOP_MIN = 3;
export const FIPSAS_SURFACING_RATE_M_PER_MIN = 3;
export const FIPSAS_RECOMMENDED_SURFACE_INTERVAL_MIN = 120;
export const FIPSAS_MIN_REPETITIVE_DIVE_MIN = 15;

export interface FipsasPreviousDive {
	exitFar: FarGroup;
	surfaceIntervalMin: number;
	maxDepthM?: number;
}

export interface FipsasDiveInput {
	depthM: number;
	bottomTimeMin: number;
	previousDive?: FipsasPreviousDive;
	riskFactors?: boolean;
	deepStop?: boolean;
	air?: FipsasAirInput;
}

export interface FipsasRepetitiveInfo {
	initialFar: FarGroup;
	surfaceIntervalMin: number;
	isRepetitive: boolean;
	residualFar: FarGroup | null;
	residualNitrogenMin: number;
	farUnchanged: boolean;
}

export type FipsasStopKind = 'DEEP_STOP' | 'DECO_STOP' | 'SAFETY_STOP';

export interface FipsasStop {
	kind: FipsasStopKind;
	depthM: number;
	durationMin: number;
}

export type FipsasWarningCode =
	| 'DECOMPRESSION_REQUIRED'
	| 'SHORT_SURFACE_INTERVAL'
	| 'SHORT_REPETITIVE_DIVE'
	| 'REPETITIVE_DEEPER_THAN_PREVIOUS'
	| 'INSUFFICIENT_AIR';

export interface FipsasDiveResult {
	tableDepthM: number;
	repetitive: FipsasRepetitiveInfo | null;
	tableTimeMin: number;
	tableRow: DecompressionRow;
	exitFar: FarGroup;
	withinNoDecoLimit: boolean;
	noDecoLimitMin: number;
	maxBottomTimeInCurveMin: number;
	decoStopMin: number;
	stops: FipsasStop[];
	ascentTimeMin: number;
	totalDiveTimeMin: number;
	air: FipsasAirResult | null;
	warnings: FipsasWarningCode[];
}

export const calculateFipsasDive = (input: FipsasDiveInput): FipsasDiveResult => {
	const { depthM, bottomTimeMin, previousDive } = input;

	if(depthM <= 0) throw new FipsasError('INVALID_INPUT', 'Depth must be positive');
	if(bottomTimeMin <= 0) throw new FipsasError('INVALID_INPUT', 'Bottom time must be positive');

	const column = decompressionColumn(depthM);
	const warnings: FipsasWarningCode[] = [];

	let repetitive: FipsasRepetitiveInfo | null = null;

	if(previousDive) {
		const residualFar = farAfterSurfaceInterval(previousDive.exitFar, previousDive.surfaceIntervalMin);
		const entry = residualFar ? residualNitrogenEntry(residualFar, depthM) : 0;

		repetitive = {
			initialFar: previousDive.exitFar,
			surfaceIntervalMin: previousDive.surfaceIntervalMin,
			isRepetitive: residualFar !== null,
			residualFar,
			residualNitrogenMin: typeof entry === 'number' ? entry : 0,
			farUnchanged: entry === 'NO_CHANGE',
		}

		if(previousDive.surfaceIntervalMin < FIPSAS_RECOMMENDED_SURFACE_INTERVAL_MIN) warnings.push('SHORT_SURFACE_INTERVAL');

		if(repetitive.isRepetitive) {
			if(bottomTimeMin < FIPSAS_MIN_REPETITIVE_DIVE_MIN) warnings.push('SHORT_REPETITIVE_DIVE');
			if(previousDive.maxDepthM !== undefined && depthM > previousDive.maxDepthM) {
				warnings.push('REPETITIVE_DEEPER_THAN_PREVIOUS');
			}
		}
	}

	const residualNitrogenMin = repetitive?.residualNitrogenMin ?? 0;
	const tableTimeMin = bottomTimeMin + residualNitrogenMin;

	let tableRow = decompressionRow(depthM, tableTimeMin, input.riskFactors ?? false);

	if(repetitive?.farUnchanged && repetitive.residualFar && compareFar(repetitive.residualFar, tableRow.far) > 0) {
		tableRow = { far: repetitive.residualFar, timeMin: Infinity, decoMin: 0 };
	}

	const withinNoDecoLimit = tableRow.decoMin === 0;
	if(!withinNoDecoLimit) warnings.push('DECOMPRESSION_REQUIRED');

	const maxBottomTimeInCurveMin = repetitive?.farUnchanged
		? Infinity
		: Math.max(0, column.noDecoLimitMin - residualNitrogenMin);

	const stopDepthM = Math.min(FIPSAS_DECO_STOP_DEPTH_M, depthM);
	const hasDeepStop = (input.deepStop ?? true) && withinNoDecoLimit && depthM > FIPSAS_DEEP_STOP_OVER_DEPTH_M;
	const deepStopMin = hasDeepStop ? Math.min(FIPSAS_DEEP_STOP_MIN, bottomTimeMin) : 0;

	const stops: FipsasStop[] = [];
	
	if(hasDeepStop) stops.push({ kind: 'DEEP_STOP', depthM: depthM / 2, durationMin: deepStopMin });
	if(tableRow.decoMin > 0) stops.push({ kind: 'DECO_STOP', depthM: stopDepthM, durationMin: tableRow.decoMin });
	
	stops.push({ kind: 'SAFETY_STOP', depthM: stopDepthM, durationMin: FIPSAS_SAFETY_STOP_MIN });

	const ascentToStopMin = (depthM - stopDepthM) / FIPSAS_ASCENT_RATE_M_PER_MIN;
	const surfacingMin = stopDepthM / FIPSAS_SURFACING_RATE_M_PER_MIN;
	const ascentTimeMin = ascentToStopMin + tableRow.decoMin + FIPSAS_SAFETY_STOP_MIN + surfacingMin;

	const air = input.air
		? calculateAir(
			{
				depthM,
				bottomMin: bottomTimeMin - deepStopMin,
				ascentMin: ascentToStopMin,
				stopDepthM,
				deepStopMin,
				decoMin: tableRow.decoMin,
				safetyStopMin: FIPSAS_SAFETY_STOP_MIN,
				surfacingMin,
			},
			input.air,
		)
		: null;

	if(air && !air.sufficient) warnings.push('INSUFFICIENT_AIR');

	return {
		tableDepthM: column.depthM,
		repetitive,
		tableTimeMin,
		tableRow,
		exitFar: tableRow.far,
		withinNoDecoLimit,
		noDecoLimitMin: column.noDecoLimitMin,
		maxBottomTimeInCurveMin,
		decoStopMin: tableRow.decoMin,
		stops,
		ascentTimeMin,
		totalDiveTimeMin: bottomTimeMin + ascentTimeMin,
		air,
		warnings,
	}
}