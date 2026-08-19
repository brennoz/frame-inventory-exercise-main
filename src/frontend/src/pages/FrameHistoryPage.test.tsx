import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { getFrameHistory } from '../api/frames'
import { FrameHistoryPage } from './FrameHistoryPage'

vi.mock('../api/frames', () => ({ getFrameHistory: vi.fn() }))

describe('frame history', () => {
  it('renders newest-first field changes with their source and actor', async () => {
    vi.mocked(getFrameHistory).mockResolvedValue([{
      id: 2, action: 'UPDATED', source: 'MANUAL', actor: 'demo-user', occurredAt: '2026-08-19T14:00:00Z',
      changes: [
        { fieldName: 'status', oldValue: 'LIVE', newValue: 'MAINTENANCE' },
        { fieldName: 'countryCode', oldValue: null, newValue: 'UK' },
      ],
    }])

    render(<MemoryRouter initialEntries={['/frames/FRAME-1/history']}><Routes><Route path="/frames/:frameId/history" element={<FrameHistoryPage />} /></Routes></MemoryRouter>)
    expect(await screen.findByText('Status')).toBeInTheDocument()
    expect(screen.getByText('Live')).toBeInTheDocument()
    expect(screen.getByText('Maintenance')).toBeInTheDocument()
    expect(screen.getByText('UK')).toBeInTheDocument()
    expect(screen.getByText(/demo-user/)).toBeInTheDocument()
  })
})
