/* eslint-disable react-refresh/only-export-components */

import { i18n, type Messages } from '@lingui/core';
import { I18nProvider } from '@lingui/react';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import PageLoader from '@/components/common/page-loader';

export const LANGUAGES = [
    { code: 'it', label: 'Italiano' },
    { code: 'en', label: 'English' },
    { code: 'de', label: 'Deutsch' },
    { code: 'fr', label: 'Français' },
    { code: 'es', label: 'Español' },
] as const;

export type Language = (typeof LANGUAGES)[number]['code'];

export const DEFAULT_LANGUAGE: Language = 'it';

const STORAGE_KEY = 'language';

const isLanguage = (value: unknown): value is Language => LANGUAGES.some((language) => language.code === value);

const deviceLanguage = (): Language | null => {
    try {
        const preferred = navigator.languages?.length ? navigator.languages : [navigator.language];

        for(const tag of preferred) {
            const code = tag?.split('-')[0]?.toLowerCase();
            if(isLanguage(code)) return code;
        }
    } catch {
        // swallow
    }

    return null;
}

const initialLanguage = (): Language => {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if(isLanguage(stored)) return stored;
    } catch {
        // swallow
    }

    return deviceLanguage() ?? DEFAULT_LANGUAGE;
}

const importCatalog = async (language: Language): Promise<Messages> => {
    const { messages } = await import(`../locales/${language}/messages.po`);
    return messages;
}

interface LanguageContextType {
    language: Language;
    setLanguage: (language: Language) => void;
    languages: typeof LANGUAGES;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const LanguageProvider = ({ children }: { children: ReactNode }) => {
    const [language, setLanguageState] = useState<Language>(initialLanguage);
    const [isReady, setIsReady] = useState(false);

    useEffect(() => {
        let active = true;

        const activate = async () => {
            let target = language;
            let messages: Messages;

            try {
                messages = await importCatalog(target);
            } catch(error) {
                console.error(`Unable to load the "${target}" catalog`, error);
                target = DEFAULT_LANGUAGE;
                messages = await importCatalog(target);
            }

            if(!active) return;

            i18n.loadAndActivate({ locale: target, messages });
            document.documentElement.lang = target;
            setIsReady(true);
        }

        void activate();

        return () => {
            active = false;
        }
    }, [language]);

    const setLanguage = useCallback((next: Language) => {
        setLanguageState(next);

        try {
            localStorage.setItem(STORAGE_KEY, next);
        } catch {
            // swallow
        }
    }, []);

    const value = useMemo(() => ({ language, setLanguage, languages: LANGUAGES }), [language, setLanguage]);

    if(!isReady) return <PageLoader/>;

    return (
        <LanguageContext.Provider value = { value }>
            <I18nProvider i18n = { i18n }>
                {children}
            </I18nProvider>
        </LanguageContext.Provider>
    )
}

export const useLanguage = () => {
    const context = useContext(LanguageContext);

    if(!context) throw new Error('useLanguage must be used inside a LanguageProvider');

    return context;
}