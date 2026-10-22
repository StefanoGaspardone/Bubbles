import type { MessageDescriptor } from '@lingui/core';
import { msg } from '@lingui/core/macro';
import { Trans, useLingui } from '@lingui/react/macro';
import { ExternalLinkIcon, WavesIcon } from 'lucide-react';
import { Link } from 'react-router';

import Bubbles from '@/components/common/bubbles';

import { cn } from '@/lib/utils';

const AUTHOR = 'Stefano Gaspardone';
const AUTHOR_URL = 'https://github.com/StefanoGaspardone';
const GITHUB_URL = 'https://github.com/StefanoGaspardone/Bubbles';

type Label = MessageDescriptor | string;

interface FooterLink {
    label: Label;
    to: string;
    external?: boolean;
}

interface FooterColumn {
    title: MessageDescriptor;
    links: FooterLink[];
}

const COLUMNS: FooterColumn[] = [
    {
        title: msg`App`,
        links: [
            { label: msg`Accedi`, to: '/login' },
            { label: msg`Registrati`, to: '/signup' },
        ],
    },
    {
        title: msg`Risorse`,
        links: [
            { label: 'FIPSAS', to: 'https://www.fipsas.it', external: true },
            { label: 'DAN Europe', to: 'https://www.daneurope.org', external: true },
        ],
    },
    {
        title: msg`Progetto`,
        links: [
            { label: 'GitHub', to: GITHUB_URL, external: true },
        ],
    },
]

const linkClassName = 'inline-flex items-center gap-1 text-sm text-white/70 transition-colors hover:text-white focus-visible:text-white focus-visible:outline-none focus-visible:underline';

const FooterLinkItem = ({ link }: { link: FooterLink }) => {
    const { t } = useLingui();
    const label = typeof link.label === 'string' ? link.label : t(link.label);

    if(link.external) {
        return (
            <a href = { link.to } target = '_blank' rel = 'noreferrer' className = { linkClassName }>
                {label}
                <ExternalLinkIcon className = 'size-3' aria-hidden = 'true'/>
                <span className = 'sr-only'><Trans>(si apre in una nuova scheda)</Trans></span>
            </a>
        )
    }

    return <Link to = { link.to } className = { linkClassName }>{label}</Link>;
}

interface SiteFooterProps {
    className?: string;
}

const SiteFooter = ({ className }: SiteFooterProps) => {
    const { t } = useLingui();
    const year = new Date().getFullYear();

    return (
        <footer className = { cn('relative overflow-hidden bg-[oklch(0.24_0.05_245)] text-white dark:bg-[oklch(0.12_0.03_248)] dark:border-t dark:border-white/10', className) }>
            <Bubbles className = 'opacity-40' bubbleClassName = 'border-white/30 bg-white/5'/>
            <div className = 'relative mx-auto max-w-6xl px-4 py-10'>
                <div className = 'flex flex-col gap-10 md:flex-row md:justify-between'>
                    <div className = 'flex max-w-xs flex-col gap-3'>
                        <Link to = '/' className = 'flex items-center gap-2 self-start font-semibold tracking-tight'>
                            <span className = 'flex size-8 items-center justify-center rounded-lg bg-white/10'>
                                <WavesIcon className = 'size-4'/>
                            </span>
                            Bubbles
                        </Link>
                        <p className = 'text-sm text-white/70 text-pretty'>
                            <Trans>Il logbook subacqueo per registrare le immersioni, pianificare la prossima e tenere in ordine attrezzatura e brevetti.</Trans>
                        </p>
                    </div>
                    <nav aria-label = { t`Link del footer` } className = 'grid grid-cols-2 gap-8 sm:grid-cols-3'>
                        {COLUMNS.map(column => (
                            <div key = { column.title.id } className = 'flex flex-col gap-3'>
                                <h2 className = 'text-xs font-semibold uppercase tracking-wider text-white/50'>{t(column.title)}</h2>
                                <ul className = 'flex flex-col gap-2'>
                                    {column.links.map(link => (
                                        <li key = { link.to }>
                                            <FooterLinkItem link = { link }/>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </nav>
                </div>
                <div className = 'mt-10 flex flex-col gap-2 border-t border-white/10 pt-10 text-xs text-white/50 sm:flex-row sm:justify-between'>
                    <p>
                        <Trans>
                            © {year} Bubbles · Realizzato da{' '}
                            <a href = { AUTHOR_URL } target = '_blank' rel = 'noreferrer' className = 'text-white/70 hover:text-white'>{AUTHOR}</a>
                        </Trans>
                    </p>
                    <p><Trans>Non sostituisce la formazione subacquea né il computer da immersione.</Trans></p>
                </div>
            </div>
        </footer>
    )
}

export default SiteFooter;