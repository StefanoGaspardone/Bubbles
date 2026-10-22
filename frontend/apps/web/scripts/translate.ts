import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { parseArgs } from 'node:util';

import { DeepLClient, GlossaryEntries, type MultilingualGlossaryInfo, type SourceLanguageCode, type TargetLanguageCode } from 'deepl-node';
import { po, type GetTextTranslation } from 'gettext-parser';

import linguiConfig from '../lingui.config.ts';

const ROOT = path.resolve(import.meta.dirname, '..');
const GLOSSARY_FILE = path.join(import.meta.dirname, 'i18n-glossary.json');
const REVIEW_FLAG = 'fuzzy';
const BATCH_SIZE = 50;
const CONTEXT = 'User interface texts of Bubbles, a scuba diving logbook and dive planning web app. Short labels, buttons and messages addressed informally to the user.';

const TARGET_CODES: Record<string, TargetLanguageCode> = {
	en: 'en-US',
	pt: 'pt-PT',
}

interface Glossary {
	keep: string[];
	terms: Record<string, Record<string, string>>;
}

const hasIcuSyntax = (text: string): boolean => /\{\s*\w+\s*,\s*(plural|select|selectordinal)\s*,/.test(text);

const escapeXml = (text: string): string =>
	text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');

const unescapeXml = (text: string): string =>
	text.replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('&quot;', '"').replaceAll('&apos;', '\'').replaceAll('&amp;', '&');

const escapeRegExp = (text: string): string => text.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);

const toDeepL = (message: string, keep: string[]): string => {
	let xml = escapeXml(message);

	for(const term of [...keep].sort((a, b) => b.length - a.length)) {
		const pattern = new RegExp(String.raw`(?<![\w>])${escapeRegExp(escapeXml(term))}(?![\w<])`, 'g');
		xml = xml.replace(pattern, (match) => `<k>${match}</k>`);
	}

	return xml
		.replace(/\{(\w+)\}/g, '<v id="$1"/>')
		.replace(/&lt;(\d+)\/&gt;/g, '<x id="$1"/>')
		.replace(/&lt;(\d+)&gt;/g, '<x id="$1">')
		.replace(/&lt;\/\d+&gt;/g, '</x>');
}

const fromDeepL = (xml: string): string => {
	const open: string[] = [];

	const restored = xml
		.replace(/<k>(.*?)<\/k>/g, '$1')
		.replace(/<v id="(\w+)"\s*\/>/g, '{$1}')
		.replace(/<x id="(\d+)"\s*\/>|<x id="(\d+)">|<\/x>/g, (_match, selfClosing: string | undefined, opening: string | undefined) => {
			if(selfClosing !== undefined) return `<${selfClosing}/>`;
			if(opening !== undefined) {
				open.push(opening);
				return `<${opening}>`;
			}
			return `</${open.pop() ?? ''}>`;
		});

	return unescapeXml(restored);
}

const samePlaceholders = (source: string, translated: string): boolean => {
	const placeholders = (text: string) => [...text.matchAll(/\{\w+\}|<\/?\d+\/?>/g)].map((match) => match[0]).sort((a, b) => a.localeCompare(b)).join();
	return placeholders(source) === placeholders(translated);
}

const catalogPath = (locale: string): string => {
	const pattern = linguiConfig.catalogs?.[0]?.path;
	if(!pattern) throw new Error('No catalog path in lingui.config.ts');

	return `${pattern.replace('<rootDir>', ROOT).replace('{locale}', locale)}.po`;
}

const missingEntries = (catalog: ReturnType<typeof po.parse>): GetTextTranslation[] =>
	Object.values(catalog.translations)
		.flatMap((context) => Object.values(context))
		.filter((entry) => entry.msgid !== '' && entry.msgstr.every((value) => value === ''));

const markForReview = (entry: GetTextTranslation): void => {
	const flags = entry.comments?.flag?.split(',').map((flag) => flag.trim()).filter(Boolean) ?? [];
	if(flags.includes(REVIEW_FLAG)) return;

	entry.comments = { ...entry.comments, flag: [...flags, REVIEW_FLAG].join(', ') } as GetTextTranslation['comments'];
}

const createGlossary = async (client: DeepLClient, glossary: Glossary, sourceLocale: string, locales: string[]): Promise<MultilingualGlossaryInfo | null> => {
	const dictionaries = locales
		.map((locale) => ({
			sourceLangCode: sourceLocale,
			targetLangCode: locale,
			terms: Object.fromEntries(
				Object.entries(glossary.terms)
					.filter(([, translations]) => translations[locale])
					.map(([term, translations]) => [term, translations[locale]]),
			),
		}))
		.filter(({ terms }) => Object.keys(terms).length > 0);

	if(dictionaries.length === 0) return null;

	return client.createMultilingualGlossary(
		`bubbles-i18n-${Date.now()}`,
		dictionaries.map(({ sourceLangCode, targetLangCode, terms }) => ({
			sourceLangCode,
			targetLangCode,
			entries: new GlossaryEntries({ entries: terms }),
		})),
	);
}

