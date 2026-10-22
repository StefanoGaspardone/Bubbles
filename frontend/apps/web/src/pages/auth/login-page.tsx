import { zodResolver } from '@hookform/resolvers/zod';
import { Trans, useLingui } from '@lingui/react/macro';
import { AlertCircleIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { toApiError } from '@frontend/shared';

import AuthPageLayout from '@/components/layout/auth-page-layout';
import PasswordInput from '@/components/auth/password-input';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

import { useAuth } from '@/contexts/auth-context';

import { loginSchema, type LoginFormValues } from '@/schemas/auth';

import { errorMessage } from '@/lib/errors';

const ACCOUNT_NOT_ACTIVATED = 'Account is not activated';

const LoginPage = () => {
	const { t } = useLingui();
	const { login } = useAuth();

	const { register, handleSubmit, getValues, setError, formState: { errors, isSubmitting } } = useForm<LoginFormValues>({
		resolver: zodResolver(loginSchema),
		defaultValues: { identifier: '', password: '' },
	});

	const onSubmit = async (values: LoginFormValues): Promise<void> => {
		try {
			await login(values);
		} catch(error) {
			const apiError = toApiError(error);

			if(apiError.status === 401) {
				setError('root', { message: t`Email, username o password non corretti.` });
			} else if(apiError.status === 403 && apiError.message === ACCOUNT_NOT_ACTIVATED) {
				setError('root', { type: 'not-activated', message: t`L'account non è ancora attivo.` });
			} else {
				setError('root', { message: errorMessage(apiError) });
			}
		}
	}

	const rootError = errors.root;
	let rootErrorContent: ReactNode = rootError?.message;

	if(rootError?.type === 'not-activated') {
		const identifier = encodeURIComponent(getValues('identifier'));

		rootErrorContent = (
			<>
				{rootError.message}{' '}
				<Link to = { `/activate?identifier=${identifier}` } className = 'font-medium underline underline-offset-4'>
					<Trans>Attivalo con il codice ricevuto via email</Trans>
				</Link>
			</>
		)
	}

	return (
		<AuthPageLayout title = { t`Bentornato` } description = { t`Accedi per ritrovare il tuo logbook.` }
			footer = {
				<Trans>
					Non hai un account?{' '}
					<Link to = '/signup' className = 'font-medium text-foreground underline underline-offset-4'>
						Registrati
					</Link>
				</Trans>
			}
		>
			<form onSubmit = { handleSubmit(onSubmit) } noValidate>
				<FieldGroup>
					{rootError && (
						<Alert variant = 'destructive'>
							<AlertCircleIcon/>
							<AlertDescription>{rootErrorContent}</AlertDescription>
						</Alert>
					)}
					<Field data-invalid = { !!errors.identifier }>
						<FieldLabel htmlFor = 'identifier'><Trans>Email o username</Trans></FieldLabel>
						<Input id = 'identifier' autoComplete = 'username' autoFocus aria-invalid = { !!errors.identifier } {...register('identifier')}/>
						<FieldError errors = { [errors.identifier] }/>
					</Field>
					<Field data-invalid = { !!errors.password }>
						<div className = 'flex items-center justify-between'>
							<FieldLabel htmlFor = 'password'><Trans>Password</Trans></FieldLabel>
							<Link to = '/forgot-password' className = 'text-sm text-muted-foreground underline-offset-4 hover:underline'>
								<Trans>Password dimenticata?</Trans>
							</Link>
						</div>
						<PasswordInput id = 'password' autoComplete = 'current-password' aria-invalid = { !!errors.password } {...register('password')}/>
						<FieldError errors = { [errors.password] }/>
					</Field>
					<Button type = 'submit' className = 'w-full' disabled = { isSubmitting }>
						{isSubmitting ? <Trans>Accesso in corso…</Trans> : <Trans>Accedi</Trans>}
					</Button>
				</FieldGroup>
			</form>
		</AuthPageLayout>
	)
}

export default LoginPage;