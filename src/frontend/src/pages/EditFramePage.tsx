import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { getFrame, updateFrame } from '../api/frames'
import { ApiError } from '../api/http'
import { FrameForm } from '../components/frame/FrameForm'
import { frameToFormValues } from '../components/frame/frameFormModel'
import { FeedbackState } from '../components/ui/FeedbackState'
import { useRemoteData } from '../hooks/useRemoteData'
import type { FrameWritePayload } from '../types/frame'
import { inventoryReturnPath } from '../utils/inventoryNavigation'

export function EditFramePage() {
  const { frameId = '' } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const returnTo = inventoryReturnPath(location.state)
  const detailPath = `/frames/${encodeURIComponent(frameId)}`
  const frame = useRemoteData((signal) => getFrame(frameId, signal), [frameId])
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string>()
  const [conflict, setConflict] = useState(false)

  async function submit(_frameId: string, payload: FrameWritePayload) {
    if (frame.status !== 'success') return
    setSubmitting(true); setNotice(undefined); setConflict(false); setFieldErrors({})
    try {
      const updated = await updateFrame(frameId, { version: frame.data.version, ...payload })
      navigate(`/frames/${encodeURIComponent(updated.frameId)}`, { replace: true, state: { returnTo } })
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors); setNotice(error.message); setConflict(error.status === 409)
      } else setNotice('An unexpected error occurred while saving the frame.')
    } finally { setSubmitting(false) }
  }

  function reload() { setNotice(undefined); setConflict(false); setFieldErrors({}); frame.retry() }

  if (frame.status === 'loading') return <div className="form-page"><div className="detail-loading" role="status" aria-label="Loading frame"><span /><span /><span /></div></div>
  if (frame.status === 'error') return <div className="form-page"><Link className="back-link" to={detailPath} state={{ returnTo }}><ArrowLeft /> Back to frame</Link><FeedbackState kind="error" title="Frame could not be loaded" message={frame.error} onRetry={frame.retry} /></div>

  return <div className="form-page"><Link className="back-link" to={detailPath} state={{ returnTo }}><ArrowLeft /> Back to frame</Link><section className="form-heading"><div><h1>Edit {frame.data.frameId}</h1><p>Version {frame.data.version} · changes are recorded in frame history.</p></div></section><FrameForm key={frame.data.version} mode="edit" initialValues={frameToFormValues(frame.data)} fieldErrors={fieldErrors} notice={notice} conflict={conflict} submitting={submitting} onSubmit={submit} onReload={reload} onCancel={() => navigate(detailPath, { state: { returnTo } })} /></div>
}
