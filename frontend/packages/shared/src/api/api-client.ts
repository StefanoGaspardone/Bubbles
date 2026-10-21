import axios, { type AxiosInstance } from 'axios';

import type { ClientType } from '../types';

declare module 'axios' {
	interface InternalAxiosRequestConfig {
		_retry?: boolean;
	}
}

// =============================================================================
// Errors
// =============================================================================

interface ErrorResponseBody {
	timestamp: string;
	status: number;
	error: string;
	message: string;
}

export class ApiError extends Error {
	readonly status: number;
	readonly error: string | null;
	readonly retryAfterSeconds: number | null;

	constructor(status: number, message: string, error: string | null = null, retryAfterSeconds: number | null = null) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
		this.error = error;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	get isNetworkError(): boolean {
		return this.status === 0;
	}

	get isAuthError(): boolean {
		return this.status === 401 || this.status === 403;
	}
}

const isErrorResponseBody = (data: unknown): data is ErrorResponseBody =>
	typeof data === 'object' && data !== null && 'status' in data && 'message' in data;

export const toApiError = (error: unknown): ApiError => {
	if(error instanceof ApiError) return error;

	if(axios.isAxiosError(error)) {
		const response = error.response;

		if(!response) {
			return new ApiError(0, error.code === 'ECONNABORTED' ? 'Request timed out' : 'Network error');
		}

		const retryAfter = Number(response.headers?.['retry-after']);
		const retryAfterSeconds = Number.isFinite(retryAfter) ? retryAfter : null;

		if(isErrorResponseBody(response.data)) {
			return new ApiError(response.status, response.data.message, response.data.error, retryAfterSeconds);
		}

		return new ApiError(response.status, error.message, response.statusText || null, retryAfterSeconds);
	}

	return new ApiError(0, error instanceof Error ? error.message : 'Unknown error');
}

// =============================================================================
// Token store
// =============================================================================

export interface TokenStore {
	getAccessToken(): string | null | Promise<string | null>;
	setAccessToken(token: string | null): void | Promise<void>;
    getRefreshToken?(): string | null | Promise<string | null>;
	setRefreshToken?(token: string | null): void | Promise<void>;
}

export const createMemoryTokenStore = (): TokenStore => {
	let accessToken: string | null = null;

	return {
		getAccessToken: () => accessToken,
		setAccessToken: (token) => {
			accessToken = token;
		},
	}
}

// =============================================================================
// Client
// =============================================================================

export const CLIENT_TYPE_HEADER = 'X-Client-Type';

export interface SessionTokens {
	accessToken: string;
	refreshToken?: string | null;
}

export interface ApiClientConfig {
	baseUrl: string;
	clientType: ClientType;
	tokenStore: TokenStore;
	onSessionExpired?: () => void;
	timeoutMs?: number;
}

export interface ApiClient {
	http: AxiosInstance;
    storeSession(tokens: SessionTokens): Promise<void>;
    refreshSession(): Promise<string>;
    restoreSession(): Promise<boolean>;
    logout(): Promise<void>;
    clearSession(): Promise<void>;
}

const isAuthEndpoint = (url: string | undefined): boolean =>
	url !== undefined && /^\/?auth\//.test(url);

export const createApiClient = (config: ApiClientConfig): ApiClient => {
	const { baseUrl, clientType, tokenStore, onSessionExpired, timeoutMs = 15_000 } = config;
	const isWeb = clientType === 'WEB';

	const defaults = {
		baseURL: baseUrl,
		timeout: timeoutMs,
		withCredentials: isWeb,
		headers: isWeb ? {} : { [CLIENT_TYPE_HEADER]: 'MOBILE' },
	}

	const http = axios.create(defaults);
    const authHttp = axios.create(defaults);

	// ---------------------------------------------------------------------------
	// Session
	// ---------------------------------------------------------------------------

	const storeSession = async (tokens: SessionTokens): Promise<void> => {
		await tokenStore.setAccessToken(tokens.accessToken);

		if(!isWeb && tokens.refreshToken) {
			await tokenStore.setRefreshToken?.(tokens.refreshToken);
		}
	}

	const clearSession = async (): Promise<void> => {
		await tokenStore.setAccessToken(null);
		await tokenStore.setRefreshToken?.(null);
	}

	const refreshTokenBody = async (): Promise<{ refreshToken: string } | undefined | null> => {
		if(isWeb) return undefined;

		const refreshToken = await tokenStore.getRefreshToken?.();
		return refreshToken ? { refreshToken } : null;
	}

	const performRefresh = async (): Promise<string> => {
		const body = await refreshTokenBody();

		if(body === null) {
			await clearSession();
			throw new ApiError(401, 'No active session');
		}

		try {
			const { data } = await authHttp.post<SessionTokens>('/auth/refresh', body);
			await storeSession(data);

			return data.accessToken;
		} catch(error) {
			const apiError = toApiError(error);

			if(apiError.isAuthError) {
				await clearSession();
			}

			throw apiError;
		}
	}

	let refreshPromise: Promise<string> | null = null;

	const refreshSession = (): Promise<string> => {
		refreshPromise ??= performRefresh().finally(() => {
			refreshPromise = null;
		});

		return refreshPromise;
	}

	const restoreSession = async (): Promise<boolean> => {
		try {
			await refreshSession();
			return true;
		} catch(error) {
			const apiError = toApiError(error);
			if(apiError.isAuthError) return false;

			throw apiError;
		}
	}

	const logout = async (): Promise<void> => {
		try {
			const body = await refreshTokenBody();
			if(body !== null) {
				await authHttp.post('/auth/logout', body);
			}
		} catch {
			// swallow catch
		} finally {
			await clearSession();
		}
	}

	// ---------------------------------------------------------------------------
	// Interceptor
	// ---------------------------------------------------------------------------

	http.interceptors.request.use(async (requestConfig) => {
		if(!requestConfig.headers.has('Authorization')) {
			const accessToken = await tokenStore.getAccessToken();
			if(accessToken) {
				requestConfig.headers.set('Authorization', `Bearer ${accessToken}`);
			}
		}

		return requestConfig;
	});

	http.interceptors.response.use(
		(response) => response,
		async (error: unknown) => {
			if(!axios.isAxiosError(error) || !error.config) {
				throw toApiError(error);
			}

			const originalRequest = error.config;
			const shouldRefresh =
				error.response?.status === 401 &&
				!originalRequest._retry &&
				!isAuthEndpoint(originalRequest.url);

			if(!shouldRefresh) {
				throw toApiError(error);
			}

			originalRequest._retry = true;

			let accessToken: string;
			try {
				accessToken = await refreshSession();
			} catch(refreshError) {
				const apiError = toApiError(refreshError);

				if(apiError.isAuthError) {
					onSessionExpired?.();
				}

				throw apiError;
			}

			originalRequest.headers.set('Authorization', `Bearer ${accessToken}`);
			return http(originalRequest);
		},
	);

	return {
		http,
		storeSession,
		refreshSession,
		restoreSession,
		logout,
		clearSession,
	}
}

let currentClient: ApiClient | null = null;

export const initApiClient = (config: ApiClientConfig): ApiClient => {
	currentClient = createApiClient(config);
	return currentClient;
}

export const getApiClient = (): ApiClient => {
	if(!currentClient) {
		throw new Error('API client not initialized: call initApiClient() at app startup');
	}

	return currentClient;
}