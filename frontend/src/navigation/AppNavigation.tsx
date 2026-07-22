import { Link, NavLink } from 'react-router-dom'
import { IconBars, IconBook, IconHome, IconPlus, IconUser } from '../ui/Icons'

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
          <NavLink to="/today" className="tab-link">
            <IconHome className="tab-icon" />
            <span>Dashboard</span>
          </NavLink>
          <NavLink to="/diary" className="tab-link">
            <IconBook className="tab-icon" />
            <span>Diary</span>
          </NavLink>
          <Link to="/lookup" className="bottom-nav-fab" aria-label="Track food">
            <IconPlus className="bottom-nav-fab-icon" />
          </Link>
          <NavLink to="/analytics" className="tab-link">
            <IconBars className="tab-icon" />
            <span>Analytics</span>
          </NavLink>
          <NavLink to="/me" className="tab-link">
            <IconUser className="tab-icon" />
            <span>Profile</span>
          </NavLink>
        </nav>
      )}
    </>
  )
}
