export interface Compartment {
	halfTimeMin: number;
	a: number;
	b: number;
}

export const ZHL16C_N2: readonly Compartment[] = [
	{ halfTimeMin: 5.0, a: 1.1696, b: 0.5578 },
	{ halfTimeMin: 8.0, a: 1.0, b: 0.6514 },
	{ halfTimeMin: 12.5, a: 0.8618, b: 0.7222 },
	{ halfTimeMin: 18.5, a: 0.7562, b: 0.7825 },
	{ halfTimeMin: 27.0, a: 0.62, b: 0.8126 },
	{ halfTimeMin: 38.3, a: 0.5043, b: 0.8434 },
	{ halfTimeMin: 54.3, a: 0.441, b: 0.8693 },
	{ halfTimeMin: 77.0, a: 0.4, b: 0.891 },
	{ halfTimeMin: 109.0, a: 0.375, b: 0.9092 },
	{ halfTimeMin: 146.0, a: 0.35, b: 0.9222 },
	{ halfTimeMin: 187.0, a: 0.3295, b: 0.9319 },
	{ halfTimeMin: 239.0, a: 0.3065, b: 0.9403 },
	{ halfTimeMin: 305.0, a: 0.2835, b: 0.9477 },
	{ halfTimeMin: 390.0, a: 0.261, b: 0.9544 },
	{ halfTimeMin: 498.0, a: 0.248, b: 0.9602 },
	{ halfTimeMin: 635.0, a: 0.2327, b: 0.9653 },
]

export const WATER_VAPOUR_PRESSURE_BAR = 0.0627;
export const SEA_LEVEL_PRESSURE_BAR = 1.01325;