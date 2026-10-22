import decompressionJson from './tables/decompression-air.json';
import residualNitrogenJson from './tables/residual-nitrogen.json';
import surfaceIntervalJson from './tables/surface-interval.json';
import { FAR_GROUPS, type FarGroup, isFarGroup } from './types';

export interface DecompressionRow {
	far: FarGroup;
	timeMin: number;
	decoMin: number;
}

export interface DecompressionColumn {
	depthM: number;
	noDecoLimitMin: number;
	rows: readonly DecompressionRow[];
}

export interface SurfaceIntervalRange {
	far: FarGroup;
	fromMin: number;
	toMin: number;
}

export type ResidualNitrogenEntry = number | 'NO_CHANGE';

const byFar = <T>(build: (far: FarGroup) => T): Record<FarGroup, T> => {
	const result = {} as Record<FarGroup, T>;
	FAR_GROUPS.forEach((far) => {
		result[far] = build(far);
	});

	return result;
}

const parseFar = (value: string): FarGroup => {
	if(!isFarGroup(value)) throw new Error(`Invalid FAR group in FIPSAS tables: ${value}`);
	return value;
}

export const parseHoursMinutes = (value: string): number => {
	const match = /^(\d+):([0-5]\d)$/.exec(value);
	
	if(!match) throw new Error(`Invalid h:mm value: ${value}`);
	
	return Number(match[1]) * 60 + Number(match[2]);
}

export const formatHoursMinutes = (minutes: number): string => {
	const total = Math.round(minutes);
	return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}

export const FIPSAS_DESCENT_RATE_M_PER_MIN: number = decompressionJson.descentRateMPerMin;
export const FIPSAS_ASCENT_RATE_M_PER_MIN: number = decompressionJson.ascentRateMPerMin;
export const FIPSAS_DECO_STOP_DEPTH_M: number = decompressionJson.decoStopDepthM;

export const FIPSAS_DECOMPRESSION_TABLE: readonly DecompressionColumn[] = decompressionJson.depths.map((column) => {
	const rows = column.rows.map((row): DecompressionRow => ({
		far: parseFar(row.far),
		timeMin: row.time ?? Infinity,
		decoMin: ('deco' in row ? row.deco : undefined) ?? 0,
	}));

	rows.slice(1).forEach((row, index) => {
		if(row.timeMin <= rows[index].timeMin || FAR_GROUPS.indexOf(row.far) <= FAR_GROUPS.indexOf(rows[index].far)) {
			throw new Error(`FIPSAS table 1, ${column.depthM} m: rows must increase in time and FAR`);
		}
	});

	return { depthM: column.depthM, noDecoLimitMin: column.noDecoLimit ?? Infinity, rows };
});

export const FIPSAS_TABLE_DEPTHS_M: readonly number[] = FIPSAS_DECOMPRESSION_TABLE.map((column) => column.depthM);

export const FIPSAS_CONTINUATION_UNDER_MIN: number = parseHoursMinutes(surfaceIntervalJson.continuationUnder);

export const FIPSAS_SURFACE_INTERVAL_TABLE: Readonly<Record<FarGroup, readonly SurfaceIntervalRange[]>> = byFar(
	(initialFar) => {
		const ranges = surfaceIntervalJson.byInitialFar[initialFar].map((range): SurfaceIntervalRange => ({
			far: parseFar(range.far),
			fromMin: parseHoursMinutes(range.from),
			toMin: parseHoursMinutes(range.to),
		}));

		ranges.forEach((range, index) => {
			const expectedFrom = index === 0 ? FIPSAS_CONTINUATION_UNDER_MIN : ranges[index - 1].toMin + 1;
			if(range.fromMin !== expectedFrom || range.toMin < range.fromMin) {
				throw new Error(`FIPSAS table 2, initial FAR ${initialFar}: ranges must be contiguous`);
			}
		});

		return ranges;
	},
);

export const FIPSAS_RESIDUAL_NITROGEN_TABLE: Readonly<Record<FarGroup, ReadonlyMap<number, ResidualNitrogenEntry>>> = byFar(
	(far) => {
		const entries = Object.entries(residualNitrogenJson.byFar[far]).map(([depth, value]): [number, ResidualNitrogenEntry] => {
			const depthM = Number(depth);
			if(!FIPSAS_TABLE_DEPTHS_M.includes(depthM)) throw new Error(`FIPSAS table 3: unknown depth ${depth}`);
			if(value !== 'NO_CHANGE' && typeof value !== 'number') throw new Error(`FIPSAS table 3: invalid value at ${far}/${depth}`);
			return [depthM, value];
		});

		return new Map(entries);
	},
);