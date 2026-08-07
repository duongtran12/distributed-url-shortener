import { type FormEvent, useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import { createShortUrl } from './linkApi'
import type { ShortUrl } from './types'

interface CreateLinkFormProps {
  onCreated: (shortUrl: ShortUrl) => void
  onCancel: () => void
}

export function CreateLinkForm({ onCreated, onCancel }: CreateLinkFormProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})
    const form = new FormData(event.currentTarget)
    const customAlias = String(form.get('customAlias') || '').trim()
	const title = String(form.get('title') || '').trim()
	const tag = String(form.get('tag') || '').trim()
    const localExpiration = String(form.get('expiresAt') || '')

    try {
      const created = await createShortUrl({
        originalUrl: String(form.get('originalUrl') || '').trim(),
		...(title ? { title } : {}),
		...(tag ? { tag } : {}),
        ...(customAlias ? { customAlias } : {}),
        ...(localExpiration ? { expiresAt: new Date(localExpiration).toISOString() } : {}),
      })
      onCreated(created)
    } catch (caught: unknown) {
      if (caught instanceof ApiClientError) {
        setError(caught.message)
        setFieldErrors(caught.fieldErrors)
      } else {
        setError('Could not connect to the API.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="create-link-form" onSubmit={handleSubmit}>
      <div className="form-heading">
        <div><span className="capability-index">NEW LINK</span><h2>Shorten a destination</h2></div>
        <button className="icon-button" type="button" onClick={onCancel} aria-label="Close form">x</button>
      </div>
	  <label className="wide-field">Link title <span>Optional</span><input name="title" type="text" placeholder="AWS production console" maxLength={120} />{fieldErrors.title && <small>{fieldErrors.title}</small>}</label>
      <label className="wide-field">Destination URL<input name="originalUrl" type="url" placeholder="https://example.com/a-very-long-path" maxLength={2048} required />{fieldErrors.originalUrl && <small>{fieldErrors.originalUrl}</small>}</label>
      <div className="form-grid">
		<label>Tag <span>Optional</span><input name="tag" type="text" list="short-link-tag-suggestions" placeholder="marketing" maxLength={32} pattern="[A-Za-z0-9][A-Za-z0-9_-]*" />{fieldErrors.tag && <small>{fieldErrors.tag}</small>}</label>
        <label>Custom alias <span>Optional</span><input name="customAlias" type="text" placeholder="summer-launch" minLength={4} maxLength={32} pattern="[A-Za-z0-9_-]+" />{fieldErrors.customAlias && <small>{fieldErrors.customAlias}</small>}</label>
        <label>Expiration <span>Optional</span><input name="expiresAt" type="datetime-local" />{fieldErrors.expiresAt && <small>{fieldErrors.expiresAt}</small>}</label>
      </div>
      {error && <div className="auth-error" role="alert">{error}</div>}
      <div className="form-actions"><button className="text-button" type="button" onClick={onCancel}>Cancel</button><button className="primary-button" type="submit" disabled={submitting}>{submitting ? 'Creating...' : 'Create short link'} <span>-&gt;</span></button></div>
    </form>
  )
}
