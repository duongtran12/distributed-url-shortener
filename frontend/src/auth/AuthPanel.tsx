import { type FormEvent, useState } from 'react'
import { ApiClientError, login, register } from './authApi'
import type { UserProfile } from './types'

type Mode = 'login' | 'register'

interface AuthPanelProps {
  initialMode: Mode
  onAuthenticated: (user: UserProfile) => void
  onClose: () => void
}

export function AuthPanel({ initialMode, onAuthenticated, onClose }: AuthPanelProps) {
  const [mode, setMode] = useState<Mode>(initialMode)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})
    const form = new FormData(event.currentTarget)

    try {
      const email = String(form.get('email') || '')
      const password = String(form.get('password') || '')
      const profile = mode === 'login'
        ? await login(email, password)
        : await register(String(form.get('displayName') || ''), email, password)
      onAuthenticated(profile)
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

  function switchMode(nextMode: Mode) {
    setMode(nextMode)
    setError('')
    setFieldErrors({})
  }

  return (
    <div className="auth-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="auth-panel" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="auth-close" type="button" onClick={onClose} aria-label="Close authentication form">x</button>
        <p className="eyebrow"><span /> Secure access</p>
        <h2 id="auth-title">{mode === 'login' ? 'Welcome back.' : 'Start shortening.'}</h2>
        <p className="auth-intro">
          {mode === 'login' ? 'Sign in to manage links and inspect live traffic.' : 'Create an account to launch your first tracked short link.'}
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          {mode === 'register' && (
            <label>Display name<input name="displayName" type="text" minLength={2} maxLength={100} autoComplete="name" required />{fieldErrors.displayName && <small>{fieldErrors.displayName}</small>}</label>
          )}
          <label>Email address<input name="email" type="email" maxLength={320} autoComplete="email" required />{fieldErrors.email && <small>{fieldErrors.email}</small>}</label>
          <label>Password<input name="password" type="password" minLength={8} maxLength={72} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />{fieldErrors.password && <small>{fieldErrors.password}</small>}</label>
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="primary-button auth-submit" type="submit" disabled={submitting}>
            {submitting ? 'Please wait...' : mode === 'login' ? 'Sign in securely' : 'Create account'}
          </button>
        </form>

        <p className="auth-switch">
          {mode === 'login' ? 'New to Shortwave?' : 'Already have an account?'}{' '}
          <button type="button" onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Create an account' : 'Sign in'}
          </button>
        </p>
      </section>
    </div>
  )
}
