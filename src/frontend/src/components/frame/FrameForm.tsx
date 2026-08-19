import { AlertCircle, Check, LoaderCircle } from 'lucide-react'
import { type ChangeEvent, type FormEvent, type ReactNode, useState } from 'react'
import type { FrameFormValues } from './frameFormModel'
import { toFrameWritePayload } from './frameFormModel'
import type { FrameWritePayload } from '../../types/frame'

type Props = {
  mode: 'create' | 'edit'
  initialValues: FrameFormValues
  fieldErrors?: Record<string, string>
  notice?: string
  conflict?: boolean
  submitting: boolean
  onSubmit: (frameId: string, payload: FrameWritePayload) => void
  onCancel: () => void
  onReload?: () => void
}

type FieldProps = {
  label: string
  name: keyof FrameFormValues
  error?: string
  required?: boolean
  className?: string
  children: ReactNode
}

function Field({ label, name, error, required, className = '', children }: FieldProps) {
  return <label className={`form-field ${className}`} htmlFor={name}><span>{label}{required && <b aria-hidden="true"> *</b>}</span>{children}{error && <small id={`${name}-error`} className="field-error">{error}</small>}</label>
}

function validate(values: FrameFormValues, mode: Props['mode']): Record<string, string> {
  const errors: Record<string, string> = {}
  if (mode === 'create' && !values.frameId.trim()) errors.frameId = 'Frame ID is required'
  else if (mode === 'create' && !/^[A-Za-z0-9_-]+$/.test(values.frameId.trim())) errors.frameId = 'Use only letters, numbers, underscores, or hyphens'
  if (!values.format.trim()) errors.format = 'Format is required'
  if (!values.countryCode.trim()) errors.countryCode = 'Country code is required'

  const nonNegativeIntegers: Array<keyof FrameFormValues> = ['numberOfSlots', 'distanceToClosestSchool', 'pixelHeight', 'pixelWidth']
  nonNegativeIntegers.forEach((field) => {
    const value = values[field]
    if (typeof value === 'string' && value !== '' && (!Number.isInteger(Number(value)) || Number(value) < 0)) errors[field] = 'Enter a whole number of zero or more'
  })
  const longitude = values.longitude === '' ? null : Number(values.longitude)
  const latitude = values.latitude === '' ? null : Number(values.latitude)
  if (longitude !== null && (!Number.isFinite(longitude) || longitude < -180 || longitude > 180)) errors.longitude = 'Enter a value from -180 to 180'
  if (latitude !== null && (!Number.isFinite(latitude) || latitude < -90 || latitude > 90)) errors.latitude = 'Enter a value from -90 to 90'
  return errors
}

