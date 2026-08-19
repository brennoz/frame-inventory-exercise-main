import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { createFrame } from '../api/frames'
import { ApiError } from '../api/http'
import { FrameForm } from '../components/frame/FrameForm'
import { emptyFrameForm } from '../components/frame/frameFormModel'
import type { FrameWritePayload } from '../types/frame'
import { inventoryReturnPath } from '../utils/inventoryNavigation'

export function CreateFramePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const returnTo = inventoryReturnPath(location.state)
  const [submitting, setSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [notice, setNotice] = useState<string>()

  async function submit(frameId: string, payload: FrameWritePayload) {
    setSubmitting(true); setNotice(undefined); setFieldErrors({})
    try {
      const frame = await createFrame({ frameId, ...payload })
      navigate(`/frames/${encodeURIComponent(frame.frameId)}`, { replace: true, state: { returnTo } })
    } catch (error) {
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors)
        setNotice(error.status === 409 ? 'A frame with this ID already exists. Choose another ID.' : error.message)
      } else setNotice('An unexpected error occurred while saving the frame.')
    } finally { setSubmitting(false) }
  }

  return <div className="form-page"><Link className="back-link" to={returnTo}><ArrowLeft /> Back to frames</Link><section className="form-heading"><div><h1>New frame</h1><p>Add a manually managed frame to inventory.</p></div></section><FrameForm mode="create" initialValues={emptyFrameForm} fieldErrors={fieldErrors} notice={notice} submitting={submitting} onSubmit={submit} onCancel={() => navigate(returnTo)} /></div>
}
