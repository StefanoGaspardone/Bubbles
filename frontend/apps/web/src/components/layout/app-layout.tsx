import { Outlet } from 'react-router';

import AppSidebar from '@/components/common/app-sidebar';
import { Separator } from '@/components/ui/separator';
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar';

const AppLayout = () => (
	<SidebarProvider>
		<AppSidebar/>
		<SidebarInset>
			<header className = 'sticky top-0 z-10 flex h-14 shrink-0 items-center gap-2 border-b bg-background/80 px-4 backdrop-blur'>
				<SidebarTrigger className = '-ml-1' />
				<Separator orientation = 'vertical' className = 'mr-2 data-[orientation=vertical]:h-4'/>
				<span className = 'text-sm font-medium md:hidden'>Bubbles</span>
			</header>
			<main className = 'mx-auto w-full max-w-6xl flex-1 p-4 md:p-6'>
				<Outlet/>
			</main>
		</SidebarInset>
	</SidebarProvider>
)

export default AppLayout;