import { type FormEvent, useState } from 'react'
import { ApiClientError, confirmPasswordReset, login, register, requestPasswordReset } from './authApi'
import type { UserProfile } from './types'

type Mode = 'login' | 'register' | 'forgot' | 'reset'

interface AuthPanelProps {
  initialMode: Mode
  notice?: string
  onAuthenticated: (user: UserProfile) => void
  onClose: () => void
  resetToken?: string
  onPasswordReset: () => void
  onResetTokenDismissed: () => void
}

export function AuthPanel({ initialMode, notice, onAuthenticated, onClose, resetToken = '', onPasswordReset, onResetTokenDismissed }: AuthPanelProps) {
  const [mode, setMode] = useState<Mode>(initialMode)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [requestSent, setRequestSent] = useState(false)

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

  async function handleForgotPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})
    const form = new FormData(event.currentTarget)
    try {
      await requestPasswordReset(String(form.get('email') || ''))
      setRequestSent(true)
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

  async function handleResetPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})
    const form = new FormData(event.currentTarget)
    const newPassword = String(form.get('newPassword') || '')
    const confirmation = String(form.get('confirmation') || '')
    if (newPassword !== confirmation) {
      setFieldErrors({ confirmation: 'Password confirmation does not match.' })
      setSubmitting(false)
      return
    }
    try {
      await confirmPasswordReset(resetToken, newPassword)
      onPasswordReset()
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
		if (mode === 'reset' && nextMode !== 'reset') onResetTokenDismissed()
    if (nextMode === 'forgot') setRequestSent(false)
    setMode(nextMode)
    setError('')
    setFieldErrors({})
  }

  const heading = mode === 'login' ? 'Welcome back.' : mode === 'register' ? 'Start shortening.' : mode === 'forgot' ? 'Recover access.' : 'Choose a new password.'
  const intro = mode === 'login'
    ? 'Sign in to manage links and inspect live traffic.'
    : mode === 'register'
      ? 'Create an account to launch your first tracked short link.'
      : mode === 'forgot'
        ? 'Enter your account email and we will send a secure reset link.'
        : 'Set a new password to secure your Shortwave account.'

  return (
    <div className="auth-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="auth-panel" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="auth-close" type="button" onClick={onClose} aria-label="Close authentication form">x</button>
        <p className="eyebrow"><span /> Secure access</p>
        <h2 id="auth-title">{heading}</h2>
        <p className="auth-intro">{intro}</p>
        {notice && <div className="auth-success" role="status">{notice}</div>}

        {(mode === 'login' || mode === 'register') && <form className="auth-form" onSubmit={handleSubmit}>
          {mode === 'register' && (
            <label>Display name<input name="displayName" type="text" minLength={2} maxLength={100} autoComplete="name" required />{fieldErrors.displayName && <small>{fieldErrors.displayName}</small>}</label>
          )}
          <label>Email address<input name="email" type="email" maxLength={320} autoComplete="email" required />{fieldErrors.email && <small>{fieldErrors.email}</small>}</label>
          <label>Password<input name="password" type="password" minLength={8} maxLength={72} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />{fieldErrors.password && <small>{fieldErrors.password}</small>}</label>
          {mode === 'login' && <button className="auth-forgot" type="button" onClick={() => switchMode('forgot')}>Forgot password?</button>}
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="primary-button auth-submit" type="submit" disabled={submitting}>
            {submitting ? 'Please wait...' : mode === 'login' ? 'Sign in securely' : 'Create account'}
          </button>
        </form>}

        {mode === 'forgot' && (requestSent ? (
          <div className="auth-request-sent"><div className="auth-success" role="status">If an account exists for that email, a reset link has been sent. Check the Mailpit inbox when running locally.</div><button className="secondary-button" type="button" onClick={() => switchMode('login')}>Return to sign in</button></div>
        ) : (
          <form className="auth-form" onSubmit={handleForgotPassword}>
            <label>Email address<input name="email" type="email" maxLength={320} autoComplete="email" required />{fieldErrors.email && <small>{fieldErrors.email}</small>}</label>
            {error && <div className="auth-error" role="alert">{error}</div>}
            <button className="primary-button auth-submit" type="submit" disabled={submitting}>{submitting ? 'Sending...' : 'Send reset link'}</button>
          </form>
        ))}

        {mode === 'reset' && <form className="auth-form" onSubmit={handleResetPassword}>
          <label>New password<input name="newPassword" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />{fieldErrors.newPassword && <small>{fieldErrors.newPassword}</small>}</label>
          <label>Confirm password<input name="confirmation" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />{fieldErrors.confirmation && <small>{fieldErrors.confirmation}</small>}</label>
          {error && <div className="auth-error" role="alert">{error}</div>}
          <button className="primary-button auth-submit" type="submit" disabled={submitting}>{submitting ? 'Updating...' : 'Reset password'}</button>
        </form>}

        {(mode === 'login' || mode === 'register') && <p className="auth-switch">
          {mode === 'login' ? 'New to Shortwave?' : 'Already have an account?'}{' '}
          <button type="button" onClick={() => switchMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Create an account' : 'Sign in'}
          </button>
        </p>}
        {(mode === 'forgot' || mode === 'reset') && !requestSent && <p className="auth-switch"><button type="button" onClick={() => switchMode('login')}>Back to sign in</button></p>}
      </section>
    </div>
  )
}
