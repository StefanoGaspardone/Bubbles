import { useCallback, useEffect, useState } from 'react';

export const useCountdown = (initialSeconds = 0) => {
	const [endsAt, setEndsAt] = useState<number | null>(() => initialSeconds > 0 ? Date.now() + initialSeconds * 1000 : null);
	const [seconds, setSeconds] = useState(initialSeconds);

	useEffect(() => {
		if(endsAt === null) return;

		const tick = () => {
			const remaining = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000));
			setSeconds(remaining);

			if(remaining === 0) setEndsAt(null);
		}

		tick();
		const interval = window.setInterval(tick, 250);

		return () => window.clearInterval(interval);
	}, [endsAt]);

	const start = useCallback((value: number) => {
		setSeconds(value);
		setEndsAt(value > 0 ? Date.now() + value * 1000 : null);
	}, []);

	return { seconds, isRunning: seconds > 0, start };
}