export interface GasMix {
	o2Percent: number;
	hePercent: number;
}

export const AIR: GasMix = { o2Percent: 21, hePercent: 0 };

export const nitrox = (o2Percent: number): GasMix => {
	if(o2Percent < 21 || o2Percent > 100) {
		throw new RangeError('Nitrox O2 percentage must be between 21 and 100');
	}

	return { o2Percent, hePercent: 0 };
}

export const n2Fraction = (gas: GasMix): number =>
	1 - gas.o2Percent / 100 - gas.hePercent / 100;

export const isAir = (gas: GasMix): boolean =>
	gas.o2Percent === 21 && gas.hePercent === 0;

export const gasLabel = (gas: GasMix): string => {
	if(isAir(gas)) return 'Aria';
	if(gas.hePercent === 0) return `EAN${gas.o2Percent}`;
	
    return `Tx ${gas.o2Percent}/${gas.hePercent}`;
}