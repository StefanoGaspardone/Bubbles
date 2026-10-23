import { zodResolver } from '@hookform/resolvers/zod';
import { Trans, useLingui } from '@lingui/react/macro';
import { AlertCircleIcon, MailCheckIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link, useNavigate, useSearchParams } from 'react-router';

import { activate, resendActivationOtp, toApiError } from '@frontend/shared';

import OtpInput from '@/components/auth/otp-input';
import AuthPageLayout from '@/components/layout/auth-page-layout';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';

import { useCountdown } from '@/hooks/use-countdown';

import { activateSchema, type ActivateFormValues } from '@/schemas/auth';

import { errorMessage } from '@/lib/errors';
import { LOGIN_PATH, type LoginLocationState } from '@/lib/redirect';

const RESEND_COOLDOWN_SECONDS = 60;

const INVALID_OTP = 'Invalid OTP code';
const EXPIRED_OTP = 'OTP code expired';

const ActivatePage = () => {
	const { t } = useLingui();
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();
	const cooldown = useCountdown();

	const initialIdentifier = searchParams.get('identifier') ?? '';
	const [editingIdentifier, setEditingIdentifier] = useState(initialIdentifier === '');
	const [notice, setNotice] = useState<string | null>(null);
	const [isResending, setIsResending] = useState(false);

	const { control, register, handleSubmit, getValues, setValue, setError, clearErrors, trigger, formState: { errors, isSubmitting } } = useForm<ActivateFormValues>({
		resolver: zodResolver(activateSchema),
		defaultValues: { identifier: initialIdentifier, otpCode: '' },
	});

	const onSubmit = async (values: ActivateFormValues): Promise<void> => {
		setNotice(null);

		try {
			await activate(values);

			const state: LoginLocationState = { notice: 'activated', identifier: values.identifier };
			await navigate(LOGIN_PATH, { replace: true, state });
		} catch(error) {
			const apiError = toApiError(error);
			const resetCode = () => setValue('otpCode', '');

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
				setError('root', { message: t`Nessun account da attivare con questa email o username.` });
			} else if(apiError.status === 409) {
				setError('root', { type: 'already-active', message: t`Questo account è già attivo.` });
			} else {
				setError('root', { message: errorMessage(apiError) });
			}
		}
	}

	const handleResend = async (): Promise<void> => {
		if(!await trigger('identifier')) {
			setEditingIdentifier(true);
			return;
		}

		setNotice(null);
		clearErrors();
		setIsResending(true);

		try {
			await resendActivationOtp({ identifier: getValues('identifier') });

			setValue('otpCode', '');
			setNotice(t`Ti abbiamo inviato un nuovo codice.`);
			cooldown.start(RESEND_COOLDOWN_SECONDS);
		} catch(error) {
			const apiError = toApiError(error);

			if(apiError.status === 429) {
				cooldown.start(apiError.retryAfterSeconds ?? RESEND_COOLDOWN_SECONDS);
			} else if(apiError.status === 404) {
				setEditingIdentifier(true);
				setError('root', { message: t`Nessun account da attivare con questa email o username.` });
			} else {
				setError('root', { message: errorMessage(apiError) });
			}
		} finally {
			setIsResending(false);
		}
	}

	const rootError = errors.root;
	let rootErrorContent: ReactNode = rootError?.message;

	if(rootError?.type === 'already-active') {
		rootErrorContent = (
			<>
				{rootError.message}{' '}
				<Link to = { LOGIN_PATH } className = 'font-medium underline underline-offset-4'>
					<Trans>Accedi</Trans>
				</Link>
			</>
		)
	}

	const seconds = cooldown.seconds;
	let resendLabel: ReactNode = <Trans>Non hai ricevuto il codice? Invialo di nuovo</Trans>;
	if(isResending) resendLabel = <Trans>Invio in corso…</Trans>;
	else if(cooldown.isRunning) resendLabel = <Trans>Puoi richiedere un nuovo codice tra {seconds} s</Trans>;

	return (
		<AuthPageLayout title = { t`Attiva il tuo account` }
			description = {
				editingIdentifier
					? <Trans>Inserisci la tua email o username e il codice di 6 cifre che ti abbiamo inviato.</Trans>
					: <Trans>Ti abbiamo inviato un codice di 6 cifre a <strong className = 'font-medium text-foreground'>{initialIdentifier}</strong>. Inseriscilo qui sotto per attivare l'account.</Trans>
			}
			footer = {
				<Trans>
					Account già attivo?{' '}
					<Link to = { LOGIN_PATH } className = 'font-medium text-foreground underline underline-offset-4'>
						Accedi
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
							<OtpInput id = 'otpCode' autoFocus = { !editingIdentifier } disabled = { isSubmitting } aria-invalid = { !!errors.otpCode } value = { field.value } onChange = { field.onChange } onBlur = { field.onBlur } onComplete = { () => void handleSubmit(onSubmit)() }/>
						) }/>
						<FieldError errors = { [errors.otpCode] }/>
					</Field>
					<Button type = 'submit' className = 'w-full cursor-pointer' disabled = { isSubmitting }>
						{isSubmitting ? <Trans>Attivazione in corso…</Trans> : <Trans>Attiva account</Trans>}
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

export default ActivatePage;