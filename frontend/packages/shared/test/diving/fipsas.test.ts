import { describe, expect, it } from 'vitest';

import {
	calculateFipsasDive,
	decompressionRow,
	FAR_GROUPS,
	farAfterSurfaceInterval,
	FIPSAS_DECOMPRESSION_TABLE,
	FIPSAS_RESIDUAL_NITROGEN_TABLE,
	FipsasError,
	type FipsasErrorCode,
	fipsasTableDepth,
	remainingNoDecoTime,
} from '../../src/diving';

const expectFipsasError = (action: () => unknown, code: FipsasErrorCode): void => {
	try {
		action();
	} catch(error) {
		expect(error).toBeInstanceOf(FipsasError);
		expect((error as FipsasError).code).toBe(code);
		
		return;
	}

	throw new Error(`Expected FipsasError ${code}`);
}

describe('tables data', () => {
	it('the no-deco limit of each depth is the last row without decompression', () => {
		FIPSAS_DECOMPRESSION_TABLE.forEach((column) => {
			const lastInCurve = column.rows.filter((row) => row.decoMin === 0).at(-1);
			expect(lastInCurve?.timeMin).toBe(column.noDecoLimitMin);
			column.rows.filter((row) => row.timeMin > column.noDecoLimitMin).forEach((row) => expect(row.decoMin).toBeGreaterThan(0));
		});
	});

	it('residual nitrogen grows with the FAR group at every depth', () => {
		FIPSAS_DECOMPRESSION_TABLE.forEach(({ depthM }) => {
			const values = FAR_GROUPS.map((far) => FIPSAS_RESIDUAL_NITROGEN_TABLE[far].get(depthM))
				.filter((value): value is number => typeof value === 'number');
			values.slice(1).forEach((value, index) => expect(value).toBeGreaterThan(values[index]));
		});
	});
});

describe('table 1', () => {
	it('rounds depth up to the next column', () => {
		expect(fipsasTableDepth(18)).toBe(18);
		expect(fipsasTableDepth(18.5)).toBe(21);
		expect(fipsasTableDepth(3)).toBe(6);
		expectFipsasError(() => fipsasTableDepth(46), 'DEPTH_OUT_OF_TABLE');
	});

	it('picks the first row with an equal or greater time', () => {
		expect(decompressionRow(18, 30)).toEqual({ far: 'F', timeMin: 33, decoMin: 0 });
		expect(decompressionRow(18, 63)).toEqual({ far: 'K', timeMin: 63, decoMin: 0 });
		expect(decompressionRow(18, 64)).toEqual({ far: 'L', timeMin: 70, decoMin: 7 });
	});

	it('skips the empty cells of the card', () => {
		expect(decompressionRow(18, 71)).toEqual({ far: 'N', timeMin: 80, decoMin: 14 });
		expect(decompressionRow(30, 26)).toEqual({ far: 'J', timeMin: 30, decoMin: 3 });
		expect(decompressionRow(45, 2)).toEqual({ far: 'B', timeMin: 3, decoMin: 0 });
	});

	it('6 m is unlimited', () => {
		expect(decompressionRow(6, 1000)).toEqual({ far: 'L', timeMin: Infinity, decoMin: 0 });
	});

	it('uses the next row with risk factors', () => {
		expect(decompressionRow(18, 30, true).far).toBe('G');
		expectFipsasError(() => decompressionRow(45, 11, true), 'TIME_OUT_OF_TABLE');
	});

	it('rejects times beyond the table', () => {
		expectFipsasError(() => decompressionRow(24, 46), 'TIME_OUT_OF_TABLE');
	});
});

