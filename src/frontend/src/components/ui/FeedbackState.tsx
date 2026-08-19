import { AlertCircle, Inbox, RefreshCw } from 'lucide-react'

type Props = {
  kind: 'error' | 'empty'
  title: string
  message: string
  onRetry?: () => void
}

export function FeedbackState({ kind, title, message, onRetry }: Props) {
  const Icon = kind === 'error' ? AlertCircle : Inbox
  return (
    <div className={`feedback-state ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <Icon />
      <h2>{title}</h2>
      <p>{message}</p>
      {onRetry && <button className="button secondary" onClick={onRetry}><RefreshCw /> Retry</button>}
    </div>
  )
}