const translateLocale = async (
	client: DeepLClient,
	sourceLocale: string,
	locale: string,
	texts: string[],
	keep: string[],
	glossary: MultilingualGlossaryInfo | null,
): Promise<string[]> => {
	const targetLang = TARGET_CODES[locale] ?? (locale as TargetLanguageCode);
	const hasGlossary = glossary?.dictionaries.some((dictionary) => dictionary.targetLangCode === locale) ?? false;
	const results: string[] = [];

	for(let start = 0; start < texts.length; start += BATCH_SIZE) {
		const batch = texts.slice(start, start + BATCH_SIZE).map((text) => toDeepL(text, keep));

		const translated = await client.translateText(batch, sourceLocale as SourceLanguageCode, targetLang, {
			tagHandling: 'xml',
			ignoreTags: ['k', 'v'],
			formality: 'prefer_less',
			context: CONTEXT,
			...(hasGlossary && glossary ? { glossary } : {}),
		});

		results.push(...translated.map((result) => fromDeepL(result.text)));
	}

	return results;
}

const main = async (): Promise<void> => {
	const { values } = parseArgs({
		options: {
			locale: { type: 'string' },
			'dry-run': { type: 'boolean', default: false },
		},
	});

	const sourceLocale = linguiConfig.sourceLocale ?? 'it';
	const allLocales = (linguiConfig.locales ?? []).filter((locale) => locale !== sourceLocale);
	const locales = values.locale ? values.locale.split(',').map((locale) => locale.trim()).filter((locale) => allLocales.includes(locale)) : allLocales;
	const dryRun = values['dry-run'];

	if(locales.length === 0) {
		console.log('\n\t[INFO] [i18n][translate] No target locales');
		return;
	}

	const glossary = JSON.parse(await readFile(GLOSSARY_FILE, 'utf8')) as Glossary;

	const apiKey = process.env.DEEPL_API_KEY;
	if(!dryRun && !apiKey) {
		throw new Error('DEEPL_API_KEY is missing: add it to apps/web/.env (or run with --dry-run)');
	}

	const client = dryRun ? null : new DeepLClient(apiKey ?? '');
	let deeplGlossary: MultilingualGlossaryInfo | null = null;

	try {
		if(client) {
			try {
				deeplGlossary = await createGlossary(client, glossary, sourceLocale, locales);
			} catch(error) {
				console.warn(`\n\t[WARN] [i18n][translate] Glossary not created, translating without it: ${(error as Error).message}`);
			}
		}

		for(const locale of locales) {
			const file = catalogPath(locale);
			const catalog = po.parse(await readFile(file));
			const entries = missingEntries(catalog);

			const translatable = entries.filter((entry) => !hasIcuSyntax(entry.msgid));
			const manual = entries.filter((entry) => hasIcuSyntax(entry.msgid));

			console.log(`\n\t[INFO] [i18n][translate] ${locale}: ${translatable.length} to translate${manual.length ? `, ${manual.length} plural/select to translate by hand` : ''}`);
			manual.forEach((entry) => console.log(`\t\t- (manual) ${entry.msgid}`));

			if(translatable.length === 0) continue;

			if(!client) {
				translatable.forEach((entry) => console.log(`\t\t- ${entry.msgid}`));
				continue;
			}

			const translations = await translateLocale(client, sourceLocale, locale, translatable.map((entry) => entry.msgid), glossary.keep, deeplGlossary);
			let rejected = 0;

			translatable.forEach((entry, index) => {
				const translated = translations[index];

				if(!translated || !samePlaceholders(entry.msgid, translated)) {
					rejected += 1;
					console.warn(`\t\t- [WARN] placeholders changed, left empty: ${entry.msgid} -> ${translated}`);
					return;
				}

				entry.msgstr = [translated];
				markForReview(entry);
			});

			await writeFile(file, po.compile(catalog, { foldLength: 0 }));
			console.log(`\t[INFO] [i18n][translate] ${locale}: ${translatable.length - rejected} translated, ${rejected} rejected -> ${path.relative(ROOT, file)}`);
		}

		if(client) {
			const usage = await client.getUsage();
			if(usage.character) {
				console.log(`\n\t[INFO] [i18n][translate] DeepL usage: ${usage.character.count} / ${usage.character.limit} characters this month`);
			}
		}
	} finally {
		if(client && deeplGlossary) {
			await client.deleteMultilingualGlossary(deeplGlossary).catch(() => undefined);
		}
	}
}

main().catch((error: unknown) => {
	console.error(`\n\t[ERROR] [i18n][translate] ${error instanceof Error ? error.message : String(error)}`);
	process.exitCode = 1;
});