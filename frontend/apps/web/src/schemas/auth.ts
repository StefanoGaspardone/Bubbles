import { t } from '@lingui/core/macro';
import { z } from 'zod';

const USERNAME_PATTERN = /^[a-zA-Z0-9._]+$/;
const OTP_PATTERN = /^\d{6}$/;

const identifier = z.string().trim().min(1, { error: () => t`Inserisci email o username` });

const otpCode = z.string().regex(OTP_PATTERN, { error: () => t`Inserisci il codice di 6 cifre` });

const password = z
	.string()
	.min(8, { error: () => t`Almeno 8 caratteri` })
	.max(72, { error: () => t`Al massimo 72 caratteri` });

const confirmPassword = z.string().min(1, { error: () => t`Ripeti la password` });

export const loginSchema = z.object({
	identifier,
	password: z.string().min(1, { error: () => t`Inserisci la password` }),
});

export const signupSchema = z
	.object({
		firstName: z.string().trim().min(1, { error: () => t`Inserisci il nome` }).max(50, { error: () => t`Al massimo 50 caratteri` }),
		lastName: z.string().trim().min(1, { error: () => t`Inserisci il cognome` }).max(50, { error: () => t`Al massimo 50 caratteri` }),
		username: z
			.string()
			.trim()
			.min(3, { error: () => t`Almeno 3 caratteri` })
			.max(50, { error: () => t`Al massimo 50 caratteri` })
			.regex(USERNAME_PATTERN, { error: () => t`Solo lettere, numeri, punto e underscore` }),
		email: z.string().trim().pipe(z.email({ error: () => t`Email non valida` }).max(255, { error: () => t`Al massimo 255 caratteri` })),
		password,
		confirmPassword,
	})
	.refine((values) => values.password === values.confirmPassword, {
		error: () => t`Le password non coincidono`,
		path: ['confirmPassword'],
	});

export const activateSchema = z.object({
	identifier,
	otpCode,
});

export const forgotPasswordSchema = z.object({
	identifier,
});

export const resetPasswordSchema = z
	.object({
		identifier,
		otpCode,
		newPassword: password,
		confirmPassword,
	})
	.refine((values) => values.newPassword === values.confirmPassword, {
		error: () => t`Le password non coincidono`,
		path: ['confirmPassword'],
	});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;
export type ActivateFormValues = z.infer<typeof activateSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;