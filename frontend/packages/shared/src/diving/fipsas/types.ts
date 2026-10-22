export const FAR_GROUPS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'Z'] as const;

export type FarGroup = (typeof FAR_GROUPS)[number];

export const isFarGroup = (value: string): value is FarGroup => 
	(FAR_GROUPS as readonly string[]).includes(value);

export const compareFar = (a: FarGroup, b: FarGroup): number => 
	FAR_GROUPS.indexOf(a) - FAR_GROUPS.indexOf(b);

export type FipsasErrorCode =
	| 'INVALID_INPUT'
	| 'DEPTH_OUT_OF_TABLE'
	| 'TIME_OUT_OF_TABLE'
	| 'CONTINUATION_OF_PREVIOUS_DIVE'
	| 'REPETITIVE_NOT_FEASIBLE';

export class FipsasError extends Error {
	readonly code: FipsasErrorCode;

	constructor(code: FipsasErrorCode, message: string) {
		super(message);
		
		this.name = 'FipsasError';
		this.code = code;
	}
}

export const FIPSAS_DISCLAIMER =
	'Questa procedura vuole essere, semplicemente, uno strumento di verifica del calcolo delle immersioni in curva di ' +
	'sicurezza secondo lo standard FIPSAS/DAN 2017; non sostituisce la partecipazione ad un corso ove vengono insegnate ' +
	'le corrette metodologie di calcolo e di immersione. Non si risponde della correttezza dei calcoli.';