import { Navigate, Route, Routes, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { isLoggedIn } from './auth/tokenStorage'
import { fetchMe } from './api/client'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import AuthCallbackPage from './pages/AuthCallbackPage'
import LookupPage from './pages/LookupPage'
import ProductPage from './pages/ProductPage'
import DiaryPage from './pages/DiaryPage'
import SubmitProductPage from './pages/SubmitProductPage'
import ModerationPage from './pages/ModerationPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  if (!isLoggedIn()) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  const loggedIn = isLoggedIn()
  const me = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
    enabled: loggedIn,
  })
  const canModerate = me.data?.role === 'MODERATOR' || me.data?.role === 'ADMIN'
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
            {canModerate && <Link to="/moderation">Moderation</Link>}
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
          path="/submit-product"
          element={
            <RequireAuth>
              <SubmitProductPage />
            </RequireAuth>
          }
        />
        <Route
          path="/moderation"
          element={
            <RequireAuth>
              <ModerationPage />
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
