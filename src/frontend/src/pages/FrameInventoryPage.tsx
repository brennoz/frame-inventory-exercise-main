import { ChevronLeft, ChevronRight, Search, SlidersHorizontal, X } from 'lucide-react'
import { type FormEvent, useEffect, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import { searchFrames } from '../api/frames'
import { FeedbackState } from '../components/ui/FeedbackState'
import { LoadingTable } from '../components/ui/LoadingTable'
import { StatusBadge } from '../components/ui/StatusBadge'
import { useRemoteData } from '../hooks/useRemoteData'
import type { FrameEnvironment, FrameSearch, FrameStatus, MediaType } from '../types/frame'
import type { InventoryNavigationState } from '../utils/inventoryNavigation'

const statuses: FrameStatus[] = ['LIVE', 'PENDING', 'MAINTENANCE', 'BLOCKED']
const environments: FrameEnvironment[] = ['UNDERGROUND', 'RAIL', 'ROADSIDE', 'AIRPORT']
const mediaTypes: MediaType[] = ['CLASSIC', 'DIGITAL']

function pretty(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}

function optionalEnum<T extends string>(value: string | null, values: readonly T[]): T | undefined {
  return value && values.includes(value as T) ? value as T : undefined
}

export function FrameInventoryPage() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  const urlQuery = params.get('q') ?? ''
  const [query, setQuery] = useState(urlQuery)
  const search: FrameSearch = {
    q: params.get('q') || undefined,
    status: optionalEnum(params.get('status'), statuses),
    environment: optionalEnum(params.get('environment'), environments),
    mediaType: optionalEnum(params.get('mediaType'), mediaTypes),
    page: Math.max(0, Number(params.get('page')) || 0),
    size: [25, 50, 100].includes(Number(params.get('size'))) ? Number(params.get('size')) : 25,
  }
  const requestKey = params.toString()
  const frames = useRemoteData((signal) => searchFrames(search, signal), [requestKey])
  const hasFilters = Boolean(search.q || search.status || search.environment || search.mediaType)
  const navigationState: InventoryNavigationState = { returnTo: `${location.pathname}${location.search}` }

  useEffect(() => {
    // Browser history can change the URL without remounting this page.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setQuery(urlQuery)
  }, [urlQuery])

  function updateParam(key: string, value?: string) {
    setParams((current) => {
      const next = new URLSearchParams(current)
      if (value) next.set(key, value)
      else next.delete(key)
      if (key !== 'page') next.delete('page')
      return next
    })
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    updateParam('q', query.trim() || undefined)
  }

  function clearFilters() {
    setQuery('')
    setParams(new URLSearchParams())
  }

  const data = frames.status === 'success' ? frames.data : undefined
  const resultStart = data && data.totalElements > 0 ? data.page * data.size + 1 : 0
  const resultEnd = data ? Math.min((data.page + 1) * data.size, data.totalElements) : 0

  return (
    <div className="workspace-page">
      <section className="page-heading">
        <div><h1>Frames</h1><p>{data ? `${data.totalElements.toLocaleString()} frames found` : 'Search and inspect inventory frames'}</p></div>
      </section>

      <div className="inventory-layout">
        <aside className="filter-panel" aria-label="Frame filters">
          <div className="filter-title"><SlidersHorizontal /><h2>Filters</h2>{hasFilters && <button onClick={clearFilters}>Clear all</button>}</div>
          <form className="search-field" onSubmit={submitSearch}>
            <button className="search-submit" type="submit" aria-label="Search frames"><Search /></button>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Frame ID, site or location" aria-label="Search frames" />
            {query && <button className="search-clear" type="button" onClick={() => { setQuery(''); updateParam('q') }} aria-label="Clear search"><X /></button>}
          </form>
          <label className="select-field"><span>Status</span><select value={search.status ?? ''} onChange={(event) => updateParam('status', event.target.value)}><option value="">All statuses</option>{statuses.map((value) => <option key={value} value={value}>{pretty(value)}</option>)}</select></label>
          <label className="select-field"><span>Environment</span><select value={search.environment ?? ''} onChange={(event) => updateParam('environment', event.target.value)}><option value="">All environments</option>{environments.map((value) => <option key={value} value={value}>{pretty(value)}</option>)}</select></label>
          <label className="select-field"><span>Media type</span><select value={search.mediaType ?? ''} onChange={(event) => updateParam('mediaType', event.target.value)}><option value="">All media types</option>{mediaTypes.map((value) => <option key={value} value={value}>{pretty(value)}</option>)}</select></label>
        </aside>

        <section className="results-panel" aria-live="polite">
          <div className="results-toolbar">
            <div><strong>Inventory results</strong><span>{data ? `${resultStart}-${resultEnd} of ${data.totalElements.toLocaleString()}` : 'Loading results'}</span></div>
            <label>Rows per page<select value={search.size} onChange={(event) => updateParam('size', event.target.value)}><option>25</option><option>50</option><option>100</option></select></label>
          </div>

          {frames.status === 'loading' && <LoadingTable />}
          {frames.status === 'error' && <FeedbackState kind="error" title="Frames could not be loaded" message={frames.error} onRetry={frames.retry} />}
          {data && data.items.length === 0 && <FeedbackState kind="empty" title="No frames found" message={hasFilters ? 'Try broadening or clearing the current filters.' : 'Import or create a frame to begin building the inventory.'} />}
          {data && data.items.length > 0 && (
            <div className="table-scroll">
              <table className="inventory-table">
                <thead><tr><th>Frame ID</th><th>Site</th><th>Location</th><th>Format</th><th>Environment</th><th>Status</th><th>Updated</th><th><span className="sr-only">Open</span></th></tr></thead>
                <tbody>{data.items.map((frame) => (
                  <tr key={frame.frameId}>
                    <td><Link className="frame-id" to={`/frames/${encodeURIComponent(frame.frameId)}`} state={navigationState}>{frame.frameId}</Link>{frame.premium && <span className="premium-label">Premium</span>}</td>
                    <td><strong>{frame.station || frame.siteNumber || 'Unassigned site'}</strong><small>{frame.siteNumber && frame.station ? frame.siteNumber : frame.address || 'No address'}</small></td>
                    <td><strong>{frame.town || 'Unknown town'}</strong><small>{frame.region || frame.countryCode}</small></td>
                    <td><strong>{pretty(frame.mediaType)}</strong><small>{frame.format}{frame.pixelWidth !== null && frame.pixelHeight !== null ? ` · ${frame.pixelWidth}×${frame.pixelHeight}` : ''}</small></td>
                    <td>{pretty(frame.environment)}</td>
                    <td><StatusBadge status={frame.status} /></td>
                    <td><time dateTime={frame.updatedAt}>{new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(new Date(frame.updatedAt))}</time></td>
                    <td><Link className="row-open" to={`/frames/${encodeURIComponent(frame.frameId)}`} state={navigationState} aria-label={`Open ${frame.frameId}`}><ChevronRight /></Link></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}

          {data && data.totalPages > 0 && (
            <nav className="pagination" aria-label="Results pages">
              <span>Page {data.page + 1} of {data.totalPages}</span>
              <div>
                <button className="icon-button" disabled={data.page === 0} onClick={() => updateParam('page', String(data.page - 1))} aria-label="Previous page"><ChevronLeft /></button>
                <button className="icon-button" disabled={data.page + 1 >= data.totalPages} onClick={() => updateParam('page', String(data.page + 1))} aria-label="Next page"><ChevronRight /></button>
              </div>
            </nav>
          )}
        </section>
      </div>
    </div>
  )
}
