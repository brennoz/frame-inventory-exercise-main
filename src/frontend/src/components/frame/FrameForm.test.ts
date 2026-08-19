import { describe, expect, it } from 'vitest'
import type { Frame } from '../../types/frame'
import { emptyFrameForm, frameToFormValues, toFrameWritePayload } from './frameFormModel'

const frame: Frame = {
  frameId: 'FRAME-1', mediaType: 'DIGITAL', format: 'D6', environment: 'RAIL',
  siteNumber: null, station: ' Waterloo ', address: null, region: 'London', countryCode: 'UK',
  town: 'London', postcode: null, longitude: 0, latitude: 51.5, status: 'LIVE',
  statusReason: null, numberOfSlots: 0, distanceToClosestSchool: null, pixelHeight: 0,
  pixelWidth: 1920, premium: true, createdAt: '2026-08-19T12:00:00Z',
  updatedAt: '2026-08-19T12:00:00Z', version: 2,
}

describe('frame form mapping', () => {
  it('maps a frame into editable string values without losing zeroes', () => {
    const values = frameToFormValues(frame)
    expect(values.longitude).toBe('0')
    expect(values.numberOfSlots).toBe('0')
    expect(values.pixelHeight).toBe('0')
    expect(values.premium).toBe(true)
  })

  it('trims text and maps blank optional values to null', () => {
    const values = { ...emptyFrameForm, frameId: ' FRAME-2 ', format: ' D6 ', countryCode: ' UK ', station: '  ', numberOfSlots: '0' }
    expect(toFrameWritePayload(values)).toMatchObject({
      format: 'D6', countryCode: 'UK', station: null, numberOfSlots: 0,
    })
  })
})
