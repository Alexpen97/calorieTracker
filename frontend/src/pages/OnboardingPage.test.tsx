import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import OnboardingPage from './OnboardingPage'
import * as client from '../api/client'

describe('OnboardingPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('walks through body stats and diet goal then submits calculated nutrient goals', async () => {
    const completeOnboarding = vi.spyOn(client, 'completeOnboarding').mockResolvedValue({
      needsProfile: false,
      profile: {
        id: 'u1',
        email: 'alex@example.com',
        displayName: 'Alex',
        avatarUrl: null,
        role: 'USER',
        sex: 'MALE',
        birthDate: '1996-07-21',
        heightCm: 180,
        activityLevel: 'MODERATE',
        objective: 'LOSE',
      },
      weight: { id: 'w1', weightKg: 80, measuredAt: '2026-07-22T10:00:00Z' },
      goals: [
        {
          nutrientCode: 'energy_kcal',
          dailyTarget: 2200,
          unit: 'kcal',
          origin: 'COMPUTED',
          computedAt: '2026-07-22T10:00:00Z',
        },
        {
          nutrientCode: 'protein',
          dailyTarget: 128,
          unit: 'g',
          origin: 'COMPUTED',
          computedAt: '2026-07-22T10:00:00Z',
        },
        {
          nutrientCode: 'water_ml',
          dailyTarget: 2800,
          unit: 'ml',
          origin: 'COMPUTED',
          computedAt: '2026-07-22T10:00:00Z',
        },
      ],
    })

    renderOnboarding()

    expect(screen.getByRole('heading', { name: 'Set up NutriTrack' })).toBeInTheDocument()
    expect(screen.getByText(/weight, height, and diet goal/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/height/i), { target: { value: '180' } })
    fireEvent.change(screen.getByLabelText(/weight/i), { target: { value: '80' } })
    fireEvent.click(screen.getByRole('button', { name: /continue/i }))

    expect(screen.getByRole('heading', { name: 'Your diet goal' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText(/dieting goal/i), { target: { value: 'LOSE' } })
    fireEvent.change(screen.getByLabelText(/activity level/i), { target: { value: 'MODERATE' } })
    fireEvent.change(screen.getByLabelText(/^sex$/i), { target: { value: 'MALE' } })
    fireEvent.change(screen.getByLabelText(/birth date/i), { target: { value: '1996-07-21' } })
    fireEvent.click(screen.getByRole('button', { name: /calculate goals/i }))

    await waitFor(() => {
      expect(completeOnboarding.mock.calls[0]?.[0]).toEqual({
        heightCm: 180,
        weightKg: 80,
        objective: 'LOSE',
        activityLevel: 'MODERATE',
        sex: 'MALE',
        birthDate: '1996-07-21',
      })
    })

    expect(await screen.findByRole('heading', { name: 'Your nutrient goals' })).toBeInTheDocument()
    expect(screen.getByText(/Energy kcal/i)).toBeInTheDocument()
    expect(screen.getByText(/2,200 kcal/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /go to dashboard/i })).toHaveAttribute('href', '/today')
  })
})

function renderOnboarding() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter initialEntries={['/onboarding']}>
      <QueryClientProvider client={qc}>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/today" element={<div>Dashboard</div>} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>,
  )
}
