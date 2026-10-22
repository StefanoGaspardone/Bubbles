import type { MessageDescriptor } from '@lingui/core';
import { msg } from '@lingui/core/macro';
import { Trans, useLingui } from '@lingui/react/macro';
import { AwardIcon, BookOpenIcon, CalculatorIcon, type LucideIcon, MapPinIcon, QrCodeIcon, WrenchIcon } from 'lucide-react';
import { useNavigate } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import SiteFooter from '@/components/common/site-footer';

interface Feature {
	icon: LucideIcon;
	title: MessageDescriptor;
	description: MessageDescriptor;
}

const FEATURES: Feature[] = [
	{
		icon: BookOpenIcon,
		title: msg`Logbook`,
		description: msg`Ogni immersione con profilo, bombole, gas, condizioni e note.`,
	},
	{
		icon: CalculatorIcon,
		title: msg`Pianificazione`,
		description: msg`Calcolo con le tabelle FIPSAS/DAN e con il modello Bühlmann.`,
	},
	{
		icon: QrCodeIcon,
		title: msg`Compagni`,
		description: msg`Aggiungi i compagni di immersione e fai confermare l'immersione con un QR code.`,
	},
	{
		icon: MapPinIcon,
		title: msg`Siti`,
		description: msg`I tuoi punti di immersione, con le immersioni fatte in ognuno.`,
	},
	{
		icon: WrenchIcon,
		title: msg`Attrezzatura`,
		description: msg`Inventario dell'attrezzatura e cosa hai usato in ogni immersione.`,
	},
	{
		icon: AwardIcon,
		title: msg`Brevetti`,
		description: msg`Tutti i brevetti in un posto, con le foto delle tessere.`,
	},
]

const LandingPage = () => {
    const { t } = useLingui();
    const navigate = useNavigate();

	return (
        <div className = 'flex min-h-svh flex-col'>
            <header className = 'border-b'>
                <div className = 'mx-auto flex h-14 max-w-6xl items-center justify-between px-4'>
                    <span className = 'font-semibold tracking-tight'>Bubbles</span>
                    <div className = 'flex gap-2'>
                        <Button variant = 'ghost' className = 'cursor-pointer' onClick = { () => navigate('/login') }>
                            <Trans>Accedi</Trans>
                        </Button>
                        <Button className = 'cursor-pointer' onClick = { () => navigate('/signup') }>
                            <Trans>Registrati</Trans>
                        </Button>
                    </div>
                </div>
            </header>
            <main className = 'flex-1'>
                <section className = 'mx-auto flex max-w-3xl flex-col items-center gap-6 px-4 py-20 text-center sm:py-28'>
                    <h1 className = 'text-4xl font-semibold tracking-tight text-balance sm:text-5xl'><Trans>Il tuo logbook subacqueo, sempre con te</Trans></h1>
                    <p className = 'max-w-xl text-lg text-muted-foreground text-pretty'><Trans>Registra le immersioni, pianifica la prossima e tieni in ordine attrezzatura e brevetti.</Trans></p>
                    <div className = 'flex flex-wrap justify-center gap-3'>
                        <Button size = 'lg' className = 'cursor-pointer' onClick = { () => navigate('/signup') }>
                            <Trans>Inizia ora</Trans>
                        </Button>
                        <Button size = 'lg' variant = 'outline' className = 'cursor-pointer' onClick = { () => navigate('/login') }>
                            <Trans>Ho già un account</Trans>
                        </Button>
                    </div>
                </section>
                <section className = 'mx-auto grid max-w-6xl gap-4 px-4 pb-20 sm:grid-cols-2 lg:grid-cols-3'>
                    {FEATURES.map(({ icon: Icon, title, description }) => (
                        <Card key = { title.id }>
                            <CardHeader>
                                <div className = 'mb-2 flex size-10 items-center justify-center rounded-md bg-accent text-accent-foreground'>
                                    <Icon className = 'size-5'/>
                                </div>
                                <CardTitle>{t(title)}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <CardDescription>{t(description)}</CardDescription>
                            </CardContent>
                        </Card>
                    ))}
                </section>
            </main>
            <SiteFooter/>
        </div>
    )
}

export default LandingPage;