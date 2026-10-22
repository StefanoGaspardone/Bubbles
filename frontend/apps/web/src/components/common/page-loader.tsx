import { Loader2Icon } from 'lucide-react';

const PageLoader = () => {
    return (
        <div className = 'flex min-h-svh items-center justify-center'>
            <Loader2Icon className = 'size-8 animate-spin text-muted-foreground' aria-label = 'Loading'/>
        </div>
    )
}

export default PageLoader;