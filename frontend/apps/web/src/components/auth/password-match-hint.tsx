import { Trans } from '@lingui/react/macro';
import { CheckIcon, XIcon } from 'lucide-react';

import { cn } from '@/lib/utils';

interface PasswordMatchHintProps {
	id: string;
	matches: boolean;
}

const PasswordMatchHint = ({ id, matches }: PasswordMatchHintProps) => {
	return (
		<p id = { id } aria-live = 'polite' className = { cn('flex items-center gap-1.5 text-sm', matches ? 'text-success' : 'text-destructive') }>
			{matches ? (
				<>
					<CheckIcon className = 'size-4' aria-hidden = 'true'/>
					<Trans>Le password coincidono</Trans>
				</>
			) : (
				<>
					<XIcon className = 'size-4' aria-hidden = 'true'/>
					<Trans>Le password non coincidono</Trans>
				</>
			)}
		</p>
	)
}

export default PasswordMatchHint;