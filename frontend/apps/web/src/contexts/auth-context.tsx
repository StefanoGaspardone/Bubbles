import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router';

import { getMyProfile, login as apiLogin, type LoginRequestDTO, logout as apiLogout, restoreSession, type UserDTO } from '@frontend/shared';

import { setSessionExpiredHandler } from '@/lib/api';
import { LOGIN_PATH } from '@/lib/redirect';

interface AuthContextType {
	user: UserDTO | null;
	isLoggedIn: boolean;
	isLoading: boolean;
	setUser: (user: UserDTO | null) => void;
	login: (credentials: LoginRequestDTO) => Promise<UserDTO>;
	logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

let initialUserPromise: Promise<UserDTO | null> | null = null;

const loadInitialUser = (): Promise<UserDTO | null> => {
	initialUserPromise ??= (async () => {
		try {
			const authenticated = await restoreSession();
			return authenticated ? await getMyProfile() : null;
		} catch {
			return null;
		}
	})();

	return initialUserPromise;
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
	const [user, setUser] = useState<UserDTO | null>(null);
	const [isLoading, setIsLoading] = useState<boolean>(true);

	const navigate = useNavigate();

	useEffect(() => {
		let active = true;

		void loadInitialUser().then((initialUser) => {
			if(!active) return;

			setUser(initialUser);
			setIsLoading(false);
		});

		return () => {
			active = false;
		}
	}, []);

	useEffect(() => {
		setSessionExpiredHandler(() => {
			setUser(null);
			void navigate(LOGIN_PATH, { replace: true });
		});

		return () => setSessionExpiredHandler(null);
	}, [navigate]);

	const login = useCallback(async (credentials: LoginRequestDTO): Promise<UserDTO> => {
		await apiLogin(credentials);

		try {
			const me = await getMyProfile();
			setUser(me);

			return me;
		} catch(error) {
			await apiLogout();
			throw error;
		}
	}, []);

	const logout = useCallback(async (): Promise<void> => {
		await apiLogout();
		
        setUser(null);
		
        await navigate(LOGIN_PATH, { replace: true });
	}, [navigate]);

	const value = useMemo<AuthContextType>(
        () => ({ user, isLoggedIn: user !== null, isLoading, setUser, login, logout }),
		[user, isLoading, login, logout],
	);

	return (
		<AuthContext.Provider value = { value }>
			{children}
		</AuthContext.Provider>
	);
}

export const useAuth = (): AuthContextType => {
	const context = useContext(AuthContext);

	if(context === undefined) throw new Error('useAuth must be used inside an AuthProvider');

	return context;
}