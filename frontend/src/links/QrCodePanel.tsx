import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { ApiClientError } from '../auth/authApi'
import { getShortUrlQrCode } from './linkApi'
import type { ShortUrl } from './types'

interface QrCodePanelProps {
  link: ShortUrl
  onClose: () => void
}

export function QrCodePanel({ link, onClose }: QrCodePanelProps) {
  const [imageUrl, setImageUrl] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    let objectUrl = ''
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }

    document.addEventListener('keydown', handleKeyDown)
    getShortUrlQrCode(link.id)
      .then((blob) => {
        if (!active) return
        objectUrl = URL.createObjectURL(blob)
        setImageUrl(objectUrl)
      })
      .catch((caught: unknown) => {
        if (!active) return
        setError(caught instanceof ApiClientError ? caught.message : 'Could not generate this QR code.')
      })

    return () => {
      active = false
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKeyDown)
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [link.id, onClose])

  return createPortal(
    <div className="detail-backdrop qr-code-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="qr-code-panel" role="dialog" aria-modal="true" aria-labelledby="qr-code-title">
        <header className="qr-code-header">
          <div><p className="eyebrow"><span /> Share route</p><h2 id="qr-code-title">QR /{link.shortCode}</h2><p>Scan to open the public short URL.</p></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close QR code">x</button>
        </header>
        <div className="qr-code-body">
          {error && <div className="auth-error" role="alert">{error}</div>}
          {!error && !imageUrl && <div className="qr-code-loading"><span className="health-dot" /> Generating QR code...</div>}
          {imageUrl && <img src={imageUrl} alt={`QR code for ${link.shortUrl}`} />}
          <code>{link.shortUrl}</code>
          {imageUrl && <a className="primary-button qr-download" href={imageUrl} download={`${link.shortCode}-qr.png`}>Download PNG</a>}
        </div>
      </section>
    </div>,
    document.body,
  )
}
