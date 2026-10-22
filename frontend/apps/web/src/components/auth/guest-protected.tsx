import { Navigate, Outlet, useSearchParams } from 'react-router';

import PageLoader from '@/components/common/page-loader';

import { useAuth } from '@/contexts/auth-context';

import { REDIRECT_PARAM, safeRedirectPath } from '@/lib/redirect';

const GuestRoute = () => {
	const { isLoggedIn, isLoading } = useAuth();
	const [searchParams] = useSearchParams();

	if(isLoading) return <PageLoader/>;

	if(isLoggedIn) return <Navigate to = { safeRedirectPath(searchParams.get(REDIRECT_PARAM)) } replace/>;

	return (
        <Outlet/>
    )
}

export default GuestRoute;