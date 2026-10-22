import type { MessageDescriptor } from '@lingui/core';
import { msg } from '@lingui/core/macro';
import { Trans, useLingui } from '@lingui/react/macro';
import { ChevronsUpDownIcon, LanguagesIcon, LogOutIcon, MonitorIcon, MoonIcon, SunIcon, UserIcon } from 'lucide-react';
import { Link } from 'react-router';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuSub, DropdownMenuSubContent, DropdownMenuSubTrigger, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar } from '@/components/ui/sidebar';

import { useAuth } from '@/contexts/auth-context';
import { type Language, useLanguage } from '@/contexts/language-context';
import { type Theme, useTheme } from '@/contexts/theme-context';

const THEMES: { value: Theme; label: MessageDescriptor; icon: typeof SunIcon }[] = [
	{ value: 'light', label: msg`Chiaro`, icon: SunIcon },
	{ value: 'dark', label: msg`Scuro`, icon: MoonIcon },
	{ value: 'system', label: msg`Sistema`, icon: MonitorIcon },
]

const isTheme = (value: unknown): value is Theme => THEMES.some(theme => theme.value === value);

const initials = (firstName: string, lastName: string): string =>
	`${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();

const UserMenu = () => {
	const { t } = useLingui();
	const { user, logout } = useAuth();
	const { theme, setTheme } = useTheme();
	const { language, setLanguage, languages } = useLanguage();
	const { isMobile } = useSidebar();

	if(!user) return null;

	const fullName = `${user.firstName} ${user.lastName}`;
	const isLanguage = (value: unknown): value is Language => languages.some(entry => entry.code === value);

	return (
		<SidebarMenu>
			<SidebarMenuItem>
				<DropdownMenu>
					<DropdownMenuTrigger render = { <SidebarMenuButton size = 'lg' className = 'data-popup-open:bg-sidebar-accent data-popup-open:text-sidebar-accent-foreground'/> }>
						<Avatar className = 'size-8 rounded-lg'>
							<AvatarFallback className = 'rounded-lg bg-primary text-primary-foreground'>
								{initials(user.firstName, user.lastName)}
							</AvatarFallback>
						</Avatar>
						<div className = 'grid flex-1 text-left text-sm leading-tight'>
							<span className = 'truncate font-medium'>{fullName}</span>
							<span className = 'truncate text-xs text-muted-foreground'>@{user.username}</span>
						</div>
						<ChevronsUpDownIcon className = 'ml-auto size-4'/>
					</DropdownMenuTrigger>
					<DropdownMenuContent className = 'w-(--anchor-width) min-w-56 rounded-lg' side = { isMobile ? 'bottom' : 'right' } align = 'end' sideOffset = { 4 }>
						<DropdownMenuGroup>
							<DropdownMenuLabel className = 'font-normal'>
								<div className = 'grid text-sm leading-tight'>
									<span className = 'truncate font-medium'>{fullName}</span>
									<span className = 'truncate text-xs text-muted-foreground'>{user.email}</span>
								</div>
							</DropdownMenuLabel>
						</DropdownMenuGroup>
						<DropdownMenuSeparator/>
						<DropdownMenuGroup>
							<DropdownMenuItem render = { <Link to = '/profile'/> }>
								<UserIcon/>
								<Trans>Profilo</Trans>
							</DropdownMenuItem>
							<DropdownMenuSub>
								<DropdownMenuSubTrigger>
									<SunIcon className = 'dark:hidden'/>
									<MoonIcon className = 'hidden dark:block'/>
									<Trans>Tema</Trans>
								</DropdownMenuSubTrigger>
								<DropdownMenuSubContent>
									<DropdownMenuRadioGroup value = { theme } onValueChange = { (value: unknown) => isTheme(value) && setTheme(value) }>
										{THEMES.map(({ value, label, icon: Icon }) => (
											<DropdownMenuRadioItem key = { value } value = { value }>
												<Icon/>
												{t(label)}
											</DropdownMenuRadioItem>
										))}
									</DropdownMenuRadioGroup>
								</DropdownMenuSubContent>
							</DropdownMenuSub>
							<DropdownMenuSub>
								<DropdownMenuSubTrigger>
									<LanguagesIcon/>
									<Trans>Lingua</Trans>
								</DropdownMenuSubTrigger>
								<DropdownMenuSubContent>
									<DropdownMenuRadioGroup value = { language } onValueChange = { (value: unknown) => isLanguage(value) && setLanguage(value) }>
										{languages.map(({ code, label }) => (
											<DropdownMenuRadioItem key = { code } value = { code } lang = { code }>
												{label}
											</DropdownMenuRadioItem>
										))}
									</DropdownMenuRadioGroup>
								</DropdownMenuSubContent>
							</DropdownMenuSub>
						</DropdownMenuGroup>
						<DropdownMenuSeparator/>
						<DropdownMenuItem onClick = { () => void logout() }>
							<LogOutIcon/>
							<Trans>Esci</Trans>
						</DropdownMenuItem>
					</DropdownMenuContent>
				</DropdownMenu>
			</SidebarMenuItem>
		</SidebarMenu>
	)
}

export default UserMenu;