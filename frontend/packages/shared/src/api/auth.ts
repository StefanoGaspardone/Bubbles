import type { AuthResponseDTO, LoginRequestDTO } from '../types';
import { getApiClient } from './api-client';
import * as generated from './generated/auth';

export {
	activate,
	forgotPassword,
	resendActivationOtp,
	resetPassword,
	signup,
} from './generated/auth';

export const login = async (request: LoginRequestDTO): Promise<AuthResponseDTO> => {
	const response = await generated.login(request);
	await getApiClient().storeSession(response);

	return response;
}

export const logout = (): Promise<void> =>
	getApiClient().logout();

export const restoreSession = (): Promise<boolean> =>
	getApiClient().restoreSession();

export const refreshSession = (): Promise<string> =>
	getApiClient().refreshSession();