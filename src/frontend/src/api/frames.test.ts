import { afterEach, describe, expect, it, vi } from 'vitest'
import type { FrameWritePayload } from '../types/frame'
import { createFrame, importFrames, updateFrame } from './frames'
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

  it('uploads CSV data without overriding the multipart boundary', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      created: 1, updated: 0, unchanged: 0, failed: 0, errorsTruncated: false, errors: [],
    }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const file = new File(['frame_id,format\nFRAME-1,D6'], 'frames.csv', { type: 'text/csv' })

    await importFrames(file)

    const [, request] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(request.method).toBe('POST')
    expect(request.body).toBeInstanceOf(FormData)
    expect((request.body as FormData).get('file')).toBe(file)
    expect(request.headers).toEqual({ Accept: 'application/json' })
  })
})
