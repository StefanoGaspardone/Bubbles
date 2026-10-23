import { useLingui } from '@lingui/react/macro';
import { EyeIcon, EyeOffIcon } from 'lucide-react';
import { type ComponentProps, useState } from 'react';

import { Input } from '@/components/ui/input';

import { cn } from '@/lib/utils';

const PasswordInput = ({ className, ...props }: Omit<ComponentProps<typeof Input>, 'type'>) => {
	const { t } = useLingui();
	const [visible, setVisible] = useState(false);

	return (
		<div className = 'relative'>
			<Input type = { visible ? 'text' : 'password' } className = { cn('pr-10', className) } {...props}/>
			<button type = 'button' onClick = { () => setVisible(current => !current) } className = 'absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-md text-muted-foreground hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring cursor-pointer' aria-label = { visible ? t`Nascondi password` : t`Mostra password` } aria-pressed = { visible }>
				{visible ? <EyeOffIcon className = 'size-4'/> : <EyeIcon className = 'size-4'/>}
			</button>
		</div>
	);
}

export default PasswordInput;