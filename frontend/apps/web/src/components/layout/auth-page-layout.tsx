import type { ReactNode } from 'react';
import { Link } from 'react-router';

import OceanIllustration from '@/components/common/ocean-illustration';

import { cn } from '@/lib/utils';

interface AuthPageLayoutProps {
	title: string;
	description: ReactNode;
	children: ReactNode;
	footer?: ReactNode;
	illustration?: boolean;
}

const AuthPageLayout = ({ title, description, children, footer, illustration = true }: AuthPageLayoutProps) => {
	return (
		<div className = { cn('grid min-h-svh', illustration && 'lg:grid-cols-2') }>
			<div className = 'flex flex-col p-6 md:p-10'>
				<Link to = '/' className = 'self-start font-semibold tracking-tight'>
					Bubbles
				</Link>
				<main className = 'flex flex-1 items-center justify-center py-10'>
					<div className = 'flex w-full max-w-sm flex-col gap-6'>
						<div className = 'flex flex-col gap-1.5'>
							<h1 className = 'text-2xl font-semibold tracking-tight'>{title}</h1>
							<p className = 'text-sm text-muted-foreground text-pretty'>{description}</p>
						</div>
						{children}
						{footer && <div className = 'text-center text-sm text-muted-foreground'>{footer}</div>}
					</div>
				</main>
			</div>
			{illustration && <OceanIllustration className = 'hidden lg:sticky lg:top-0 lg:block lg:h-svh'/>}
		</div>
	)
}

export default AuthPageLayout;