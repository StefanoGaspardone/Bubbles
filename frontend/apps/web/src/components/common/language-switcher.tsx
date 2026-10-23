import { useLingui } from '@lingui/react/macro';
import { ChevronDownIcon, LanguagesIcon } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

import { type Language, useLanguage } from '@/contexts/language-context';

import { cn } from '@/lib/utils';

interface LanguageSwitcherProps {
	className?: string;
	side?: 'top' | 'bottom';
	align?: 'start' | 'end';
}

const LanguageSwitcher = ({ className, side = 'bottom', align = 'end' }: LanguageSwitcherProps) => {
	const { t } = useLingui();
	const { language, setLanguage, languages } = useLanguage();

	const current = languages.find(entry => entry.code === language);
	const isLanguage = (value: unknown): value is Language => languages.some(entry => entry.code === value);

	return (
		<DropdownMenu>
			<DropdownMenuTrigger render = { <Button variant = 'ghost' size = 'sm' className = { cn('cursor-pointer gap-1.5', className) } aria-label = { t`Lingua` }/> }>
				<LanguagesIcon className = 'size-4'/>
				<span lang = { language }>{current?.label}</span>
				<ChevronDownIcon className = 'size-3.5 opacity-60'/>
			</DropdownMenuTrigger>
			<DropdownMenuContent side = { side } align = { align } className = 'min-w-40'>
				<DropdownMenuRadioGroup value = { language } onValueChange = { (value: unknown) => isLanguage(value) && setLanguage(value) }>
					{languages.map(({ code, label }) => (
						<DropdownMenuRadioItem key = { code } value = { code } lang = { code } className = 'cursor-pointer'>
							{label}
						</DropdownMenuRadioItem>
					))}
				</DropdownMenuRadioGroup>
			</DropdownMenuContent>
		</DropdownMenu>
	)
}

export default LanguageSwitcher;