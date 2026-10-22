import { Trans } from '@lingui/react/macro';
import { ArrowLeftIcon, HomeIcon } from 'lucide-react';
import { useNavigate } from 'react-router';

import Bubbles from '@/components/common/bubbles';
import { Button } from '@/components/ui/button';

const canGoBack = (): boolean => {
	const state: unknown = window.history.state;
	return typeof state === 'object' && state !== null && 'idx' in state && typeof state.idx === 'number' && state.idx > 0;
}

const NotFoundPage = () => {
	const navigate = useNavigate();

	return (
		<main className = 'relative flex min-h-svh flex-col items-center justify-center overflow-hidden bg-linear-to-b from-background to-accent/60 px-6 text-center'>
			<Bubbles bubbleClassName = 'border-primary/30 bg-primary/5'/>
			<div className = 'relative flex max-w-md flex-col items-center gap-4'>
				<p className = 'font-mono text-sm text-muted-foreground'><Trans>Profondità: 404 m · fuori tabella</Trans></p>
				<h1 className = 'text-7xl font-semibold tracking-tight text-primary sm:text-8xl'>404</h1>
				<h2 className = 'text-2xl font-semibold tracking-tight'><Trans>Sei sceso troppo a fondo</Trans></h2>
				<p className = 'text-muted-foreground text-pretty'><Trans>Questa pagina non esiste o è stata spostata. Risali con calma e riparti da un punto sicuro.</Trans></p>
				<div className = 'mt-2 flex flex-wrap justify-center gap-3'>
					<Button className = 'flex gap-2 cursor-pointer' onClick = { () => navigate('/') }>
						<HomeIcon/>
                        <Trans>Torna alla home</Trans>
					</Button>
					{canGoBack() && (
						<Button className = 'cursor-pointer' variant = 'outline' onClick = { () => void navigate(-1) }>
							<ArrowLeftIcon/>
							<Trans>Indietro</Trans>
						</Button>
					)}
				</div>
			</div>
		</main>
	)
}

export default NotFoundPage;