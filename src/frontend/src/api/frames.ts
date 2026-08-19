import type { CreateFrameRequest, Frame, FrameRevision, FrameSearch, PageResponse, UpdateFrameRequest } from '../types/frame'
import { getJson, sendJson } from './http'

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

export function createFrame(request: CreateFrameRequest): Promise<Frame> {
  return sendJson<Frame>('/api/frames', 'POST', request)
}

export function updateFrame(frameId: string, request: UpdateFrameRequest): Promise<Frame> {
  return sendJson<Frame>(`/api/frames/${encodeURIComponent(frameId)}`, 'PUT', request)
}

export function getFrameHistory(frameId: string, signal?: AbortSignal): Promise<FrameRevision[]> {
  return getJson<FrameRevision[]>(`/api/frames/${encodeURIComponent(frameId)}/history`, signal)
}
