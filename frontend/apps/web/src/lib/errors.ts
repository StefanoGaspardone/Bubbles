import { toApiError } from '@frontend/shared';
import { t } from '@lingui/core/macro';

export const errorMessage = (error: unknown): string => {
	const apiError = toApiError(error);

	if(apiError.isNetworkError) return t`Server non raggiungibile. Controlla la connessione e riprova.`;

	if(apiError.status === 429) {
		const seconds = apiError.retryAfterSeconds;

		return seconds
			? t`Troppi tentativi: riprova tra ${seconds} secondi.`
			: t`Troppi tentativi: riprova tra poco.`;
	}

	if(apiError.status >= 500) return t`Errore del server. Riprova tra poco.`;

	return apiError.message;
}