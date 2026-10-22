import { defineConfig } from '@lingui/cli';

export default defineConfig({
    sourceLocale: 'it',
    locales: ['it', 'en', 'fr', 'es', 'de'],
    catalogs: [
        {
            path: '<rootDir>/src/locales/{locale}/messages',
            include: ['src'],
        },
    ],
});