describe('table 2', () => {
	it('finds the FAR after the surface interval', () => {
		expect(farAfterSurfaceInterval('F', 30)).toBe('F');
		expect(farAfterSurfaceInterval('F', 90)).toBe('E');
		expect(farAfterSurfaceInterval('Z', 15 * 60 + 50)).toBe('A');
	});

	it('beyond the table the next dive is not repetitive', () => {
		expect(farAfterSurfaceInterval('A', 141)).toBeNull();
		expect(farAfterSurfaceInterval('Z', 15 * 60 + 51)).toBeNull();
	});

	it('under 10 minutes it is a continuation of the previous dive', () => {
		expectFipsasError(() => farAfterSurfaceInterval('C', 9), 'CONTINUATION_OF_PREVIOUS_DIVE');
	});
});

describe('table 3', () => {
	it('remaining time in curve matches the card', () => {
		const card: [Parameters<typeof remainingNoDecoTime>[0], number, number][] = [
			['A', 9, 353], ['A', 45, 4], ['B', 45, 2], ['C', 45, 0], ['C', 42, 2], ['D', 39, 1], ['E', 36, 1],
			['F', 33, 2], ['G', 30, 2], ['H', 27, 4], ['I', 24, 1], ['J', 18, 5], ['K', 15, 11], ['L', 15, 2],
			['M', 12, 27], ['N', 12, 11], ['O', 9, 63], ['H', 9, 263], ['E', 12, 118],
		];

		card.forEach(([far, depth, expected]) => expect(remainingNoDecoTime(far, depth), `${far} ${depth} m`).toBe(expected));
	});

	it('6 m is unlimited', () => {
		expect(remainingNoDecoTime('K', 6)).toBe(Infinity);
		expect(remainingNoDecoTime('Z', 6)).toBe(Infinity);
	});
});

describe('single dive', () => {
	it('within the no-deco limit', () => {
		const result = calculateFipsasDive({ depthM: 18, bottomTimeMin: 30 });

		expect(result.exitFar).toBe('F');
		expect(result.withinNoDecoLimit).toBe(true);
		expect(result.maxBottomTimeInCurveMin).toBe(63);
		expect(result.stops).toEqual([{ kind: 'SAFETY_STOP', depthM: 6, durationMin: 3 }]);
		expect(result.ascentTimeMin).toBeCloseTo(12 / 9 + 3 + 2, 9);
		expect(result.warnings).toEqual([]);
	});

	it('beyond the no-deco limit: deco stop then safety stop, no deep stop', () => {
		const result = calculateFipsasDive({ depthM: 30, bottomTimeMin: 28 });

		expect(result.exitFar).toBe('J');
		expect(result.decoStopMin).toBe(3);
		expect(result.stops).toEqual([
			{ kind: 'DECO_STOP', depthM: 6, durationMin: 3 },
			{ kind: 'SAFETY_STOP', depthM: 6, durationMin: 3 },
		]);
		expect(result.ascentTimeMin).toBeCloseTo(24 / 9 + 3 + 3 + 2, 9);
		expect(result.warnings).toContain('DECOMPRESSION_REQUIRED');
	});

	it('air consumption counts both deco and safety stop', () => {
		const result = calculateFipsasDive({ depthM: 30, bottomTimeMin: 28, air: { cylinderL: 15, pressureBar: 200 } });

		expect(result.air?.lines.map((line) => [line.phase, line.liters])).toEqual([
			['BOTTOM', 2240], ['ASCENT', 150], ['DECO_STOP', 96], ['SAFETY_STOP', 96], ['SURFACING', 64],
		]);
		expect(result.air?.requiredLiters).toBe(2911);
		expect(result.warnings).toContain('INSUFFICIENT_AIR');
	});

	it('deep stop only beyond 18 m', () => {
		expect(calculateFipsasDive({ depthM: 18, bottomTimeMin: 30 }).stops.map((stop) => stop.kind)).toEqual(['SAFETY_STOP']);
		expect(calculateFipsasDive({ depthM: 19, bottomTimeMin: 30 }).stops[0]).toEqual({ kind: 'DEEP_STOP', depthM: 9.5, durationMin: 2.5 });
		expect(calculateFipsasDive({ depthM: 24, bottomTimeMin: 30, deepStop: false }).stops.map((stop) => stop.kind)).toEqual(['SAFETY_STOP']);
	});

	it('air consumption as in the course spreadsheet', () => {
		const result = calculateFipsasDive({ depthM: 18, bottomTimeMin: 30, air: { cylinderL: 15, pressureBar: 200 } });

		expect(result.air?.lines.map((line) => line.liters)).toEqual([1680, 59, 96, 64]);
		expect(result.air?.requiredLiters).toBe(2089);
		expect(result.air?.availableLiters).toBe(2250);
		expect(result.air?.sufficient).toBe(true);

		const intermediate = calculateFipsasDive({ depthM: 20, bottomTimeMin: 30, air: { cylinderL: 15, pressureBar: 200 } });
		expect(intermediate.tableDepthM).toBe(21);
		expect(intermediate.exitFar).toBe('G');
		expect(intermediate.stops[0]).toEqual({ kind: 'DEEP_STOP', depthM: 10, durationMin: 2.5 });
		expect(intermediate.ascentTimeMin).toBeCloseTo(14 / 9 + 3 + 2, 9);
		expect(intermediate.air?.lines.map((line) => line.liters)).toEqual([1650, 72, 100, 96, 64]);
		expect(intermediate.air?.requiredLiters).toBe(2181);

		const small = calculateFipsasDive({ depthM: 18, bottomTimeMin: 30, air: { cylinderL: 10, pressureBar: 200 } });
		expect(small.warnings).toContain('INSUFFICIENT_AIR');
	});
});

