import { Route, Routes } from 'react-router';

import { Toaster } from '@/components/ui/toast';
import GuestRoute from '@/components/auth/guest-protected';
import ProtectedRoute from '@/components/auth/protected-ruoute';
import AppLayout from '@/components/layout/app-layout';

import LandingPage from '@/pages/landing-page';
import LoginPage from '@/pages/auth/login-page';
import SignupPage from '@/pages/auth/signup-page';
import NotFoundPage from '@/pages/not-found-page';
import HomePage from '@/pages/home-page';

const App = () => {
	return (
		<>
			<Toaster/>
			<Routes>
				<Route element = { <GuestRoute/> }>
					<Route path = '/login' element = { <LoginPage/> }/>
					<Route path = '/signup' element = { <SignupPage/> }/>
				</Route>
				<Route element = { <ProtectedRoute landing = { <LandingPage/> }/> }>
					<Route element = { <AppLayout/> }>
						<Route index element = { <HomePage/> }/>
					</Route>
				</Route>
				<Route path = '*' element = { <NotFoundPage/> }/>
			</Routes>
		</>
	)
}
 
export default App;