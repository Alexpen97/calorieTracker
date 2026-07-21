import { Navigate, Route, Routes, Link } from 'react-router-dom'
import { isLoggedIn } from './auth/tokenStorage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import AuthCallbackPage from './pages/AuthCallbackPage'
import LookupPage from './pages/LookupPage'
import ProductPage from './pages/ProductPage'
import DiaryPage from './pages/DiaryPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  if (!isLoggedIn()) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  const loggedIn = isLoggedIn()
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to={loggedIn ? '/today' : '/'} className="brand">
          NutriTrack
        </Link>
        {loggedIn && (
          <nav className="topnav">
            <Link to="/today">Today</Link>
            <Link to="/lookup">Lookup</Link>
            <Link to="/me">Profile</Link>
          </nav>
        )}
      </header>
      <Routes>
        <Route path="/" element={loggedIn ? <Navigate to="/today" replace /> : <LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route
          path="/today"
          element={
            <RequireAuth>
              <DiaryPage />
            </RequireAuth>
          }
        />
        <Route
          path="/lookup"
          element={
            <RequireAuth>
              <LookupPage />
            </RequireAuth>
          }
        />
        <Route
          path="/products/:id"
          element={
            <RequireAuth>
              <ProductPage />
            </RequireAuth>
          }
        />
        <Route
          path="/me"
          element={
            <RequireAuth>
              <ProfilePage />
            </RequireAuth>
          }
        />
      </Routes>
    </div>
  )
}
