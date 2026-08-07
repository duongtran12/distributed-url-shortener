import type { ShortUrlAuditAction, ShortUrlAuditEvent } from './auditApi'

interface AuditTimelineProps {
  events: ShortUrlAuditEvent[]
  loading: boolean
  loadingMore: boolean
  exporting: boolean
  error: string
  exportError: string
  action: ShortUrlAuditAction | ''
  hasMore: boolean
  onActionChange: (action: ShortUrlAuditAction | '') => void
  onLoadMore: () => void
  onExport: () => void
  onRetry: () => void
}

const ACTION_LABELS: Record<ShortUrlAuditAction, string> = {
  CREATED: 'Created',
  UPDATED: 'Updated',
  STATUS_CHANGED: 'Status changed',
  PIN_CHANGED: 'Priority changed',
  DUPLICATED: 'Duplicated',
  BULK_TAG_CHANGED: 'Tag changed',
  DELETED: 'Deleted',
}

export function AuditTimeline({ events, loading, loadingMore, exporting, error, exportError, action, hasMore, onActionChange, onLoadMore, onExport, onRetry }: AuditTimelineProps) {
  return (
    <section className="audit-section" id="activity">
      <div className="section-heading">
        <div><span className="capability-index">AUDIT TRAIL</span><h2>Recent activity</h2></div>
        <div className="audit-controls">
          <label className="audit-filter">
            <span>Action</span>
            <select value={action} onChange={(event) => onActionChange(event.target.value as ShortUrlAuditAction | '')}>
              <option value="">All activity</option>
              {Object.entries(ACTION_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </label>
          <button className="export-button" type="button" disabled={exporting} onClick={onExport}>{exporting ? 'Exporting...' : 'Export CSV'}</button>
        </div>
      </div>
	  {exportError && <div className="export-error" role="alert">{exportError}</div>}

      {error ? (
        <div className="auth-error links-load-error" role="alert">{error} <button type="button" onClick={onRetry}>Retry</button></div>
      ) : loading ? (
        <div className="links-state"><span className="health-dot" /> Loading activity...</div>
      ) : events.length === 0 ? (
        <div className="audit-empty">Actions such as creating, editing, or disabling links will appear here.</div>
      ) : (
        <><ol className="audit-timeline">
          {events.map((event) => (
            <li className="audit-event" key={event.id}>
              <span className={`audit-marker ${event.action === 'DELETED' ? 'audit-marker--deleted' : ''}`} />
              <div className="audit-event-content">
                <div><strong>{ACTION_LABELS[event.action]}</strong><code>/{event.shortCode}</code></div>
                <p>{event.details}</p>
              </div>
              <time dateTime={event.createdAt}>{new Date(event.createdAt).toLocaleString()}</time>
            </li>
          ))}
        </ol>{hasMore && <button className="audit-load-more" type="button" disabled={loadingMore} onClick={onLoadMore}>{loadingMore ? 'Loading...' : 'Load more activity'}</button>}</>
      )}
    </section>
  )
}