describe('repetitive dive', () => {
	it('adds residual nitrogen from tables 2 and 3', () => {
		// uscita F, 1:30 in superficie -> E; 15 m con E -> +35; 30 + 35 = 65 -> riga 71, J
		const result = calculateFipsasDive({
			depthM: 15,
			bottomTimeMin: 30,
			previousDive: { exitFar: 'F', surfaceIntervalMin: 90, maxDepthM: 18 },
		});

		expect(result.repetitive).toEqual({
			initialFar: 'F',
			surfaceIntervalMin: 90,
			isRepetitive: true,
			residualFar: 'E',
			residualNitrogenMin: 35,
			farUnchanged: false,
		});
		expect(result.tableTimeMin).toBe(65);
		expect(result.exitFar).toBe('J');
		expect(result.maxBottomTimeInCurveMin).toBe(57);
		expect(result.warnings).toEqual(['SHORT_SURFACE_INTERVAL']);
	});

	it('beyond table 2 it is calculated as a single dive', () => {
		const result = calculateFipsasDive({ depthM: 18, bottomTimeMin: 30, previousDive: { exitFar: 'A', surfaceIntervalMin: 180 } });

		expect(result.repetitive?.isRepetitive).toBe(false);
		expect(result.tableTimeMin).toBe(30);
		expect(result.exitFar).toBe('F');
	});

	it('warns when the repetitive dive is deeper or shorter than allowed', () => {
		const result = calculateFipsasDive({
			depthM: 21,
			bottomTimeMin: 10,
			previousDive: { exitFar: 'D', surfaceIntervalMin: 150, maxDepthM: 18 },
		});

		expect(result.warnings).toEqual(expect.arrayContaining(['SHORT_REPETITIVE_DIVE', 'REPETITIVE_DEEPER_THAN_PREVIOUS']));
	});

	it('the dark cells of table 3 are not feasible', () => {
		expectFipsasError(
			() => calculateFipsasDive({ depthM: 24, bottomTimeMin: 20, previousDive: { exitFar: 'K', surfaceIntervalMin: 30 } }),
			'REPETITIVE_NOT_FEASIBLE',
		);
	});

	it('** at 6 m keeps the FAR and the time is unlimited', () => {
		const result = calculateFipsasDive({ depthM: 6, bottomTimeMin: 60, previousDive: { exitFar: 'Z', surfaceIntervalMin: 30 } });

		expect(result.repetitive?.farUnchanged).toBe(true);
		expect(result.exitFar).toBe('Z');
		expect(result.maxBottomTimeInCurveMin).toBe(Infinity);
	});
});
