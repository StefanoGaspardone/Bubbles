import { t } from '@lingui/core/macro';
import { z } from 'zod';

const USERNAME_PATTERN = /^[a-zA-Z0-9._]+$/;

const password = z
	.string()
	.min(8, { error: () => t`Almeno 8 caratteri` })
	.max(72, { error: () => t`Al massimo 72 caratteri` });

export const loginSchema = z.object({
	identifier: z.string().trim().min(1, { error: () => t`Inserisci email o username` }),
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
		confirmPassword: z.string().min(1, { error: () => t`Ripeti la password` }),
	})
	.refine((values) => values.password === values.confirmPassword, {
		error: () => t`Le password non coincidono`,
		path: ['confirmPassword'],
	});

export type LoginFormValues = z.infer<typeof loginSchema>;
export type SignupFormValues = z.infer<typeof signupSchema>;