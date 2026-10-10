import type { ReactNode } from 'react'
import { AlertCircle, LoaderCircle, RotateCw } from 'lucide-react'
import { readableError } from '../../hooks/useRemoteData'

export function PageHeader({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: ReactNode }) {
  return <header className="app-page-header">{eyebrow && <p className="app-eyebrow">{eyebrow}</p>}<div className="app-page-header-row"><div><h1>{title}</h1>{description && <p>{description}</p>}</div>{actions && <div className="app-page-actions">{actions}</div>}</div></header>
}

export function LoadingState({ label = 'Loading' }: { label?: string }) {
  return <div className="app-feedback" role="status"><LoaderCircle size={19} className="app-spinner" /><span>{label}…</span></div>
}

export function ErrorState({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return <div className="app-feedback app-error" role="alert"><AlertCircle size={19} /><div><strong>Could not load this information</strong><p>{readableError(error)}</p></div>{onRetry && <button className="app-button secondary" onClick={onRetry}><RotateCw size={14} /> Try again</button>}</div>
}

export function EmptyState({ title, children, action }: { title: string; children: ReactNode; action?: ReactNode }) {
  return <div className="app-empty"><span className="app-empty-mark" aria-hidden="true" /><h2>{title}</h2><p>{children}</p>{action}</div>
}

export function StatusBadge({ value }: { value: string }) {
  return <span className={`app-status status-${value.toLowerCase().replaceAll('_', '-')}`}>{value.replaceAll('_', ' ')}</span>
}

export function AppButton({ children, variant = 'primary', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'quiet' }) {
  return <button className={`app-button ${variant}`} {...props}>{children}</button>
}
