export type FrameStatus = 'LIVE' | 'PENDING' | 'MAINTENANCE' | 'BLOCKED'
export type FrameEnvironment = 'UNDERGROUND' | 'RAIL' | 'ROADSIDE' | 'AIRPORT'
export type MediaType = 'CLASSIC' | 'DIGITAL'

export type Frame = {
  frameId: string
  mediaType: MediaType
  format: string
  environment: FrameEnvironment
  siteNumber: string | null
  station: string | null
  address: string | null
  region: string | null
  countryCode: string
  town: string | null
  postcode: string | null
  longitude: number | null
  latitude: number | null
  status: FrameStatus
  statusReason: string | null
  numberOfSlots: number | null
  distanceToClosestSchool: number | null
  pixelHeight: number | null
  pixelWidth: number | null
  premium: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export type PageResponse<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type FrameSearch = {
  q?: string
  status?: FrameStatus
  environment?: FrameEnvironment
  mediaType?: MediaType
  page: number
  size: number
}

export type FrameWritePayload = {
  mediaType: MediaType
  format: string
  environment: FrameEnvironment
  siteNumber: string | null
  station: string | null
  address: string | null
  region: string | null
  countryCode: string
  town: string | null
  postcode: string | null
  longitude: number | null
  latitude: number | null
  status: FrameStatus
  statusReason: string | null
  numberOfSlots: number | null
  distanceToClosestSchool: number | null
  pixelHeight: number | null
  pixelWidth: number | null
  premium: boolean
}

export type CreateFrameRequest = FrameWritePayload & { frameId: string }
export type UpdateFrameRequest = FrameWritePayload & { version: number }

export type RevisionAction = 'CREATED' | 'UPDATED'
export type ChangeSource = 'MANUAL' | 'CSV'

export type FrameChange = {
  fieldName: string
  oldValue: string | null
  newValue: string | null
}

export type FrameRevision = {
  id: number
  action: RevisionAction
  source: ChangeSource
  actor: string
  occurredAt: string
  changes: FrameChange[]
}
