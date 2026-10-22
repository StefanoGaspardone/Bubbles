export const LOGIN_PATH = '/login';
export const HOME_PATH = '/';
export const REDIRECT_PARAM = 'redirectTo';

export const safeRedirectPath = (value: string | null): string =>
	value && value.startsWith('/') && !value.startsWith('//') ? value : HOME_PATH;