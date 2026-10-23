import { zodResolver } from '@hookform/resolvers/zod';
import { Trans, useLingui } from '@lingui/react/macro';
import { AlertCircleIcon } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router';

import { forgotPassword } from '@frontend/shared';

import AuthPageLayout from '@/components/layout/auth-page-layout';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

import { forgotPasswordSchema, type ForgotPasswordFormValues } from '@/schemas/auth';

import { errorMessage } from '@/lib/errors';
import { LOGIN_PATH } from '@/lib/redirect';

export const RESET_PASSWORD_PATH = '/reset-password';

const ForgotPasswordPage = () => {
	const { t } = useLingui();
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();

	const { register, handleSubmit, setError, formState: { errors, isSubmitting, isValid } } = useForm<ForgotPasswordFormValues>({
		resolver: zodResolver(forgotPasswordSchema),
		mode: 'onTouched',
		defaultValues: { identifier: searchParams.get('identifier') ?? '' },
	});

	const onSubmit = async ({ identifier }: ForgotPasswordFormValues): Promise<void> => {
		try {
			await forgotPassword({ identifier });
			await navigate(`${RESET_PASSWORD_PATH}?identifier=${encodeURIComponent(identifier)}`, { replace: true });
		} catch(error) {
			setError('root', { message: errorMessage(error) });
		}
	}

	return (
		<AuthPageLayout title = { t`Password dimenticata?` } description = { t`Nessun problema: ti mandiamo via email un codice per sceglierne una nuova.` }
			footer = {
				<Trans>
					Ti è tornata in mente?{' '}
					<Link to = { LOGIN_PATH } className = 'font-medium text-foreground underline underline-offset-4'>
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
					<Field data-invalid = { !!errors.identifier }>
						<FieldLabel htmlFor = 'identifier'><Trans>Email o username</Trans></FieldLabel>
						<Input id = 'identifier' autoComplete = 'username' autoFocus aria-invalid = { !!errors.identifier } {...register('identifier')}/>
						{errors.identifier
							? <FieldError errors = { [errors.identifier] }/>
							: <FieldDescription><Trans>Quelli che usi per accedere.</Trans></FieldDescription>}
					</Field>
					<Button type = 'submit' className = 'w-full cursor-pointer' disabled = { !isValid || isSubmitting }>
						{isSubmitting ? <Trans>Invio in corso…</Trans> : <Trans>Inviami il codice</Trans>}
					</Button>
				</FieldGroup>
			</form>
		</AuthPageLayout>
	)
}

export default ForgotPasswordPage;