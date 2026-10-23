import { zodResolver } from '@hookform/resolvers/zod';
import { Trans, useLingui } from '@lingui/react/macro';
import { AlertCircleIcon, MailCheckIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router';

import { forgotPassword, resetPassword, toApiError } from '@frontend/shared';

import OtpInput from '@/components/auth/otp-input';
import PasswordInput from '@/components/auth/password-input';
import PasswordMatchHint from '@/components/auth/password-match-hint';
import AuthPageLayout from '@/components/layout/auth-page-layout';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

import { useCountdown } from '@/hooks/use-countdown';

import { resetPasswordSchema, type ResetPasswordFormValues } from '@/schemas/auth';

import { errorMessage } from '@/lib/errors';
import { LOGIN_PATH, type LoginLocationState } from '@/lib/redirect';

const RESEND_COOLDOWN_SECONDS = 60;

const INVALID_OTP = 'Invalid OTP code';
const EXPIRED_OTP = 'OTP code expired';

const ResetPasswordPage = () => {
	const { t } = useLingui();
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();

	const initialIdentifier = searchParams.get('identifier') ?? '';
	const [editingIdentifier, setEditingIdentifier] = useState(initialIdentifier === '');
	const [notice, setNotice] = useState<string | null>(null);
	const [isResending, setIsResending] = useState(false);
	const cooldown = useCountdown(initialIdentifier ? RESEND_COOLDOWN_SECONDS : 0);

	const { control, register, handleSubmit, getValues, setValue, setError, clearErrors, trigger, watch, formState: { errors, isSubmitting, isValid } } = useForm<ResetPasswordFormValues>({
		resolver: zodResolver(resetPasswordSchema),
		mode: 'onTouched',
		defaultValues: { identifier: initialIdentifier, otpCode: '', newPassword: '', confirmPassword: '' },
	});

	const newPassword = watch('newPassword');
	const confirmPassword = watch('confirmPassword');
	const showMatch = confirmPassword.length > 0;
	const passwordsMatch = newPassword === confirmPassword;

	const onSubmit = async ({ identifier, otpCode, newPassword }: ResetPasswordFormValues): Promise<void> => {
		setNotice(null);

		try {
			await resetPassword({ identifier, otpCode, newPassword });

			const state: LoginLocationState = { notice: 'password-reset', identifier };
			await navigate(LOGIN_PATH, { replace: true, state });
		} catch(error) {
			const apiError = toApiError(error);
			const resetCode = () => setValue('otpCode', '', { shouldValidate: false });

			if(apiError.status === 400 && apiError.message === INVALID_OTP) {
				resetCode();
				setError('otpCode', { message: t`Codice non valido. Controlla l'email e riprova.` }, { shouldFocus: true });
			} else if(apiError.status === 400 && apiError.message === EXPIRED_OTP) {
				resetCode();
				setError('otpCode', { message: t`Il codice è scaduto: richiedine uno nuovo.` });
			} else if(apiError.status === 429) {
				resetCode();
				setError('otpCode', { message: t`Troppi tentativi sbagliati: richiedi un nuovo codice.` });
			} else if(apiError.status === 404) {
				setEditingIdentifier(true);
				setError('root', { message: t`Nessuna richiesta di reset attiva per questo account: richiedi un nuovo codice.` });
			} else {
				setError('root', { message: errorMessage(error) });
			}
		}
	}

	const handleResend = async (): Promise<void> => {
		if(!await trigger('identifier')) {
			setEditingIdentifier(true);
			return;
		}

		setNotice(null);
		clearErrors('root');
		clearErrors('otpCode');
		setIsResending(true);

		try {
			await forgotPassword({ identifier: getValues('identifier') });

			setValue('otpCode', '', { shouldValidate: false });
			setNotice(t`Se esiste un account con questi dati, ti abbiamo inviato un nuovo codice.`);
			
			cooldown.start(RESEND_COOLDOWN_SECONDS);
		} catch(error) {
			setError('root', { message: errorMessage(error) });
		} finally {
			setIsResending(false);
		}
	}

	const seconds = cooldown.seconds;
	let resendLabel: ReactNode = <Trans>Non hai ricevuto il codice? Invialo di nuovo</Trans>;
	if(isResending) resendLabel = <Trans>Invio in corso…</Trans>;
	else if(cooldown.isRunning) resendLabel = <Trans>Puoi richiedere un nuovo codice tra {seconds} s</Trans>;

	return (
		<AuthPageLayout title = { t`Reimposta la password` }
			description = {
				editingIdentifier
					? <Trans>Inserisci la tua email o username, il codice di 6 cifre ricevuto e la nuova password.</Trans>
					: <Trans>Se esiste un account per <strong className = 'font-medium text-foreground'>{initialIdentifier}</strong>, ti abbiamo inviato un codice di 6 cifre. Inseriscilo e scegli la nuova password.</Trans>
			}
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
					{notice && (
						<Alert role = 'status'>
							<MailCheckIcon/>
							<AlertDescription>{notice}</AlertDescription>
						</Alert>
					)}
					{editingIdentifier && (
						<Field data-invalid = { !!errors.identifier }>
							<FieldLabel htmlFor = 'identifier'><Trans>Email o username</Trans></FieldLabel>
							<Input id = 'identifier' autoComplete = 'username' autoFocus aria-invalid = { !!errors.identifier } {...register('identifier')}/>
							<FieldError errors = { [errors.identifier] }/>
						</Field>
					)}
					<Field data-invalid = { !!errors.otpCode }>
						<FieldLabel htmlFor = 'otpCode'><Trans>Codice di verifica</Trans></FieldLabel>
						<Controller name = 'otpCode' control = { control } render = { ({ field }) => (
							<OtpInput id = 'otpCode' autoFocus = { !editingIdentifier } disabled = { isSubmitting } aria-invalid = { !!errors.otpCode } value = { field.value } onChange = { field.onChange } onBlur = { field.onBlur }/>
						) }/>
						<FieldError errors = { [errors.otpCode] }/>
					</Field>
					<Field data-invalid = { !!errors.newPassword }>
						<FieldLabel htmlFor = 'newPassword'><Trans>Nuova password</Trans></FieldLabel>
						<PasswordInput id = 'newPassword' autoComplete = 'new-password' aria-invalid = { !!errors.newPassword } {...register('newPassword')}/>
						{errors.newPassword
							? <FieldError errors = { [errors.newPassword] }/>
							: <FieldDescription><Trans>Almeno 8 caratteri.</Trans></FieldDescription>}
					</Field>
					<Field data-invalid = { showMatch ? !passwordsMatch : !!errors.confirmPassword }>
						<FieldLabel htmlFor = 'confirmPassword'><Trans>Conferma password</Trans></FieldLabel>
						<PasswordInput id = 'confirmPassword' autoComplete = 'new-password' aria-invalid = { showMatch ? !passwordsMatch : !!errors.confirmPassword } aria-describedby = 'confirmPassword-match' {...register('confirmPassword')}/>
						{showMatch
							? <PasswordMatchHint id = 'confirmPassword-match' matches = { passwordsMatch }/>
							: <FieldError errors = { [errors.confirmPassword] }/>}
					</Field>
					<Button type = 'submit' className = 'w-full cursor-pointer' disabled = { !isValid || isSubmitting }>
						{isSubmitting ? <Trans>Salvataggio in corso…</Trans> : <Trans>Salva la nuova password</Trans>}
					</Button>
					<div className = 'flex flex-col items-center gap-1 text-sm'>
						<Button type = 'button' variant = 'link' className = 'h-auto cursor-pointer p-0 text-muted-foreground' disabled = { isResending || cooldown.isRunning } onClick = { () => void handleResend() }>
							{resendLabel}
						</Button>
						{!editingIdentifier && (
							<Button type = 'button' variant = 'link' className = 'h-auto cursor-pointer p-0 text-muted-foreground' onClick = { () => setEditingIdentifier(true) }>
								<Trans>Email sbagliata? Modificala</Trans>
							</Button>
						)}
					</div>
				</FieldGroup>
			</form>
		</AuthPageLayout>
	)
}

export default ResetPasswordPage;