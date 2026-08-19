import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { getFrame, updateFrame } from '../api/frames'
import { ApiError } from '../api/http'
import type { Frame } from '../types/frame'
import { EditFramePage } from './EditFramePage'

vi.mock('../api/frames', () => ({ getFrame: vi.fn(), updateFrame: vi.fn() }))

const frame: Frame = {
  frameId: 'FRAME-1', mediaType: 'DIGITAL', format: 'D6', environment: 'RAIL',
  siteNumber: null, station: 'Waterloo', address: null, region: 'London', countryCode: 'UK',
  town: 'London', postcode: null, longitude: -0.11, latitude: 51.5, status: 'LIVE',
  statusReason: null, numberOfSlots: 1, distanceToClosestSchool: null, pixelHeight: 1080,
  pixelWidth: 1920, premium: false, createdAt: '2026-08-19T12:00:00Z',
  updatedAt: '2026-08-19T12:00:00Z', version: 2,
}

describe('edit frame', () => {
  it('offers to reload the latest frame after an optimistic-lock conflict', async () => {
    vi.mocked(getFrame).mockResolvedValue(frame)
    vi.mocked(updateFrame).mockRejectedValue(new ApiError('The frame was updated by another request.', 409, {
      title: 'Frame was modified', type: 'urn:problem:stale-frame-version',
    }))

    render(<MemoryRouter initialEntries={['/frames/FRAME-1/edit']}><Routes><Route path="/frames/:frameId/edit" element={<EditFramePage />} /></Routes></MemoryRouter>)
    fireEvent.click(await screen.findByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('This frame has changed')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reload latest' })).toBeInTheDocument()
    expect(updateFrame).toHaveBeenCalledWith('FRAME-1', expect.objectContaining({ version: 2 }))
  })
})
