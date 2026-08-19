import { Boxes, Database, Frame, History, Menu, Settings, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { inventoryReturnPath } from '../../utils/inventoryNavigation'

export function AppShell() {
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const isDetail = location.pathname !== '/frames'
  const framesPath = isDetail ? inventoryReturnPath(location.state) : '/frames'
  const pageLabel = location.pathname === '/frames/new'
    ? 'New frame'
    : location.pathname === '/frames/import'
      ? 'Import frames'
    : location.pathname.endsWith('/edit')
      ? 'Edit frame'
      : location.pathname.endsWith('/history')
        ? 'History'
        : 'Frame detail'

  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 961px)')
    const closeForDesktop = (event: MediaQueryListEvent) => {
      if (event.matches) setMenuOpen(false)
    }
    desktop.addEventListener('change', closeForDesktop)
    return () => desktop.removeEventListener('change', closeForDesktop)
  }, [])

  return (
    <div className="app-shell">
      <header className="brand-bar">
        <Link className="brand" to="/frames" aria-label="Global Inventory home">
          <span className="brand-mark" aria-hidden="true">G</span>
          <span><strong>Global Inventory</strong><small>Frames · OOH</small></span>
        </Link>
        <button
          className="icon-button mobile-menu-button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-label={menuOpen ? 'Close navigation' : 'Open navigation'}
          aria-expanded={menuOpen}
        >
          {menuOpen ? <X /> : <Menu />}
        </button>
      </header>

      <header className="top-bar">
        <div className="breadcrumbs" aria-label="Breadcrumb">
          <span>Inventory</span><span>/</span>
          <Link to={framesPath}>Frames</Link>
          {isDetail && <><span>/</span><strong>{pageLabel}</strong></>}
        </div>
        <div className="user-summary" aria-label="Current user">
          <span className="avatar">DU</span>
          <span><strong>Demo user</strong><small>Inventory operations</small></span>
        </div>
      </header>

      <aside className={`navigation-rail ${menuOpen ? 'open' : ''}`} aria-label="Primary navigation">
        <p className="nav-label">Inventory</p>
        <NavLink to="/frames" onClick={() => setMenuOpen(false)}><Frame /> Frames</NavLink>
        <p className="nav-label">Workspace</p>
        <span className="nav-disabled"><History /> Activity</span>
        <span className="nav-disabled"><Database /> Master data</span>
        <span className="nav-disabled"><Settings /> Settings</span>
        <div className="rail-footer"><Boxes /> Frame Inventory <span>v1.0</span></div>
      </aside>

      {menuOpen && <button className="nav-backdrop" onClick={() => setMenuOpen(false)} aria-label="Close navigation" />}
      <main className="app-content"><Outlet /></main>
    </div>
  )
}
