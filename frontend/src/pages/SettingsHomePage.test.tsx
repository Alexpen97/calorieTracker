import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import SettingsHomePage from './SettingsHomePage'
import SettingsProfileSection from './settings/SettingsProfileSection'
import SettingsGoalsSection from './settings/SettingsGoalsSection'
import SettingsWeightSection from './settings/SettingsWeightSection'
import SettingsAccountSection from './settings/SettingsAccountSection'
import * as client from '../api/client'
import * as tokenStorage from '../auth/tokenStorage'

const profile: client.UserProfile = {
  id: 'u1',
  email: 'alex@example.com',
  displayName: 'Alex',
  avatarUrl: null,
  role: 'USER',
  sex: 'MALE',
  birthDate: '1996-07-21',
  heightCm: 180,
  activityLevel: 'MODERATE',
  objective: 'MUSCLE_GAIN',
}

const goals: client.Goal[] = [
  {
    nutrientCode: 'energy_kcal',
    dailyTarget: 2200,
    unit: 'kcal',
    origin: 'COMPUTED',
    computedAt: '2026-07-22T10:00:00Z',
  },
]

const weights: client.WeightLog[] = [
  { id: 'w1', weightKg: 80.5, measuredAt: '2026-07-22T10:00:00Z' },
]

describe('SettingsHomePage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders a settings hub with focused entry points and summaries', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue(profile)
    vi.spyOn(client, 'fetchGoals').mockResolvedValue(goals)
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue(weights)

    renderSettings('/settings')

    expect(await screen.findByRole('heading', { name: 'Settings' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Profile/i })).toHaveAttribute('href', '/settings/profile')
    expect(screen.getByRole('link', { name: /Goals/i })).toHaveAttribute('href', '/settings/goals')
    expect(screen.getByRole('link', { name: /Weight/i })).toHaveAttribute('href', '/settings/weight')
    expect(screen.getByRole('link', { name: /Account/i })).toHaveAttribute('href', '/settings/account')
    expect(screen.getByRole('button', { name: /Sign out/i })).toBeInTheDocument()

    expect(await screen.findByText(/Alex/)).toBeInTheDocument()
    expect(screen.getByText(/Lean muscle/i)).toBeInTheDocument()
    expect(screen.getByText(/computed/i)).toBeInTheDocument()
    expect(screen.getByText(/80\.5 kg/i)).toBeInTheDocument()
    expect(screen.getByText(/alex@example.com/i)).toBeInTheDocument()
  })

  it('opens the profile section and saves profile details', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue(profile)
    vi.spyOn(client, 'fetchGoals').mockResolvedValue(goals)
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue(weights)
    const updateMe = vi.spyOn(client, 'updateMe').mockResolvedValue({
      ...profile,
      displayName: 'Alex Updated',
    })

    renderSettings('/settings/profile')

    expect(await screen.findByRole('heading', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Back to settings/i })).toHaveAttribute(
      'href',
      '/settings',
    )

    const nameInput = await screen.findByLabelText(/Display name/i)
    await waitFor(() => {
      expect(nameInput).toHaveValue('Alex')
    })
    fireEvent.change(nameInput, {
      target: { value: 'Alex Updated' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Save profile/i }))

    await waitFor(() => {
      expect(updateMe.mock.calls[0]?.[0]).toEqual(
        expect.objectContaining({ displayName: 'Alex Updated' }),
      )
    })
    expect(await screen.findByText(/Profile saved/i)).toBeInTheDocument()
  })

  it('logs weight from the weight section', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue(profile)
    vi.spyOn(client, 'fetchGoals').mockResolvedValue(goals)
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue(weights)
    const logWeight = vi.spyOn(client, 'logWeight').mockResolvedValue({
      id: 'w2',
      weightKg: 81,
      measuredAt: '2026-07-25T10:00:00Z',
    })

    renderSettings('/settings/weight')

    expect(await screen.findByRole('heading', { name: 'Weight' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText(/Log weight/i), { target: { value: '81' } })
    fireEvent.click(screen.getByRole('button', { name: /^Log$/i }))

    await waitFor(() => {
      expect(logWeight.mock.calls[0]?.[0]).toEqual({ weightKg: 81 })
    })
  })

  it('saves and recalculates goals from the goals section', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue(profile)
    vi.spyOn(client, 'fetchGoals').mockResolvedValue(goals)
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue(weights)
    const overrideGoals = vi.spyOn(client, 'overrideGoals').mockResolvedValue([
      { ...goals[0], dailyTarget: 2300, origin: 'USER_OVERRIDE' },
    ])
    const recalculateGoals = vi.spyOn(client, 'recalculateGoals').mockResolvedValue({
      needsProfile: false,
      current: [{ ...goals[0], dailyTarget: 2150, origin: 'COMPUTED' }],
      suggested: [{ ...goals[0], dailyTarget: 2150, origin: 'COMPUTED' }],
    })

    renderSettings('/settings/goals')

    expect(await screen.findByRole('heading', { name: 'Daily goals' })).toBeInTheDocument()
    const goalInput = await screen.findByLabelText(/energy_kcal target/i)
    fireEvent.change(goalInput, {
      target: { value: '2300' },
    })
    fireEvent.click(screen.getByRole('button', { name: /^Save$/i }))

    await waitFor(() => {
      expect(overrideGoals.mock.calls[0]?.[0]).toEqual({
        goals: [{ nutrientCode: 'energy_kcal', dailyTarget: 2300, unit: 'kcal' }],
      })
    })

    fireEvent.click(screen.getByRole('button', { name: /Recalculate/i }))
    await waitFor(() => {
      expect(recalculateGoals).toHaveBeenCalledWith(true)
    })
    expect(await screen.findByText(/Goals recalculated/i)).toBeInTheDocument()
  })

  it('signs out from the account section', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue(profile)
    vi.spyOn(client, 'fetchGoals').mockResolvedValue(goals)
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue(weights)
    const clearTokens = vi.spyOn(tokenStorage, 'clearTokens').mockResolvedValue()

    renderSettings('/settings/account')

    expect(await screen.findByRole('heading', { name: 'Account' })).toBeInTheDocument()
    expect(await screen.findByText(/alex@example.com/i)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Sign out/i }))

    await waitFor(() => {
      expect(clearTokens).toHaveBeenCalled()
    })
    expect(await screen.findByText('Signed out')).toBeInTheDocument()
  })
})

function renderSettings(path: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter initialEntries={[path]}>
      <QueryClientProvider client={qc}>
        <Routes>
          <Route path="/settings" element={<SettingsHomePage />} />
          <Route path="/settings/profile" element={<SettingsProfileSection />} />
          <Route path="/settings/goals" element={<SettingsGoalsSection />} />
          <Route path="/settings/weight" element={<SettingsWeightSection />} />
          <Route path="/settings/account" element={<SettingsAccountSection />} />
          <Route path="/" element={<div>Signed out</div>} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}
