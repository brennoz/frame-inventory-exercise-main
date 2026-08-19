import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getFrame, searchFrames } from '../api/frames'
import type { Frame } from '../types/frame'
import { FrameDetailPage } from './FrameDetailPage'
import { FrameInventoryPage } from './FrameInventoryPage'

vi.mock('../api/frames', () => ({
  getFrame: vi.fn(),
  searchFrames: vi.fn(),
}))

const frame: Frame = {
  frameId: 'FRAME-1',
  mediaType: 'DIGITAL',
  format: 'D6',
  environment: 'RAIL',
  siteNumber: 'SITE-1',
  station: 'Waterloo',
  address: 'Platform 1',
  region: 'London',
  countryCode: 'UK',
  town: 'London',
  postcode: 'SE1',
  longitude: -0.11,
  latitude: 51.5,
  status: 'LIVE',
  statusReason: null,
  numberOfSlots: 1,
  distanceToClosestSchool: 300,
  pixelHeight: 0,
  pixelWidth: 0,
  premium: false,
  createdAt: '2026-08-19T12:00:00Z',
  updatedAt: '2026-08-19T12:00:00Z',
  version: 0,
}

function HistoryHarness() {
  const navigate = useNavigate()
  return <><button onClick={() => navigate(-1)}>Browser back</button><FrameInventoryPage /></>
}

function LocationProbe() {
  const location = useLocation()
  return <output>{`${location.pathname}${location.search}`}</output>
}

describe('frame navigation', () => {
  beforeEach(() => {
    vi.mocked(searchFrames).mockResolvedValue({ items: [], page: 0, size: 25, totalElements: 0, totalPages: 0 })
    vi.mocked(getFrame).mockResolvedValue(frame)
  })

  it('keeps the search input synchronized with browser history', async () => {
    render(
      <MemoryRouter initialEntries={['/frames?q=first', '/frames?q=second']} initialIndex={1}>
        <Routes><Route path="/frames" element={<HistoryHarness />} /></Routes>
      </MemoryRouter>,
    )

    expect(screen.getByRole('textbox', { name: 'Search frames' })).toHaveValue('second')
    fireEvent.click(screen.getByRole('button', { name: 'Browser back' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Search frames' })).toHaveValue('first'))
  })

  it('returns from detail to the originating filtered inventory URL', async () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/frames/FRAME-1', state: { returnTo: '/frames?q=Waterloo&status=LIVE&page=2' } }]}>
        <Routes>
          <Route path="/frames/:frameId" element={<FrameDetailPage />} />
          <Route path="/frames" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('link', { name: 'Back to frames' }))
    expect(await screen.findByText('/frames?q=Waterloo&status=LIVE&page=2')).toBeInTheDocument()
  })

  it('renders valid zero-valued pixel dimensions', async () => {
    render(
      <MemoryRouter initialEntries={['/frames/FRAME-1']}>
        <Routes><Route path="/frames/:frameId" element={<FrameDetailPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('0 × 0')).toBeInTheDocument()
  })
})
