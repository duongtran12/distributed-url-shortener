import { useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import { deleteShortUrl, duplicateShortUrl, updateShortUrlPin, updateShortUrlStatus } from './linkApi'
import { EditLinkPanel } from './EditLinkPanel'
import { QrCodePanel } from './QrCodePanel'
import type { ShortUrl } from './types'

interface LinkCardProps {
  link: ShortUrl
  onUpdated: (link: ShortUrl) => void
  onDeleted: (id: number) => void
  onViewAnalytics: (link: ShortUrl) => void
  selected: boolean
  onSelectionChanged: (id: number, selected: boolean) => void
  onDuplicated: (link: ShortUrl) => void
}

export function LinkCard({ link, onUpdated, onDeleted, onViewAnalytics, selected, onSelectionChanged, onDuplicated }: LinkCardProps) {
	const [working, setWorking] = useState(false)
	const [copied, setCopied] = useState(false)
	const [error, setError] = useState('')
	const [renderedAt] = useState(() => Date.now())
	const [editing, setEditing] = useState(false)
	const [showQrCode, setShowQrCode] = useState(false)

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(link.shortUrl)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1600)
    } catch {
      setError('Browser clipboard permission was denied.')
    }
  }

  async function toggleStatus() {
    setWorking(true)
    setError('')
    try {
      const updated = await updateShortUrlStatus(
        link.id,
        link.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE',
      )
      onUpdated(updated)
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not update this link.')
    } finally {
      setWorking(false)
    }
  }

  async function remove() {
    if (!window.confirm(`Delete short link ${link.shortCode}? This cannot be undone.`)) return
    setWorking(true)
    setError('')
    try {
      await deleteShortUrl(link.id)
      onDeleted(link.id)
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not delete this link.')
      setWorking(false)
    }
  }

  async function duplicate() {
    setWorking(true)
    setError('')
    try {
      onDuplicated(await duplicateShortUrl(link.id))
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not duplicate this link.')
    } finally {
      setWorking(false)
    }
  }

  async function togglePin() {
    setWorking(true)
    setError('')
    try {
      onUpdated(await updateShortUrlPin(link.id, !link.pinned))
    } catch (caught: unknown) {
      setError(caught instanceof ApiClientError ? caught.message : 'Could not update this pin.')
    } finally {
      setWorking(false)
    }
  }

	const expired = link.expiresAt ? new Date(link.expiresAt).getTime() <= renderedAt : false
  const displayStatus = expired ? 'EXPIRED' : link.status

  return (
    <article className={`link-card ${selected ? 'link-card--selected' : ''} ${link.pinned ? 'link-card--pinned' : ''}`}>
      <label className="link-selector" title={`Select /${link.shortCode}`}>
        <input type="checkbox" checked={selected} onChange={(event) => onSelectionChanged(link.id, event.target.checked)} />
        <span />
      </label>
      <div className="link-card-main">
		{link.title && <h3 className="link-display-title">{link.title}</h3>}
        <div className="link-title-row">
          <a href={link.shortUrl} target="_blank" rel="noreferrer">/{link.shortCode}</a>
          <span className={`status-pill status-pill--${displayStatus.toLowerCase()}`}>{displayStatus}</span>
		  {link.pinned && <span className="pin-indicator">PINNED</span>}
        </div>
        <p className="destination" title={link.originalUrl}>{link.originalUrl}</p>
        <div className="link-meta">
		  {link.tag && <span className="link-tag">#{link.tag}</span>}
          <span>Created {new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(link.createdAt))}</span>
          {link.expiresAt && <span>Expires {new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(link.expiresAt))}</span>}
          {link.customAlias && <span>Custom alias</span>}
        </div>
      </div>
      <div className="click-metric"><strong>{link.clickCount.toLocaleString()}</strong><span>clicks</span></div>
      <div className="link-actions">
		<button type="button" onClick={togglePin} disabled={working}>{link.pinned ? 'Unpin' : 'Pin'}</button>
        <button type="button" onClick={() => onViewAnalytics(link)}>Analytics</button>
		<button type="button" onClick={() => setShowQrCode(true)}>QR</button>
        {link.status !== 'BLOCKED' && <button type="button" onClick={() => setEditing(true)}>Edit</button>}
        <button type="button" onClick={copyLink}>{copied ? 'Copied' : 'Copy'}</button>
        {link.status !== 'BLOCKED' && <button type="button" onClick={duplicate} disabled={working}>Duplicate</button>}
        {link.status !== 'BLOCKED' && !expired && <button type="button" onClick={toggleStatus} disabled={working}>{link.status === 'ACTIVE' ? 'Disable' : 'Enable'}</button>}
        <button className="danger-action" type="button" onClick={remove} disabled={working}>Delete</button>
      </div>
      {error && <p className="link-error" role="alert">{error}</p>}
      {editing && <EditLinkPanel link={link} onClose={() => setEditing(false)} onUpdated={onUpdated} />}
	  {showQrCode && <QrCodePanel link={link} onClose={() => setShowQrCode(false)} />}
    </article>
  )
}
