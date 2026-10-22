import { Trans } from '@lingui/react/macro';

import Bubbles from '@/components/common/bubbles';

import { cn } from '@/lib/utils';

interface OceanIllustrationProps {
	className?: string;
}

const OceanIllustration = ({ className }: OceanIllustrationProps) => {
	return (
		<div className = { cn('relative overflow-hidden bg-linear-to-b from-[oklch(0.68_0.11_210)] via-[oklch(0.42_0.1_235)] to-[oklch(0.2_0.05_250)]', className) } aria-hidden = 'true'>
			<div className = 'absolute inset-x-0 top-0 h-2/3 bg-[repeating-linear-gradient(100deg,transparent_0_60px,oklch(1_0_0/0.06)_60px_90px)] mask-b-from-10% mask-b-to-100%'/>
			<Bubbles/>
			<div className = 'absolute inset-x-0 bottom-0 p-10 text-white'>
				<p className = 'max-w-sm text-2xl font-semibold leading-snug text-balance'><Trans>Ogni immersione merita di essere ricordata.</Trans></p>
				<p className = 'mt-2 text-sm text-white/70'><Trans>Logbook, pianificazione e attrezzatura in un unico posto.</Trans></p>
			</div>
		</div>
	)
}

export default OceanIllustration;