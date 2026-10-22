import type { CSSProperties } from 'react';

import { cn } from '@/lib/utils';

const BUBBLES = [
	{ left: 8, size: 14, duration: 11, delay: 0, rest: 12 },
	{ left: 15, size: 8, duration: 9, delay: 3, rest: 64 },
	{ left: 22, size: 22, duration: 14, delay: 6, rest: 38 },
	{ left: 31, size: 10, duration: 10, delay: 1.5, rest: 82 },
	{ left: 38, size: 6, duration: 8, delay: 7, rest: 20 },
	{ left: 46, size: 18, duration: 13, delay: 4, rest: 55 },
	{ left: 55, size: 9, duration: 9.5, delay: 0.5, rest: 8 },
	{ left: 62, size: 26, duration: 16, delay: 8, rest: 72 },
	{ left: 70, size: 12, duration: 11, delay: 2.5, rest: 30 },
	{ left: 78, size: 7, duration: 8.5, delay: 5.5, rest: 90 },
	{ left: 85, size: 16, duration: 12, delay: 9, rest: 46 },
	{ left: 92, size: 10, duration: 10.5, delay: 3.5, rest: 16 },
]

interface BubblesProps {
	bubbleClassName?: string;
	className?: string;
}

const Bubbles = ({ bubbleClassName = 'border-white/40 bg-white/10', className }: BubblesProps) => {
	return (
        <div className = { cn('pointer-events-none absolute inset-0', className) } aria-hidden = 'true'>
            {BUBBLES.map(({ left, size, duration, delay, rest }) => (
                <span key = { left } className = { cn('absolute bottom-(--bubble-rest) rounded-full border motion-safe:-bottom-8 motion-safe:animate-bubble-rise', bubbleClassName) }
                    style = { {
                        '--bubble-rest': `${rest}%`,
                        left: `${left}%`,
                        width: size,
                        height: size,
                        animationDuration: `${duration}s`,
                        animationDelay: `-${delay}s`,
                    } as CSSProperties }
                />
            ))}
        </div>
    )
}

export default Bubbles;