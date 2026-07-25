import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../../api/client'
import { clearTokens } from '../../auth/tokenStorage'
import { SettingsSectionShell } from './SettingsSectionShell'

export default function SettingsAccountSection() {
  const navigate = useNavigate()
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })

  function logout() {
    void clearTokens().then(() => {
      navigate('/', { replace: true })
    })
  }

  return (
    <SettingsSectionShell title="Account" description="Review account details and sign out.">
      {meQuery.isLoading && <p>Loading…</p>}
      {meQuery.error && <p className="error">{(meQuery.error as Error).message}</p>}

      <section className="dashboard-card">
        {meQuery.data && (
          <dl className="meta compact-meta">
            <dt>Email</dt>
            <dd>{meQuery.data.email}</dd>
            <dt>Role</dt>
            <dd>{meQuery.data.role}</dd>
          </dl>
        )}
        <div className="cta-row settings-signout-row">
          <button className="btn btn-secondary" type="button" onClick={logout}>
            Sign out
          </button>
        </div>
      </section>
    </SettingsSectionShell>
  )
}
