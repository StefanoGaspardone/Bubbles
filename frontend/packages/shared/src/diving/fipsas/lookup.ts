import {
	type DecompressionColumn,
	type DecompressionRow,
	FIPSAS_CONTINUATION_UNDER_MIN,
	FIPSAS_DECOMPRESSION_TABLE,
	FIPSAS_RESIDUAL_NITROGEN_TABLE,
	FIPSAS_SURFACE_INTERVAL_TABLE,
	formatHoursMinutes,
	type ResidualNitrogenEntry,
} from './tables';
import { type FarGroup, FipsasError } from './types';

export const decompressionColumn = (depthM: number): DecompressionColumn => {
	if(depthM <= 0) throw new FipsasError('INVALID_INPUT', 'Depth must be positive');

	const column = FIPSAS_DECOMPRESSION_TABLE.find((candidate) => candidate.depthM >= depthM);
	if(!column) {
		const maxDepth = FIPSAS_DECOMPRESSION_TABLE.at(-1)?.depthM;
		throw new FipsasError('DEPTH_OUT_OF_TABLE', `Depth ${depthM} m is beyond the tables (max ${maxDepth} m)`);
	}

	return column;
}

export const fipsasTableDepth = (depthM: number): number => decompressionColumn(depthM).depthM;

export const decompressionRow = (depthM: number, timeMin: number, nextRow = false): DecompressionRow => {
	if(timeMin <= 0) throw new FipsasError('INVALID_INPUT', 'Time must be positive');

	const column = decompressionColumn(depthM);
	const index = column.rows.findIndex((row) => row.timeMin >= timeMin);

	if(index < 0) {
		throw new FipsasError('TIME_OUT_OF_TABLE', `${timeMin} min at ${column.depthM} m is beyond table 1`);
	}

	if(!nextRow) return column.rows[index];

	const next = column.rows[index + 1];
	if(next) return next;

	if(column.rows[index].timeMin === Infinity) return column.rows[index];

	throw new FipsasError('TIME_OUT_OF_TABLE', `${timeMin} min at ${column.depthM} m has no next row in table 1`);
}

export const farAfterSurfaceInterval = (initialFar: FarGroup, surfaceIntervalMin: number): FarGroup | null => {
	if(surfaceIntervalMin < 0) throw new FipsasError('INVALID_INPUT', 'Surface interval must not be negative');

	if(surfaceIntervalMin < FIPSAS_CONTINUATION_UNDER_MIN) {
		throw new FipsasError(
			'CONTINUATION_OF_PREVIOUS_DIVE',
			`A surface interval under ${formatHoursMinutes(FIPSAS_CONTINUATION_UNDER_MIN)} is a continuation of the previous dive`,
		);
	}

	const range = FIPSAS_SURFACE_INTERVAL_TABLE[initialFar].find((candidate) => surfaceIntervalMin <= candidate.toMin);
	return range?.far ?? null;
}

export const residualNitrogenEntry = (far: FarGroup, depthM: number): ResidualNitrogenEntry => {
	const tableDepthM = fipsasTableDepth(depthM);
	const entry = FIPSAS_RESIDUAL_NITROGEN_TABLE[far].get(tableDepthM);

	if(entry === undefined) {
		throw new FipsasError('REPETITIVE_NOT_FEASIBLE', `A repetitive dive to ${tableDepthM} m with FAR ${far} is not feasible`);
	}

	return entry;
}

export const remainingNoDecoTime = (far: FarGroup, depthM: number): number => {
	const entry = residualNitrogenEntry(far, depthM);
	if(entry === 'NO_CHANGE') return Infinity;

	return Math.max(0, decompressionColumn(depthM).noDecoLimitMin - entry);
}