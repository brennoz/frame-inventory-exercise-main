import { afterEach, describe, expect, it, vi } from 'vitest'
import type { FrameWritePayload } from '../types/frame'
import { createFrame, updateFrame } from './frames'
import { ApiError } from './http'

const payload: FrameWritePayload = {
  mediaType: 'DIGITAL', format: 'D6', environment: 'RAIL', siteNumber: null,
  station: null, address: null, region: null, countryCode: 'UK', town: null,
  postcode: null, longitude: null, latitude: null, status: 'LIVE', statusReason: null,
  numberOfSlots: null, distanceToClosestSchool: null, pixelHeight: null, pixelWidth: null,
  premium: false,
}

afterEach(() => vi.unstubAllGlobals())

describe('frame mutation API', () => {
  it('posts a create request as JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ frameId: 'FRAME-2' }), { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)
    await createFrame({ frameId: 'FRAME-2', ...payload })
    expect(fetchMock).toHaveBeenCalledWith('/api/frames', expect.objectContaining({ method: 'POST', body: JSON.stringify({ frameId: 'FRAME-2', ...payload }) }))
  })

  it('puts the record version and exposes server field errors', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      title: 'Invalid frame', detail: 'One or more frame fields are invalid', errors: { format: 'must not be blank' },
    }), { status: 400, headers: { 'Content-Type': 'application/problem+json' } }))
    vi.stubGlobal('fetch', fetchMock)

    const error = await updateFrame('FRAME/2', { version: 3, ...payload }).catch((reason: unknown) => reason)
    expect(fetchMock).toHaveBeenCalledWith('/api/frames/FRAME%2F2', expect.objectContaining({ method: 'PUT' }))
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).fieldErrors).toEqual({ format: 'must not be blank' })
  })
})
