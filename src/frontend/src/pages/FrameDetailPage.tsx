import { ArrowLeft, CalendarClock, Crown, History, MapPin, Pencil } from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { getFrame } from '../api/frames'
import { FeedbackState } from '../components/ui/FeedbackState'
import { FieldValue } from '../components/ui/FieldValue'
import { StatusBadge } from '../components/ui/StatusBadge'
import { useRemoteData } from '../hooks/useRemoteData'
import { inventoryReturnPath } from '../utils/inventoryNavigation'

function pretty(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}

function dateTime(value: string) {
  return new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export function FrameDetailPage() {
  const { frameId = '' } = useParams()
  const location = useLocation()
  const returnTo = inventoryReturnPath(location.state)
  const frame = useRemoteData((signal) => getFrame(frameId, signal), [frameId])

  if (frame.status === 'loading') {
    return <div className="detail-page"><div className="detail-loading" role="status" aria-label="Loading frame"><span /><span /><span /></div></div>
  }
  if (frame.status === 'error') {
    return <div className="detail-page"><Link className="back-link" to={returnTo}><ArrowLeft /> Back to frames</Link><FeedbackState kind="error" title="Frame could not be loaded" message={frame.error} onRetry={frame.retry} /></div>
  }

  const data = frame.data
  return (
    <div className="detail-page">
      <Link className="back-link" to={returnTo}><ArrowLeft /> Back to frames</Link>
      <section className="detail-heading">
        <div><div className="detail-id-line"><h1>{data.frameId}</h1><StatusBadge status={data.status} />{data.premium && <span className="premium-badge"><Crown /> Premium</span>}</div><p>{data.station || data.address || 'No site description provided'}</p></div>
        <Link className="button primary heading-action" to={`/frames/${encodeURIComponent(data.frameId)}/edit`} state={{ returnTo }}><Pencil /> Edit frame</Link>
      </section>
      <nav className="detail-tabs" aria-label="Frame views">
        <Link className="active" to={`/frames/${encodeURIComponent(data.frameId)}`} state={{ returnTo }} aria-current="page">Overview</Link>
        <Link to={`/frames/${encodeURIComponent(data.frameId)}/history`} state={{ returnTo }}><History /> History</Link>
      </nav>

      <div className="detail-grid">
        <section className="detail-section"><h2>Frame specification</h2><dl>
          <FieldValue label="Media type">{pretty(data.mediaType)}</FieldValue>
          <FieldValue label="Format">{data.format}</FieldValue>
          <FieldValue label="Environment">{pretty(data.environment)}</FieldValue>
          <FieldValue label="Slots">{data.numberOfSlots}</FieldValue>
          <FieldValue label="Pixel dimensions">{data.pixelWidth !== null && data.pixelHeight !== null ? `${data.pixelWidth} × ${data.pixelHeight}` : null}</FieldValue>
          <FieldValue label="Closest school">{data.distanceToClosestSchool !== null ? `${data.distanceToClosestSchool} m` : null}</FieldValue>
        </dl></section>

        <section className="detail-section"><h2><MapPin /> Location</h2><dl>
          <FieldValue label="Site number">{data.siteNumber}</FieldValue>
          <FieldValue label="Station">{data.station}</FieldValue>
          <FieldValue label="Address">{data.address}</FieldValue>
          <FieldValue label="Town / region">{[data.town, data.region].filter(Boolean).join(', ')}</FieldValue>
          <FieldValue label="Postcode / country">{[data.postcode, data.countryCode].filter(Boolean).join(' · ')}</FieldValue>
          <FieldValue label="Coordinates">{data.latitude !== null && data.longitude !== null ? `${data.latitude}, ${data.longitude}` : null}</FieldValue>
        </dl></section>

        <section className="detail-section status-section"><h2>Status</h2><dl>
          <FieldValue label="Current status"><StatusBadge status={data.status} /></FieldValue>
          <FieldValue label="Status reason">{data.statusReason}</FieldValue>
          <FieldValue label="Premium inventory">{data.premium ? 'Yes' : 'No'}</FieldValue>
        </dl></section>

        <section className="detail-section audit-section"><h2><CalendarClock /> Record information</h2><dl>
          <FieldValue label="Created">{dateTime(data.createdAt)}</FieldValue>
          <FieldValue label="Last updated">{dateTime(data.updatedAt)}</FieldValue>
          <FieldValue label="Record version">{data.version}</FieldValue>
        </dl></section>
      </div>
    </div>
  )
}
