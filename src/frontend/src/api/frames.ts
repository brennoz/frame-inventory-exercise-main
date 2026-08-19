import type { Frame, FrameSearch, PageResponse } from '../types/frame'
import { getJson } from './http'

export function searchFrames(search: FrameSearch, signal?: AbortSignal): Promise<PageResponse<Frame>> {
  const params = new URLSearchParams({ page: String(search.page), size: String(search.size) })
  if (search.q) params.set('q', search.q)
  if (search.status) params.set('status', search.status)
  if (search.environment) params.set('environment', search.environment)
  if (search.mediaType) params.set('mediaType', search.mediaType)
  return getJson<PageResponse<Frame>>(`/api/frames?${params}`, signal)
}

export function getFrame(frameId: string, signal?: AbortSignal): Promise<Frame> {
  return getJson<Frame>(`/api/frames/${encodeURIComponent(frameId)}`, signal)
}
