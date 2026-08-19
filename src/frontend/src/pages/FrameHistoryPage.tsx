import { ArrowLeft, ArrowRight, Clock3, FileClock } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getFrameHistory } from '../api/frames'
import { FeedbackState } from '../components/ui/FeedbackState'
import { useRemoteData } from '../hooks/useRemoteData'
import { inventoryReturnPath } from '../utils/inventoryNavigation'

const fieldLabels: Record<string, string> = {
  frameId: 'Frame ID', mediaType: 'Media type', format: 'Format', environment: 'Environment',
  siteNumber: 'Site number', station: 'Station', address: 'Address', region: 'Region',
  countryCode: 'Country code', town: 'Town', postcode: 'Postcode', longitude: 'Longitude',
  latitude: 'Latitude', status: 'Status', statusReason: 'Status reason', numberOfSlots: 'Number of slots',
  distanceToClosestSchool: 'Distance to closest school', pixelHeight: 'Pixel height',
  pixelWidth: 'Pixel width', premium: 'Premium',
}

function titleCase(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}

function displayValue(fieldName: string, value: string | null) {
  if (value === null || value === '') return 'Not provided'
  if (value === 'true' || value === 'false') return value === 'true' ? 'Yes' : 'No'
  return ['mediaType', 'environment', 'status'].includes(fieldName) ? titleCase(value) : value
}

export function FrameHistoryPage() {
  const { frameId = '' } = useParams()
  const location = useLocation()
  const returnTo = inventoryReturnPath(location.state)
  const history = useRemoteData((signal) => getFrameHistory(frameId, signal), [frameId])

  return (
    <div className="detail-page">
      <Link className="back-link" to={returnTo}><ArrowLeft /> Back to frames</Link>
      <section className="detail-heading"><div><h1>{frameId}</h1><p>Revision history and field-level changes</p></div></section>
      <nav className="detail-tabs" aria-label="Frame views">
        <Link to={`/frames/${encodeURIComponent(frameId)}`} state={location.state}>Overview</Link>
        <Link className="active" to={`/frames/${encodeURIComponent(frameId)}/history`} state={location.state} aria-current="page">History</Link>
      </nav>

      {history.status === 'loading' && <div className="history-loading" role="status">Loading revision history…</div>}
      {history.status === 'error' && <div className="history-content"><FeedbackState kind="error" title="History could not be loaded" message={history.error} onRetry={history.retry} /></div>}
      {history.status === 'success' && history.data.length === 0 && <div className="history-content"><FeedbackState kind="empty" title="No revision history" message="This frame has not recorded any changes yet." /></div>}
      {history.status === 'success' && history.data.length > 0 && (
        <section className="history-content" aria-label="Revision history">
          {history.data.map((revision) => (
            <article className="revision" key={revision.id}>
              <div className="revision-marker"><FileClock /></div>
              <div className="revision-body">
                <header>
                  <div><strong>{revision.action === 'CREATED' ? 'Frame created' : 'Frame updated'}</strong><span>{revision.source === 'CSV' ? 'CSV import' : 'Manual'} by {revision.actor}</span></div>
                  <time dateTime={revision.occurredAt}><Clock3 />{new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(revision.occurredAt))}</time>
                </header>
                <div className="change-list">
                  {revision.changes.map((change) => (
                    <div className="change-row" key={change.fieldName}>
                      <strong>{fieldLabels[change.fieldName] ?? change.fieldName}</strong>
                      <span className="change-value old">{displayValue(change.fieldName, change.oldValue)}</span>
                      <ArrowRight />
                      <span className="change-value new">{displayValue(change.fieldName, change.newValue)}</span>
                    </div>
                  ))}
                </div>
              </div>
            </article>
          ))}
        </section>
      )}
    </div>
  )
}
