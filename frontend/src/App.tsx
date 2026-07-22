import { Navigate, Route, Routes } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { isLoggedIn } from './auth/tokenStorage'
import { fetchMe } from './api/client'
import LoginPage from './pages/LoginPage'
import ProfilePage from './pages/ProfilePage'
import AuthCallbackPage from './pages/AuthCallbackPage'
import LookupPage from './pages/LookupPage'
import ProductPage from './pages/ProductPage'
import DiaryPage from './pages/DiaryPage'
import DashboardPage from './pages/DashboardPage'
import AnalyticsPage from './pages/AnalyticsPage'
import SubmitProductPage from './pages/SubmitProductPage'
import ModerationPage from './pages/ModerationPage'
import AppNavigation from './navigation/AppNavigation'

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
      <AppNavigation loggedIn={loggedIn} canModerate={canModerate} />
      <Routes>
        <Route path="/" element={loggedIn ? <Navigate to="/today" replace /> : <LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route
          path="/today"
          element={
            <RequireAuth>
              <DashboardPage />
            </RequireAuth>
          }
        />
        <Route
          path="/diary"
          element={
            <RequireAuth>
              <DiaryPage />
            </RequireAuth>
          }
        />
        <Route
          path="/analytics"
          element={
            <RequireAuth>
              <AnalyticsPage />
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
