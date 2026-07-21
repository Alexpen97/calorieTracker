import { Navigate, Route, Routes } from 'react-router-dom'
import { isLoggedIn } from './auth/tokenStorage'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import AuthCallbackPage from './pages/AuthCallbackPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  if (!isLoggedIn()) {
    return <Navigate to="/" replace />
  }
  return children
}

export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">NutriTrack</div>
      </header>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
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
