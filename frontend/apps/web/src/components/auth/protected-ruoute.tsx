import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';

import PageLoader from '@/components/common/page-loader';

import { useAuth } from '@/contexts/auth-context';

import { HOME_PATH, LOGIN_PATH, REDIRECT_PARAM } from '@/lib/redirect';

interface ProtectedRouteProps {
	landing?: ReactNode;
}

const ProtectedRoute = ({ landing }: ProtectedRouteProps) => {
	const { isLoggedIn, isLoading } = useAuth();
	const location = useLocation();

	if(isLoading) return <PageLoader/>;

	if(!isLoggedIn) {
		if(location.pathname === HOME_PATH && landing) return landing;

		const requested = location.pathname + location.search;
		const search = requested === HOME_PATH ? '' : `?${new URLSearchParams({ [REDIRECT_PARAM]: requested })}`;

		return <Navigate to = { LOGIN_PATH + search } replace/>;
	}

	return (
        <Outlet/>
    )
}

export default ProtectedRoute;