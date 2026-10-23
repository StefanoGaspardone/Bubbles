import { zodResolver } from '@hookform/resolvers/zod';
import { Trans, useLingui } from '@lingui/react/macro';
import { AlertCircleIcon } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';

import { signup, toApiError } from '@frontend/shared';

import AuthPageLayout from '@/components/layout/auth-page-layout';
import PasswordInput from '@/components/auth/password-input';
import PasswordMatchHint from '@/components/auth/password-match-hint';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

import { signupSchema, type SignupFormValues } from '@/schemas/auth';

import { errorMessage } from '@/lib/errors';

const SignupPage = () => {
	const { t } = useLingui();
	const navigate = useNavigate();

	const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, isValid } } = useForm<SignupFormValues>({
		resolver: zodResolver(signupSchema),
		mode: 'onTouched',
		defaultValues: { firstName: '', lastName: '', username: '', email: '', password: '', confirmPassword: '' },
	});

	const password = watch('password');
	const confirmPassword = watch('confirmPassword');
	const showMatch = confirmPassword.length > 0;
	const passwordsMatch = password === confirmPassword;

	const onSubmit = async ({ firstName, lastName, username, email, password }: SignupFormValues): Promise<void> => {
		try {
			const response = await signup({ firstName, lastName, username, email, password });

			await navigate(`/activate?identifier=${encodeURIComponent(response.email)}`, { replace: true });
		} catch(error) {
			const apiError = toApiError(error);

			if(apiError.status === 409 && apiError.message.startsWith('Email')) {
				setError('email', { message: t`Email già registrata` }, { shouldFocus: true });
			} else if(apiError.status === 409 && apiError.message.startsWith('Username')) {
				setError('username', { message: t`Username già in uso` }, { shouldFocus: true });
			} else {
				setError('root', { message: errorMessage(apiError) });
			}
		}
	}

	return (
		<AuthPageLayout title = { t`Crea il tuo account` } description = { t`Pochi dati e sei pronto a registrare la tua prima immersione.` }
			footer = {
				<Trans>
					Hai già un account?{' '}
					<Link to = '/login' className = 'font-medium text-foreground underline underline-offset-4'>
						Accedi
					</Link>
				</Trans>
			}
		>
			<form onSubmit = { handleSubmit(onSubmit) } noValidate>
				<FieldGroup>
					{errors.root && (
						<Alert variant = 'destructive'>
							<AlertCircleIcon/>
							<AlertDescription>{errors.root.message}</AlertDescription>
						</Alert>
					)}
					<div className = 'grid gap-4 sm:grid-cols-2'>
						<Field data-invalid = { !!errors.firstName }>
							<FieldLabel htmlFor = 'firstName'><Trans>Nome</Trans></FieldLabel>
							<Input id = 'firstName' autoComplete = 'given-name' autoFocus aria-invalid = { !!errors.firstName } {...register('firstName')}/>
							<FieldError errors = { [errors.firstName] }/>
						</Field>
						<Field data-invalid = { !!errors.lastName }>
							<FieldLabel htmlFor = 'lastName'><Trans>Cognome</Trans></FieldLabel>
							<Input id = 'lastName' autoComplete = 'family-name' aria-invalid = { !!errors.lastName } {...register('lastName')}/>
							<FieldError errors = { [errors.lastName] }/>
						</Field>
					</div>
					<Field data-invalid = { !!errors.username }>
						<FieldLabel htmlFor = 'username'><Trans>Username</Trans></FieldLabel>
						<Input id = 'username' autoComplete = 'username' aria-invalid = { !!errors.username } {...register('username')}/>
						{errors.username
							? <FieldError errors = { [errors.username] }/>
							: <FieldDescription><Trans>Lettere, numeri, punto e underscore. I compagni ti troveranno con questo.</Trans></FieldDescription>}
					</Field>
					<Field data-invalid = { !!errors.email }>
						<FieldLabel htmlFor = 'email'><Trans>Email</Trans></FieldLabel>
						<Input id = 'email' type = 'email' autoComplete = 'email' aria-invalid = { !!errors.email } {...register('email')}/>
						<FieldError errors = { [errors.email] }/>
					</Field>
					<Field data-invalid = { !!errors.password }>
						<FieldLabel htmlFor = 'password'><Trans>Password</Trans></FieldLabel>
						<PasswordInput id = 'password' autoComplete = 'new-password' aria-invalid = { !!errors.password } {...register('password')}/>
						{errors.password
							? <FieldError errors = { [errors.password] }/>
							: <FieldDescription><Trans>Almeno 8 caratteri.</Trans></FieldDescription>}
					</Field>
					<Field data-invalid = { showMatch ? !passwordsMatch : !!errors.confirmPassword }>
						<FieldLabel htmlFor = 'confirmPassword'><Trans>Conferma password</Trans></FieldLabel>
						<PasswordInput id = 'confirmPassword' autoComplete = 'new-password' aria-invalid = { showMatch ? !passwordsMatch : !!errors.confirmPassword } aria-describedby = 'confirmPassword-match' {...register('confirmPassword')}/>
						{showMatch
							? <PasswordMatchHint id = 'confirmPassword-match' matches = { passwordsMatch }/>
							: <FieldError errors = { [errors.confirmPassword] }/>}
					</Field>
					<Button type = 'submit' className = 'w-full' disabled = { !isValid || isSubmitting }>
						{isSubmitting ? <Trans>Creazione account…</Trans> : <Trans>Crea account</Trans>}
					</Button>
				</FieldGroup>
			</form>
		</AuthPageLayout>
	)
}

export default SignupPage;