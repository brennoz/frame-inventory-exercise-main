import type { Frame, FrameEnvironment, FrameStatus, FrameWritePayload, MediaType } from '../../types/frame'

export type FrameFormValues = {
  frameId: string
  mediaType: MediaType
  format: string
  environment: FrameEnvironment
  siteNumber: string
  station: string
  address: string
  region: string
  countryCode: string
  town: string
  postcode: string
  longitude: string
  latitude: string
  status: FrameStatus
  statusReason: string
  numberOfSlots: string
  distanceToClosestSchool: string
  pixelHeight: string
  pixelWidth: string
  premium: boolean
}

export const emptyFrameForm: FrameFormValues = {
  frameId: '', mediaType: 'DIGITAL', format: '', environment: 'RAIL', siteNumber: '',
  station: '', address: '', region: '', countryCode: 'UK', town: '', postcode: '',
  longitude: '', latitude: '', status: 'LIVE', statusReason: '', numberOfSlots: '',
  distanceToClosestSchool: '', pixelHeight: '', pixelWidth: '', premium: false,
}

function text(value: string | null): string {
  return value ?? ''
}

function numberText(value: number | null): string {
  return value === null ? '' : String(value)
}

export function frameToFormValues(frame: Frame): FrameFormValues {
  return {
    frameId: frame.frameId, mediaType: frame.mediaType, format: frame.format,
    environment: frame.environment, siteNumber: text(frame.siteNumber), station: text(frame.station),
    address: text(frame.address), region: text(frame.region), countryCode: frame.countryCode,
    town: text(frame.town), postcode: text(frame.postcode), longitude: numberText(frame.longitude),
    latitude: numberText(frame.latitude), status: frame.status, statusReason: text(frame.statusReason),
    numberOfSlots: numberText(frame.numberOfSlots), distanceToClosestSchool: numberText(frame.distanceToClosestSchool),
    pixelHeight: numberText(frame.pixelHeight), pixelWidth: numberText(frame.pixelWidth), premium: frame.premium,
  }
}

function optionalText(value: string): string | null {
  const trimmed = value.trim()
  return trimmed || null
}

function optionalNumber(value: string): number | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : Number(trimmed)
}

export function toFrameWritePayload(values: FrameFormValues): FrameWritePayload {
  return {
    mediaType: values.mediaType, format: values.format.trim(), environment: values.environment,
    siteNumber: optionalText(values.siteNumber), station: optionalText(values.station),
    address: optionalText(values.address), region: optionalText(values.region),
    countryCode: values.countryCode.trim(), town: optionalText(values.town), postcode: optionalText(values.postcode),
    longitude: optionalNumber(values.longitude), latitude: optionalNumber(values.latitude), status: values.status,
    statusReason: optionalText(values.statusReason), numberOfSlots: optionalNumber(values.numberOfSlots),
    distanceToClosestSchool: optionalNumber(values.distanceToClosestSchool), pixelHeight: optionalNumber(values.pixelHeight),
    pixelWidth: optionalNumber(values.pixelWidth), premium: values.premium,
  }
}
