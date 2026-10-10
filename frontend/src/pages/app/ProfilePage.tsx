import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { profileApi } from '../../api/profile'
import type { UserProfile } from '../../api/types'
import { AppButton, ErrorState, LoadingState, PageHeader } from '../../components/app/AppUI'
import { useRemoteData } from '../../hooks/useRemoteData'

type EditableProfile = Pick<UserProfile, 'bio' | 'avatarUrl' | 'githubUrl' | 'linkedinUrl' | 'phone'>
const empty: EditableProfile = { bio: '', avatarUrl: '', githubUrl: '', linkedinUrl: '', phone: '' }

export default function ProfilePage() {
  const load = useCallback((signal: AbortSignal) => profileApi.get(signal), [])
  const { data, loading, error, reload } = useRemoteData('my-profile', load)
  const [form, setForm] = useState<EditableProfile>(empty)
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  // eslint-disable-next-line react-hooks/set-state-in-effect -- hydrate editable fields after the profile request resolves
  useEffect(() => { if (data) setForm({ bio: data.bio || '', avatarUrl: data.avatarUrl || '', githubUrl: data.githubUrl || '', linkedinUrl: data.linkedinUrl || '', phone: data.phone || '' }) }, [data])

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true); setNotice(null); setFormError(null)
    try {
      const updated = await profileApi.update(form)
      setForm({ bio: updated.bio || '', avatarUrl: updated.avatarUrl || '', githubUrl: updated.githubUrl || '', linkedinUrl: updated.linkedinUrl || '', phone: updated.phone || '' })
      setNotice('Your profile was updated.')
      reload()
    } catch (cause) { setFormError(cause instanceof Error ? cause.message : 'Profile update failed.') }
    finally { setSaving(false) }
  }

  const update = (field: keyof EditableProfile, value: string) => setForm((current) => ({ ...current, [field]: value }))
  return <div className="app-page">
    <PageHeader eyebrow="MEMBERS CORNER / ACCOUNT" title="My profile" description="Review your account information and update the profile details available to you." />
    {loading && <LoadingState label="Loading profile" />}{error && <ErrorState error={error} onRetry={reload} />}
    {data && <div className="app-two-col profile-layout">
      <section className="app-card"><h2>Account information</h2><dl className="app-kv"><dt>Name</dt><dd>{data.name}</dd><dt>Email</dt><dd>{data.email}</dd><dt>Program</dt><dd>{data.program}</dd><dt>Roles</dt><dd>{data.roles.join(', ')}</dd><dt>User ID</dt><dd>{data.userId}</dd></dl></section>
      <section className="app-card"><h2>Profile details</h2>{notice && <p className="app-notice success" role="status">{notice}</p>}{formError && <p className="app-notice error" role="alert">{formError}</p>}
        <form className="app-form profile-form" onSubmit={(event) => void save(event)}>
          <div className="app-field span-2"><label htmlFor="profile-bio">Bio</label><textarea id="profile-bio" maxLength={500} value={form.bio || ''} onChange={(event) => update('bio', event.target.value)} /><small>{(form.bio || '').length}/500</small></div>
          <div className="app-field span-2"><label htmlFor="profile-avatar">Avatar URL</label><input id="profile-avatar" type="url" maxLength={500} value={form.avatarUrl || ''} onChange={(event) => update('avatarUrl', event.target.value)} /></div>
          <div className="app-field"><label htmlFor="profile-github">GitHub profile URL</label><input id="profile-github" type="url" maxLength={255} value={form.githubUrl || ''} onChange={(event) => update('githubUrl', event.target.value)} /></div>
          <div className="app-field"><label htmlFor="profile-linkedin">LinkedIn profile URL</label><input id="profile-linkedin" type="url" maxLength={255} value={form.linkedinUrl || ''} onChange={(event) => update('linkedinUrl', event.target.value)} /></div>
          <div className="app-field"><label htmlFor="profile-phone">Phone</label><input id="profile-phone" type="tel" maxLength={20} pattern="\+?[0-9\s-]{7,20}" value={form.phone || ''} onChange={(event) => update('phone', event.target.value)} /></div>
          <div className="app-form-actions"><AppButton type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</AppButton></div>
        </form>
      </section>
    </div>}
  </div>
}
