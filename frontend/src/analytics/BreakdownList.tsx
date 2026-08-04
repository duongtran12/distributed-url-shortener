import type { CategoryClickCount } from './types'

interface BreakdownListProps {
  title: string
  data: CategoryClickCount[]
}

export function BreakdownList({ title, data }: BreakdownListProps) {
  const total = data.reduce((sum, item) => sum + item.clicks, 0)

  return (
    <section className="breakdown-card">
      <div className="breakdown-title"><h3>{title}</h3><span>{total.toLocaleString()} clicks</span></div>
      {data.length === 0 ? <p className="breakdown-empty">No traffic in this period</p> : (
        <div className="breakdown-list">
          {data.slice(0, 6).map((item) => {
            const percentage = total === 0 ? 0 : item.clicks / total * 100
            return (
              <div className="breakdown-row" key={item.category}>
                <div><span>{item.category}</span><strong>{item.clicks.toLocaleString()}</strong></div>
                <div className="breakdown-track"><i style={{ width: `${percentage}%` }} /></div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
