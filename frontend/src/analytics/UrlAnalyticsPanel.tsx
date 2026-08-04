import { useEffect, useState } from 'react'
import { ApiClientError } from '../auth/authApi'
import type { ShortUrl } from '../links/types'
import { getUrlAnalytics } from './analyticsApi'
import { BreakdownList } from './BreakdownList'
import { TrafficChart } from './TrafficChart'
import type { UrlAnalytics } from './types'

interface UrlAnalyticsPanelProps {
  link: ShortUrl
  onClose: () => void
}

export function UrlAnalyticsPanel({ link, onClose }: UrlAnalyticsPanelProps) {
  const [analytics, setAnalytics] = useState<UrlAnalytics | null>(null)
  const [days, setDays] = useState(30)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let active = true
    getUrlAnalytics(link.id, days)
      .then((result) => { if (active) setAnalytics(result) })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof ApiClientError ? caught.message : 'Could not load URL analytics.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [link.id, days, refresh])

  function changeRange(nextDays: number) {
    if (nextDays === days) return
    setLoading(true)
    setError('')
    setDays(nextDays)
  }

  function retry() {
    setLoading(true)
    setError('')
    setRefresh((current) => current + 1)
  }

  return (
    <div className="detail-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="analytics-detail" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <header className="detail-header">
          <div><p className="eyebrow"><span /> URL intelligence</p><h2 id="detail-title">/{link.shortCode}</h2><p title={link.originalUrl}>{link.originalUrl}</p></div>
          <div className="detail-controls"><div className="range-picker">{[7, 30, 90].map((range) => <button className={days === range ? 'active' : ''} type="button" key={range} onClick={() => changeRange(range)}>{range}D</button>)}</div><button className="icon-button" type="button" onClick={onClose} aria-label="Close analytics">x</button></div>
        </header>

        {error ? <div className="auth-error links-load-error">{error}<button type="button" onClick={retry}>Retry</button></div> : (
          <div className={loading ? 'detail-body detail-body--loading' : 'detail-body'}>
            <section className="detail-summary">
              <article><span>Lifetime clicks</span><strong>{(analytics?.lifetimeClicks ?? 0).toLocaleString()}</strong></article>
              <article><span>Lifetime visitors</span><strong>{(analytics?.lifetimeUniqueVisitors ?? 0).toLocaleString()}</strong></article>
              <article><span>Period clicks</span><strong>{(analytics?.periodClicks ?? 0).toLocaleString()}</strong></article>
              <article><span>Period visitors</span><strong>{(analytics?.periodUniqueVisitors ?? 0).toLocaleString()}</strong></article>
            </section>
            <section className="detail-chart"><div className="breakdown-title"><h3>Daily traffic</h3><span>{analytics?.from} / {analytics?.to}</span></div><TrafficChart data={analytics?.dailyClicks ?? []} /></section>
            <div className="breakdown-grid">
              <BreakdownList title="Browsers" data={analytics?.browsers ?? []} />
              <BreakdownList title="Operating systems" data={analytics?.operatingSystems ?? []} />
              <BreakdownList title="Devices" data={analytics?.devices ?? []} />
              <BreakdownList title="Referrers" data={analytics?.referrers ?? []} />
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
