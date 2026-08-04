export type HealthState = 'checking' | 'online' | 'offline'

export function HealthBadge({ health }: { health: HealthState }) {
  return (
    <span className={`health health--${health}`}>
      <span className="health-dot" />
      {health === 'checking' ? 'Checking API' : health === 'online' ? 'API online' : 'API offline'}
    </span>
  )
}
