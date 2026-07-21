import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { fetchMe } from '../api/client'
import { clearTokens } from '../auth/tokenStorage'

export default function ProfilePage() {
  const navigate = useNavigate()
  const { data, error, isLoading } = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })

  function logout() {
    clearTokens()
    navigate('/', { replace: true })
  }

  return (
    <main className="panel">
      <h2>Your profile</h2>
      <p>Signed-in account from Google (or Dev login).</p>
      {isLoading && <p>Loading…</p>}
      {error && <p className="error">{(error as Error).message}</p>}
      {data && (
        <dl className="meta">
          <dt>Name</dt>
          <dd>{data.displayName}</dd>
          <dt>Email</dt>
          <dd>{data.email}</dd>
          <dt>Role</dt>
          <dd>{data.role}</dd>
          <dt>Objective</dt>
          <dd>{data.objective}</dd>
        </dl>
      )}
      <div className="cta-row" style={{ justifyContent: 'flex-start', marginTop: '1.5rem' }}>
        <button className="btn btn-secondary" type="button" onClick={logout}>
          Sign out
        </button>
      </div>
    </main>
  )
}
