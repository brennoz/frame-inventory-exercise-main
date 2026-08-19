import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { importFrames } from '../api/frames'
import { ApiError } from '../api/http'
import { FrameImportPage } from './FrameImportPage'

vi.mock('../api/frames', () => ({ importFrames: vi.fn() }))

describe('frame CSV import', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uploads the selected file and renders partial-success counts and row errors', async () => {
    vi.mocked(importFrames).mockResolvedValue({
      created: 7, updated: 2, unchanged: 3, failed: 1, errorsTruncated: false,
      errors: [{ rowNumber: 9, frameId: 'FRAME-BAD', message: 'status must be one of LIVE, PENDING, MAINTENANCE, BLOCKED' }],
    })
    render(<MemoryRouter initialEntries={['/frames/import']}><Routes><Route path="/frames/import" element={<FrameImportPage />} /></Routes></MemoryRouter>)
    const file = new File(['frame_id,format\nFRAME-1,D6'], 'frames.csv', { type: 'text/csv' })

    fireEvent.change(screen.getByLabelText('CSV file'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'Import frames' }))

    expect(await screen.findByText('Import completed with row errors')).toBeInTheDocument()
    expect(screen.getByText('7')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('FRAME-BAD')).toBeInTheDocument()
    expect(importFrames).toHaveBeenCalledWith(file)
  })

  it('rejects a non-CSV selection before upload', () => {
    render(<MemoryRouter initialEntries={['/frames/import']}><Routes><Route path="/frames/import" element={<FrameImportPage />} /></Routes></MemoryRouter>)
    const file = new File(['not csv'], 'frames.txt', { type: 'text/plain' })

    fireEvent.change(screen.getByLabelText('CSV file'), { target: { files: [file] } })

    expect(screen.getByRole('alert')).toHaveTextContent('Choose a file with a .csv extension')
    expect(importFrames).not.toHaveBeenCalled()
  })

  it('shows structural CSV errors returned by the backend', async () => {
    vi.mocked(importFrames).mockRejectedValue(new ApiError('Missing required headers: frame_id, format', 400, {
      title: 'Invalid CSV', type: 'urn:problem:invalid-csv',
    }))
    render(<MemoryRouter initialEntries={['/frames/import']}><Routes><Route path="/frames/import" element={<FrameImportPage />} /></Routes></MemoryRouter>)
    const file = new File(['wrong_header\nvalue'], 'frames.csv', { type: 'text/csv' })

    fireEvent.change(screen.getByLabelText('CSV file'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'Import frames' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Missing required headers: frame_id, format')
  })

  it('keeps keyboard focus visible and locks file selection during upload', async () => {
    let finishImport: ((result: Awaited<ReturnType<typeof importFrames>>) => void) | undefined
    vi.mocked(importFrames).mockReturnValue(new Promise((resolve) => { finishImport = resolve }))
    render(<MemoryRouter initialEntries={['/frames/import']}><Routes><Route path="/frames/import" element={<FrameImportPage />} /></Routes></MemoryRouter>)
    const input = screen.getByLabelText('CSV file')
    const file = new File(['frame_id,format\nFRAME-1,D6'], 'frames.csv', { type: 'text/csv' })

    expect(input.closest('label')).toHaveClass('file-picker')
    fireEvent.change(input, { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: 'Import frames' }))

    expect(input).toBeDisabled()
    expect(input.closest('label')).toHaveClass('disabled')

    await act(async () => finishImport?.({ created: 1, updated: 0, unchanged: 0, failed: 0, errorsTruncated: false, errors: [] }))
    expect(await screen.findByText('Import completed')).toBeInTheDocument()
  })
})
