import { Navigate, Route, Routes } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { isLoggedIn } from './auth/tokenStorage'
import { fetchMe, fetchWeightHistory } from './api/client'
import { needsOnboarding } from './onboarding/needsOnboarding'
import LoginPage from './pages/LoginPage'
import SettingsHomePage from './pages/SettingsHomePage'
import SettingsProfileSection from './pages/settings/SettingsProfileSection'
import SettingsGoalsSection from './pages/settings/SettingsGoalsSection'
import SettingsWeightSection from './pages/settings/SettingsWeightSection'
import SettingsAccountSection from './pages/settings/SettingsAccountSection'
import SettingsIntegrationsSection from './pages/settings/SettingsIntegrationsSection'
import AuthCallbackPage from './pages/AuthCallbackPage'
import LookupPage from './pages/LookupPage'
import ProductPage from './pages/ProductPage'
import DiaryPage from './pages/DiaryPage'
import DashboardPage from './pages/DashboardPage'
import AnalyticsPage from './pages/AnalyticsPage'
import SubmitProductPage from './pages/SubmitProductPage'
import ModerationPage from './pages/ModerationPage'
import OnboardingPage from './pages/OnboardingPage'
import AppNavigation from './navigation/AppNavigation'
import PreviewIndexPage from './pages/preview/PreviewIndexPage'
import PreviewDashboardPage from './pages/preview/PreviewDashboardPage'
import PreviewDiaryPage from './pages/preview/PreviewDiaryPage'
import PreviewAnalyticsPage from './pages/preview/PreviewAnalyticsPage'
import PreviewLookupPage from './pages/preview/PreviewLookupPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  if (!isLoggedIn()) {
    return <Navigate to="/" replace />
  }
  return children
}

function RequireOnboardingComplete({ children }: { children: React.ReactNode }) {
  const me = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })
  const weights = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  if (me.isLoading || weights.isLoading) {
    return (
      <main className="mobile-page">
        <p>Loading…</p>
      </main>
    )
  }

  if (me.error || weights.error) {
    return (
      <main className="mobile-page">
        <p className="error">
          {((me.error ?? weights.error) as Error).message}
        </p>
      </main>
    )
  }

  if (me.data && weights.data && needsOnboarding(me.data, weights.data)) {
    return <Navigate to="/onboarding" replace />
  }

  return children
}

function RequireNeedsOnboarding({ children }: { children: React.ReactNode }) {
  const me = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })
  const weights = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  if (me.isLoading || weights.isLoading) {
    return (
      <main className="mobile-page">
        <p>Loading…</p>
      </main>
    )
  }

  if (me.data && weights.data && !needsOnboarding(me.data, weights.data)) {
    return <Navigate to="/today" replace />
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
  const showPreview = import.meta.env.DEV
  return (
    <div className="app-shell">
      <AppNavigation loggedIn={loggedIn} canModerate={canModerate} />
      <Routes>
        {showPreview && (
          <>
            <Route path="/preview" element={<PreviewIndexPage />} />
            <Route path="/preview/dashboard" element={<PreviewDashboardPage />} />
            <Route path="/preview/diary" element={<PreviewDiaryPage />} />
            <Route path="/preview/analytics" element={<PreviewAnalyticsPage />} />
            <Route path="/preview/lookup" element={<PreviewLookupPage />} />
          </>
        )}
        <Route path="/" element={loggedIn ? <Navigate to="/today" replace /> : <LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route
          path="/onboarding"
          element={
            <RequireAuth>
              <RequireNeedsOnboarding>
                <OnboardingPage />
              </RequireNeedsOnboarding>
            </RequireAuth>
          }
        />
        <Route
          path="/today"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <DashboardPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/diary"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <DiaryPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/analytics"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <AnalyticsPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/lookup"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <LookupPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/submit-product"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SubmitProductPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/moderation"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <ModerationPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/products/:id"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <ProductPage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsHomePage />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings/profile"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsProfileSection />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings/goals"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsGoalsSection />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings/weight"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsWeightSection />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings/account"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsAccountSection />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route
          path="/settings/integrations"
          element={
            <RequireAuth>
              <RequireOnboardingComplete>
                <SettingsIntegrationsSection />
              </RequireOnboardingComplete>
            </RequireAuth>
          }
        />
        <Route path="/me" element={<Navigate to="/settings" replace />} />
      </Routes>
    </div>
  )
}
