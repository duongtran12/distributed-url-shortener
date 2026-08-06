import { type FormEvent, useState } from 'react'
import { ApiClientError, changePassword, updateProfile } from './authApi'
import type { UserProfile } from './types'

interface AccountSettingsPanelProps {
  user: UserProfile
  onClose: () => void
  onPasswordChanged: () => void
  onProfileUpdated: (profile: UserProfile) => void
}

export function AccountSettingsPanel({ user, onClose, onPasswordChanged, onProfileUpdated }: AccountSettingsPanelProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [profileSubmitting, setProfileSubmitting] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileFieldErrors, setProfileFieldErrors] = useState<Record<string, string>>({})
  const [profileSaved, setProfileSaved] = useState(false)

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setProfileSubmitting(true)
    setProfileError('')
    setProfileFieldErrors({})
    setProfileSaved(false)

    const form = new FormData(event.currentTarget)
    try {
      const updated = await updateProfile(String(form.get('displayName') || '').trim())
      onProfileUpdated(updated)
      setProfileSaved(true)
    } catch (caught: unknown) {
      if (caught instanceof ApiClientError) {
        setProfileError(caught.message)
        setProfileFieldErrors(caught.fieldErrors)
      } else {
        setProfileError('Could not connect to the API. Confirm that the backend is running.')
      }
    } finally {
      setProfileSubmitting(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldErrors({})

    const form = new FormData(event.currentTarget)
    const currentPassword = String(form.get('currentPassword') || '')
    const newPassword = String(form.get('newPassword') || '')
    const confirmation = String(form.get('confirmation') || '')

    if (newPassword !== confirmation) {
      setFieldErrors({ confirmation: 'Password confirmation does not match.' })
      return
    }

    setSubmitting(true)
    try {
      await changePassword(currentPassword, newPassword)
      onPasswordChanged()
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

  return (
    <div className="detail-backdrop settings-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="settings-panel" role="dialog" aria-modal="true" aria-labelledby="settings-title">
        <header className="settings-header">
          <div><p className="eyebrow"><span /> Account security</p><h2 id="settings-title">Account settings</h2><p>Review your profile and update the password used to access this workspace.</p></div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close account settings">x</button>
        </header>

        <dl className="profile-details">
          <div><dt>Display name</dt><dd>{user.displayName}</dd></div>
          <div><dt>Email address</dt><dd>{user.email}</dd></div>
          <div><dt>Role</dt><dd>{user.role}</dd></div>
          <div><dt>Member since</dt><dd>{new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(new Date(user.createdAt))}</dd></div>
        </dl>

        <form className="settings-form settings-profile-form" onSubmit={handleProfileSubmit}>
          <div className="settings-form-heading"><h3>Profile</h3><p>Choose the name shown across your workspace.</p></div>
          <label>Display name<input name="displayName" type="text" defaultValue={user.displayName} maxLength={100} required />{profileFieldErrors.displayName && <small>{profileFieldErrors.displayName}</small>}</label>
          {profileError && <div className="auth-error" role="alert">{profileError}</div>}
          {profileSaved && <div className="settings-success" role="status">Profile updated successfully.</div>}
          <div className="form-actions"><button className="primary-button" type="submit" disabled={profileSubmitting}>{profileSubmitting ? 'Saving...' : 'Save profile'}</button></div>
        </form>

        <form className="settings-form" onSubmit={handleSubmit}>
          <div className="settings-form-heading"><h3>Change password</h3><p>You will be signed out after the password is updated.</p></div>
          <label>Current password<input name="currentPassword" type="password" autoComplete="current-password" maxLength={72} required />{fieldErrors.currentPassword && <small>{fieldErrors.currentPassword}</small>}</label>
          <div className="form-grid">
            <label>New password<input name="newPassword" type="password" autoComplete="new-password" minLength={8} maxLength={72} required />{fieldErrors.newPassword && <small>{fieldErrors.newPassword}</small>}</label>
            <label>Confirm new password<input name="confirmation" type="password" autoComplete="new-password" minLength={8} maxLength={72} required />{fieldErrors.confirmation && <small>{fieldErrors.confirmation}</small>}</label>
          </div>
          {error && <div className="auth-error" role="alert">{error}</div>}
          <div className="form-actions"><button className="text-button" type="button" onClick={onClose}>Cancel</button><button className="primary-button" type="submit" disabled={submitting}>{submitting ? 'Updating...' : 'Update password'}</button></div>
        </form>
      </section>
    </div>
  )
}
