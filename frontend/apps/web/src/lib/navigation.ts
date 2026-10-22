import type { MessageDescriptor } from '@lingui/core';
import { msg } from '@lingui/core/macro';
import { AwardIcon, BackpackIcon, GaugeIcon, HomeIcon, type LucideIcon, MapPinIcon, TableIcon, UsersIcon, WavesIcon } from 'lucide-react';

export type NavLabel = MessageDescriptor | string;

export interface NavItem {
	title: NavLabel;
	to: string;
	icon: LucideIcon;
	end?: boolean;
}

export interface NavSection {
	label: MessageDescriptor;
	items: NavItem[];
}

export const NAV_SECTIONS: NavSection[] = [
	{
		label: msg`Logbook`,
		items: [
			{ title: msg`Home`, to: '/', icon: HomeIcon, end: true },
			{ title: msg`Immersioni`, to: '/dives', icon: WavesIcon },
			{ title: msg`Siti`, to: '/sites', icon: MapPinIcon },
			{ title: msg`Compagni`, to: '/buddies', icon: UsersIcon },
		],
	},
	{
		label: msg`Equipaggiamento`,
		items: [
			{ title: msg`Attrezzatura`, to: '/gear', icon: BackpackIcon },
			{ title: msg`Brevetti`, to: '/certifications', icon: AwardIcon },
		],
	},
	{
		label: msg`Pianificazione`,
		items: [
			{ title: 'FIPSAS', to: '/planner/fipsas', icon: TableIcon },
			{ title: 'Bühlmann', to: '/planner/buhlmann', icon: GaugeIcon },
		],
	},
]

export const isNavItemActive = (item: NavItem, pathname: string): boolean =>
	item.end ? pathname === item.to : pathname === item.to || pathname.startsWith(`${item.to}/`);