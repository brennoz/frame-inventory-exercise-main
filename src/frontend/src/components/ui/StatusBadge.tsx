import type { FrameStatus } from '../../types/frame'

const labels: Record<FrameStatus, string> = {
  LIVE: 'Live',
  PENDING: 'Pending',
  MAINTENANCE: 'Maintenance',
  BLOCKED: 'Blocked',
}

export function StatusBadge({ status }: { status: FrameStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}><span />{labels[status]}</span>
}
