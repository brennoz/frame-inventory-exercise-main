import type { ReactNode } from 'react'

export function FieldValue({ label, children }: { label: string; children: ReactNode }) {
  const present = children !== null && children !== undefined && children !== ''
  return <div className="field-value"><dt>{label}</dt><dd>{present ? children : 'Not provided'}</dd></div>
}
