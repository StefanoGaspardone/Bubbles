import { createMemoryTokenStore, initApiClient } from '@frontend/shared';

let sessionExpiredHandler: (() => void) | null = null;

export const setSessionExpiredHandler = (handler: (() => void) | null): void => {
	sessionExpiredHandler = handler;
}

initApiClient({
	baseUrl: import.meta.env.VITE_API_URL ?? '/api/v1',
	clientType: 'WEB',
	tokenStore: createMemoryTokenStore(),
	onSessionExpired: () => sessionExpiredHandler?.(),
	refreshLock: (task) => {
		if(typeof navigator === 'undefined' || !navigator.locks) return task();

		return new Promise((resolve, reject) => {
			void navigator.locks.request('bubbles-session-refresh', () => task().then(resolve, reject));
		});
	},
});