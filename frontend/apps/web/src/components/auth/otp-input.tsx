import { REGEXP_ONLY_DIGITS } from 'input-otp';

import { InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot } from '@/components/ui/input-otp';

export const OTP_LENGTH = 6;

interface OtpInputProps {
	id?: string;
	value: string;
	onChange: (value: string) => void;
	onBlur?: () => void;
	onComplete?: (value: string) => void;
	disabled?: boolean;
	autoFocus?: boolean;
	'aria-invalid'?: boolean;
}

const OtpInput = (props: OtpInputProps) => {
	return (
		<InputOTP maxLength = { OTP_LENGTH } pattern = { REGEXP_ONLY_DIGITS } autoComplete = 'one-time-code' inputMode = 'numeric' {...props}>
			<InputOTPGroup>
				<InputOTPSlot index = { 0 }/>
				<InputOTPSlot index = { 1 }/>
				<InputOTPSlot index = { 2 }/>
			</InputOTPGroup>
			<InputOTPSeparator/>
			<InputOTPGroup>
				<InputOTPSlot index = { 3 }/>
				<InputOTPSlot index = { 4 }/>
				<InputOTPSlot index = { 5 }/>
			</InputOTPGroup>
		</InputOTP>
	)
}

export default OtpInput;