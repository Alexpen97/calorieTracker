import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchGoals, fetchMe, fetchWeightHistory } from '../api/client'
import { clearTokens } from '../auth/tokenStorage'
import { objectiveLabel } from './settings/SettingsSectionShell'

export default function SettingsHomePage() {
  const navigate = useNavigate()
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })
  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
  })

  function logout() {
    void clearTokens().then(() => {
      navigate('/', { replace: true })
    })
  }

  const latestWeight = weightQuery.data?.[0]
  const goals = goalsQuery.data ?? []
  const goalsSummary =
    goals.length === 0
      ? 'No goals yet'
      : goals.some((goal) => goal.origin === 'USER_OVERRIDE')
        ? 'Includes custom targets'
        : 'Using computed targets'

  return (
    <main className="mobile-page settings-page">
      <div className="diary-header">
        <div>
          <p className="sheet-kicker">Account</p>
          <h2>Settings</h2>
          <p className="product-meta">Manage profile, goals, weight, and account actions.</p>
        </div>
      </div>

      {(meQuery.isLoading || weightQuery.isLoading || goalsQuery.isLoading) && <p>Loading…</p>}
      {[meQuery.error, weightQuery.error, goalsQuery.error].filter(Boolean).map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      <nav className="settings-hub" aria-label="Settings sections">
        <Link className="settings-row" to="/settings/profile">
          <span className="settings-row-copy">
            <span className="settings-row-title">Profile</span>
            <span className="settings-row-summary">
              {meQuery.data
                ? `${meQuery.data.displayName} · ${objectiveLabel(meQuery.data.objective)}`
                : 'Personal details'}
            </span>
          </span>
          <span className="settings-row-chevron" aria-hidden="true">
            ›
          </span>
        </Link>

        <Link className="settings-row" to="/settings/goals">
          <span className="settings-row-copy">
            <span className="settings-row-title">Goals</span>
            <span className="settings-row-summary">{goalsSummary}</span>
          </span>
          <span className="settings-row-chevron" aria-hidden="true">
            ›
          </span>
        </Link>

        <Link className="settings-row" to="/settings/weight">
          <span className="settings-row-copy">
            <span className="settings-row-title">Weight</span>
            <span className="settings-row-summary">
              {latestWeight
                ? `${formatNumber(latestWeight.weightKg)} kg`
                : 'No weight entries yet'}
            </span>
          </span>
          <span className="settings-row-chevron" aria-hidden="true">
            ›
          </span>
        </Link>

        <Link className="settings-row" to="/settings/account">
          <span className="settings-row-copy">
            <span className="settings-row-title">Account</span>
            <span className="settings-row-summary">
              {meQuery.data
                ? `${meQuery.data.email} · ${meQuery.data.role}`
                : 'Email and sign out'}
            </span>
          </span>
          <span className="settings-row-chevron" aria-hidden="true">
            ›
          </span>
        </Link>
      </nav>

      <div className="settings-signout-block">
        <button className="btn btn-secondary settings-signout-button" type="button" onClick={logout}>
          Sign out
        </button>
      </div>
    </main>
  )
}

function formatNumber(value: number) {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}
