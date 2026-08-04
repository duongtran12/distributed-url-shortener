import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import type { UserProfile } from '../auth/types'
import { HealthBadge, type HealthState } from '../components/HealthBadge'
import { CreateLinkForm } from '../links/CreateLinkForm'
import { LinkCard } from '../links/LinkCard'
import { getShortUrls } from '../links/linkApi'
import type { ShortUrl, ShortUrlPage } from '../links/types'

interface DashboardHomeProps {
  user: UserProfile
  health: HealthState
  onLogout: () => void
}

const EMPTY_PAGE: ShortUrlPage = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
}

export function DashboardHome({ user, health, onLogout }: DashboardHomeProps) {
  const [links, setLinks] = useState<ShortUrlPage>(EMPTY_PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  const loadPage = useCallback(async (page: number) => {
    setLoading(true)
    setError('')
    try {
      setLinks(await getShortUrls(page))
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    getShortUrls(0)
      .then((page) => { if (active) setLinks(page) })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof ApiClientError ? caught.message : 'Could not load your links.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const pageClicks = useMemo(
    () => links.content.reduce((total, link) => total + link.clickCount, 0),
    [links.content],
  )
  const activeLinks = useMemo(
    () => links.content.filter((link) => link.status === 'ACTIVE' && (!link.expiresAt || new Date(link.expiresAt) > new Date())).length,
    [links.content],
  )

  function handleCreated(created: ShortUrl) {
    setShowCreate(false)
    setLinks((current) => ({
      ...current,
      content: [created, ...current.content].slice(0, current.size),
      totalElements: current.totalElements + 1,
      totalPages: Math.ceil((current.totalElements + 1) / current.size),
    }))
  }

  function handleUpdated(updated: ShortUrl) {
    setLinks((current) => ({
      ...current,
      content: current.content.map((link) => link.id === updated.id ? updated : link),
    }))
  }

  function handleDeleted(id: number) {
    setLinks((current) => ({
      ...current,
      content: current.content.filter((link) => link.id !== id),
      totalElements: Math.max(0, current.totalElements - 1),
      totalPages: Math.ceil(Math.max(0, current.totalElements - 1) / current.size),
    }))
  }

  return (
    <main className="dashboard-shell">
      <aside className="dashboard-sidebar">
        <a className="brand" href="/" aria-label="Shortwave dashboard"><span className="brand-mark">S</span><span>shortwave</span></a>
        <nav className="dashboard-nav" aria-label="Dashboard navigation">
          <a href="#overview">Overview</a>
          <a className="active" href="#links">Links</a>
          <a href="#analytics">Analytics</a>
        </nav>
        <button className="logout-button" type="button" onClick={onLogout}>Sign out</button>
      </aside>

      <section className="dashboard-content">
        <header className="workspace-header" id="overview">
          <div><p className="eyebrow"><span /> Link workspace</p><h1>{user.displayName}</h1><p>Build, control, and inspect every route from one place.</p></div>
          <div className="dashboard-identity"><HealthBadge health={health} /><span>{user.email}</span></div>
        </header>

        <section className="summary-grid" aria-label="Link summary">
          <article><span>Total links</span><strong>{links.totalElements.toLocaleString()}</strong></article>
          <article><span>Active on page</span><strong>{activeLinks.toLocaleString()}</strong></article>
          <article><span>Clicks on page</span><strong>{pageClicks.toLocaleString()}</strong></article>
        </section>

        <section className="links-section" id="links">
          <div className="section-heading">
            <div><span className="capability-index">ROUTES</span><h2>Your short links</h2></div>
            <button className="primary-button" type="button" onClick={() => setShowCreate((visible) => !visible)}>{showCreate ? 'Close form' : 'Create link'} <span>{showCreate ? 'x' : '+'}</span></button>
          </div>

          {showCreate && <CreateLinkForm onCreated={handleCreated} onCancel={() => setShowCreate(false)} />}
          {error && <div className="auth-error links-load-error" role="alert">{error} <button type="button" onClick={() => void loadPage(links.page)}>Try again</button></div>}

          {loading ? (
            <div className="links-state"><span className="health-dot" /> Loading secure links...</div>
          ) : links.content.length === 0 ? (
            <div className="dashboard-empty">
              <span className="capability-index">EMPTY / 00</span>
              <h2>No links yet.</h2>
              <p>Create your first short URL. Click events and analytics will appear here automatically.</p>
              <button className="primary-button" type="button" onClick={() => setShowCreate(true)}>Create a short link <span>-&gt;</span></button>
            </div>
          ) : (
            <div className="links-list">
              {links.content.map((link) => <LinkCard key={link.id} link={link} onUpdated={handleUpdated} onDeleted={handleDeleted} />)}
            </div>
          )}

          {links.totalPages > 1 && (
            <nav className="pagination" aria-label="Link pages">
              <button type="button" disabled={links.page === 0 || loading} onClick={() => void loadPage(links.page - 1)}>Previous</button>
              <span>Page {links.page + 1} of {links.totalPages}</span>
              <button type="button" disabled={links.page + 1 >= links.totalPages || loading} onClick={() => void loadPage(links.page + 1)}>Next</button>
            </nav>
          )}
        </section>
      </section>
    </main>
  )
}
