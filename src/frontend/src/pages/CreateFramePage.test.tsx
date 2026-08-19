import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { createFrame } from '../api/frames'
import { ApiError } from '../api/http'
import { CreateFramePage } from './CreateFramePage'

vi.mock('../api/frames', () => ({ createFrame: vi.fn() }))

describe('create frame', () => {
  it('exposes required fields to assistive technology', () => {
    render(<MemoryRouter initialEntries={['/frames/new']}><Routes><Route path="/frames/new" element={<CreateFramePage />} /></Routes></MemoryRouter>)

    expect(screen.getByLabelText(/^Frame ID/)).toBeRequired()
    expect(screen.getByLabelText(/^Media type/)).toBeRequired()
    expect(screen.getByLabelText(/^Format/)).toBeRequired()
    expect(screen.getByLabelText(/^Environment/)).toBeRequired()
    expect(screen.getByLabelText(/^Status \*/)).toBeRequired()
    expect(screen.getByLabelText(/^Country code/)).toBeRequired()
  })

  it('explains how to recover when the frame ID already exists', async () => {
    vi.mocked(createFrame).mockRejectedValue(new ApiError('Frame FRAME-1 already exists', 409, {
      title: 'Frame already exists', type: 'urn:problem:duplicate-frame',
    }))

    render(<MemoryRouter initialEntries={['/frames/new']}><Routes><Route path="/frames/new" element={<CreateFramePage />} /></Routes></MemoryRouter>)
    fireEvent.change(screen.getByLabelText(/Frame ID/), { target: { value: 'FRAME-1' } })
    fireEvent.change(screen.getByLabelText(/Format/), { target: { value: 'D6' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create frame' }))

    expect(await screen.findByText('A frame with this ID already exists. Choose another ID.')).toBeInTheDocument()
    expect(createFrame).toHaveBeenCalledWith(expect.objectContaining({ frameId: 'FRAME-1', format: 'D6' }))
  })
})
