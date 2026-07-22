import { Link, NavLink } from 'react-router-dom'

export default function AppNavigation({
  loggedIn,
  canModerate,
}: {
  loggedIn: boolean
  canModerate: boolean
}) {
  return (
    <>
      <header className="topbar app-topbar">
        <Link to={loggedIn ? '/today' : '/'} className="brand">
          NutriTrack
        </Link>
        {loggedIn && (
          <nav className="topnav desktop-nav" aria-label="Primary">
            <NavLink to="/today">Dashboard</NavLink>
            <NavLink to="/diary">Diary</NavLink>
            <NavLink to="/analytics">Analytics</NavLink>
            <NavLink to="/lookup">Lookup</NavLink>
            {canModerate && <NavLink to="/moderation">Moderation</NavLink>}
            <NavLink to="/me">Profile</NavLink>
          </nav>
        )}
      </header>
      {loggedIn && (
        <nav className="bottom-nav" aria-label="Primary mobile">
          <NavLink to="/today">Dashboard</NavLink>
          <NavLink to="/diary">Diary</NavLink>
          <NavLink to="/analytics">Analytics</NavLink>
          <NavLink to="/me">Profile</NavLink>
        </nav>
      )}
    </>
  )
}