export function FrameForm({ mode, initialValues, fieldErrors = {}, notice, conflict, submitting, onSubmit, onCancel, onReload }: Props) {
  const [values, setValues] = useState(initialValues)
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({})
  const errors = { ...fieldErrors, ...clientErrors }

  function change(event: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    const { name, type, value } = event.target
    const next = type === 'checkbox' ? (event.target as HTMLInputElement).checked : value
    setValues((current) => ({ ...current, [name]: next }))
    setClientErrors((current) => {
      if (!current[name]) return current
      const nextErrors = { ...current }
      delete nextErrors[name]
      return nextErrors
    })
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validationErrors = validate(values, mode)
    setClientErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return
    onSubmit(values.frameId.trim(), toFrameWritePayload(values))
  }

  const input = (name: keyof FrameFormValues) => ({ name, id: name, value: String(values[name]), onChange: change, 'aria-invalid': Boolean(errors[name]), 'aria-describedby': errors[name] ? `${name}-error` : undefined })

  return (
    <form className="frame-form" onSubmit={submit} noValidate>
      {notice && <div className={`form-notice ${conflict ? 'conflict' : 'error'}`} role="alert"><AlertCircle /><div><strong>{conflict ? 'This frame has changed' : 'Frame could not be saved'}</strong><span>{notice}</span></div>{conflict && onReload && <button type="button" className="button secondary" onClick={onReload}>Reload latest</button>}</div>}

      <section className="form-section"><header><h2>Frame details</h2><p>Identity, media specification, and current operational status.</p></header><div className="form-grid">
        <Field label="Frame ID" name="frameId" required error={errors.frameId}><input {...input('frameId')} required disabled={mode === 'edit'} maxLength={64} /></Field>
        <Field label="Media type" name="mediaType" required error={errors.mediaType}><select {...input('mediaType')} required><option value="DIGITAL">Digital</option><option value="CLASSIC">Classic</option></select></Field>
        <Field label="Format" name="format" required error={errors.format}><input {...input('format')} required maxLength={100} /></Field>
        <Field label="Environment" name="environment" required error={errors.environment}><select {...input('environment')} required><option value="UNDERGROUND">Underground</option><option value="RAIL">Rail</option><option value="ROADSIDE">Roadside</option><option value="AIRPORT">Airport</option></select></Field>
        <Field label="Status" name="status" required error={errors.status}><select {...input('status')} required><option value="LIVE">Live</option><option value="PENDING">Pending</option><option value="MAINTENANCE">Maintenance</option><option value="BLOCKED">Blocked</option></select></Field>
        <Field label="Status reason" name="statusReason" error={errors.statusReason}><input {...input('statusReason')} maxLength={255} /></Field>
        <label className="checkbox-field"><input name="premium" type="checkbox" checked={values.premium} onChange={change} /><span>Premium inventory</span></label>
      </div></section>

      <section className="form-section"><header><h2>Site and address</h2><p>Where the frame is installed and how operators identify the site.</p></header><div className="form-grid">
        <Field label="Site number" name="siteNumber" error={errors.siteNumber}><input {...input('siteNumber')} maxLength={64} /></Field>
        <Field label="Station" name="station" error={errors.station}><input {...input('station')} maxLength={150} /></Field>
        <Field label="Address" name="address" error={errors.address} className="span-2"><textarea {...input('address')} maxLength={500} rows={3} /></Field>
        <Field label="Town" name="town" error={errors.town}><input {...input('town')} maxLength={150} /></Field>
        <Field label="Region" name="region" error={errors.region}><input {...input('region')} maxLength={100} /></Field>
        <Field label="Postcode" name="postcode" error={errors.postcode}><input {...input('postcode')} maxLength={16} /></Field>
        <Field label="Country code" name="countryCode" required error={errors.countryCode}><input {...input('countryCode')} required maxLength={8} /></Field>
      </div></section>

      <section className="form-section"><header><h2>Position and capacity</h2><p>Coordinates, display dimensions, slots, and proximity information.</p></header><div className="form-grid">
        <Field label="Longitude" name="longitude" error={errors.longitude}><input {...input('longitude')} type="number" min="-180" max="180" step="0.00000001" /></Field>
        <Field label="Latitude" name="latitude" error={errors.latitude}><input {...input('latitude')} type="number" min="-90" max="90" step="0.00000001" /></Field>
        <Field label="Number of slots" name="numberOfSlots" error={errors.numberOfSlots}><input {...input('numberOfSlots')} type="number" min="0" step="1" /></Field>
        <Field label="Closest school (m)" name="distanceToClosestSchool" error={errors.distanceToClosestSchool}><input {...input('distanceToClosestSchool')} type="number" min="0" step="1" /></Field>
        <Field label="Pixel width" name="pixelWidth" error={errors.pixelWidth}><input {...input('pixelWidth')} type="number" min="0" step="1" /></Field>
        <Field label="Pixel height" name="pixelHeight" error={errors.pixelHeight}><input {...input('pixelHeight')} type="number" min="0" step="1" /></Field>
      </div></section>

      <footer className="form-actions"><button type="button" className="button secondary" onClick={onCancel} disabled={submitting}>Cancel</button><button type="submit" className="button primary" disabled={submitting}>{submitting ? <LoaderCircle className="spin" /> : <Check />}{submitting ? 'Saving…' : mode === 'create' ? 'Create frame' : 'Save changes'}</button></footer>
    </form>
  )
}
