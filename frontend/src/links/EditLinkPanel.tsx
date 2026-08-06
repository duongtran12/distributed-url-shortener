import { type FormEvent, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { ApiClientError } from '../auth/authApi'
import { updateShortUrl } from './linkApi'
import type { ShortUrl } from './types'

interface EditLinkPanelProps {
  link: ShortUrl
  onClose: () => void
  onUpdated: (link: ShortUrl) => void
}

function toLocalDateTime(instant: string | null) {
  if (!instant) return ''
  const date = new Date(instant)
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 16)
}

export function EditLinkPanel({ link, onClose, onUpdated }: EditLinkPanelProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [onClose])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})

    const form = new FormData(event.currentTarget)
    const localExpiration = String(form.get('expiresAt') || '')

    try {
      const updated = await updateShortUrl(link.id, {
        originalUrl: String(form.get('originalUrl') || '').trim(),
		title: String(form.get('title') || '').trim() || null,
        expiresAt: localExpiration ? new Date(localExpiration).toISOString() : null,
      })
      onUpdated(updated)
      onClose()
    } catch (caught: unknown) {
      if (caught instanceof ApiClientError) {
        setError(caught.message)
        setFieldErrors(caught.fieldErrors)
      } else {
        setError('Could not connect to the API. Confirm that the backend is running.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return createPortal(
    <div className="detail-backdrop edit-link-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="edit-link-panel" role="dialog" aria-modal="true" aria-labelledby="edit-link-title">
        <header className="edit-link-header">
          <div><p className="eyebrow"><span /> Route configuration</p><h2 id="edit-link-title">Edit /{link.shortCode}</h2><p>The short code stays the same while its destination and expiration can change.</p></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close link editor">x</button>
        </header>

        <form className="edit-link-form" onSubmit={handleSubmit}>
          <label>Link title <span>Optional - a recognizable name for this route</span><input name="title" type="text" defaultValue={link.title ?? ''} maxLength={120} />{fieldErrors.title && <small>{fieldErrors.title}</small>}</label>
          <label>Destination URL<input name="originalUrl" type="url" defaultValue={link.originalUrl} maxLength={2048} required />{fieldErrors.originalUrl && <small>{fieldErrors.originalUrl}</small>}</label>
          <label>Expiration <span>Optional - leave empty for no expiration</span><input name="expiresAt" type="datetime-local" defaultValue={toLocalDateTime(link.expiresAt)} />{fieldErrors.expiresAt && <small>{fieldErrors.expiresAt}</small>}</label>
          {link.status !== 'ACTIVE' && <div className="edit-link-note">Saving does not reactivate this link. Enable it separately after updating the route.</div>}
          {error && <div className="auth-error" role="alert">{error}</div>}
          <div className="form-actions"><button className="text-button" type="button" onClick={onClose}>Cancel</button><button className="primary-button" type="submit" disabled={submitting}>{submitting ? 'Saving...' : 'Save changes'}</button></div>
        </form>
      </section>
    </div>,
    document.body,
  )
}
