import { useLingui } from '@lingui/react/macro';
import { WavesIcon } from 'lucide-react';
import { Link, useLocation } from 'react-router';

import UserMenu from '@/components/common/user-menu';
import {
	Sidebar,
	SidebarContent,
	SidebarFooter,
	SidebarGroup,
	SidebarGroupContent,
	SidebarGroupLabel,
	SidebarHeader,
	SidebarMenu,
	SidebarMenuButton,
	SidebarMenuItem,
	SidebarRail,
	useSidebar,
} from '@/components/ui/sidebar';

import { isNavItemActive, NAV_SECTIONS, type NavLabel } from '@/lib/navigation';

const AppSidebar = () => {
	const { t } = useLingui();
	const { pathname } = useLocation();
	const { isMobile, setOpenMobile } = useSidebar();

	const label = (value: NavLabel): string => typeof value === 'string' ? value : t(value);

	const handleNavigate = (): void => {
	    if(isMobile) setOpenMobile(false);
	}

	return (
		<Sidebar collapsible = 'icon'>
			<SidebarHeader>
				<SidebarMenu>
					<SidebarMenuItem>
						<SidebarMenuButton size = 'lg' render = { <Link to = '/' onClick = { handleNavigate }/> }>
							<div className = 'flex aspect-square size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground'>
								<WavesIcon className = 'size-4'/>
							</div>
							<span className = 'truncate font-semibold tracking-tight'>Bubbles</span>
						</SidebarMenuButton>
					</SidebarMenuItem>
				</SidebarMenu>
			</SidebarHeader>
			<SidebarContent>
				{NAV_SECTIONS.map(section => (
					<SidebarGroup key = { section.label.id }>
						<SidebarGroupLabel>{t(section.label)}</SidebarGroupLabel>
						<SidebarGroupContent>
							<SidebarMenu>
								{section.items.map(item => (
									<SidebarMenuItem key = { item.to }>
										<SidebarMenuButton isActive = { isNavItemActive(item, pathname) } tooltip = { label(item.title) } render = { <Link to = { item.to } onClick = { handleNavigate }/> }>
											<item.icon/>
											<span>{label(item.title)}</span>
										</SidebarMenuButton>
									</SidebarMenuItem>
								))}
							</SidebarMenu>
						</SidebarGroupContent>
					</SidebarGroup>
				))}
			</SidebarContent>
			<SidebarFooter>
				<UserMenu/>
			</SidebarFooter>
			<SidebarRail/>
		</Sidebar>
	)
}

export default AppSidebar;