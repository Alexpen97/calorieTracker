import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import * as client from '../api/client'

describe('DashboardPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders today summary, macros, vitamins, minerals, and weight cards', async () => {
    vi.spyOn(client, 'fetchMe').mockResolvedValue({
      id: 'u1',
      email: 'alex@example.com',
      displayName: 'Alex',
      avatarUrl: null,
      role: 'USER',
      sex: null,
      birthDate: null,
      heightCm: null,
      activityLevel: null,
      objective: 'MAINTAIN',
    })
    vi.spyOn(client, 'fetchDiarySummary').mockResolvedValue({
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 1450, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 82, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 180, unit: 'g', target: 250 },
        { code: 'fat', amount: 48, unit: 'g', target: 70 },
        { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
      ],
      water: { amountMl: 1200, targetMl: 2500 },
    })
    vi.spyOn(client, 'fetchDiaryEntries').mockResolvedValue([])
    vi.spyOn(client, 'fetchWater').mockResolvedValue([])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.4, measuredAt: '2026-07-20T08:00:00Z' },
      { id: 'w2', weightKg: 72.1, measuredAt: '2026-07-22T08:00:00Z' },
    ])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Macros' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Today Summary' })).toBeInTheDocument()
    expect(screen.getByLabelText('Calories: 69%')).toBeInTheDocument()
    expect(screen.getByLabelText('Protein: 82%')).toBeInTheDocument()
    expect(screen.getByLabelText('Carbs: 72%')).toBeInTheDocument()
    expect(screen.getByLabelText('Fat: 69%')).toBeInTheDocument()
    expect(screen.getAllByTestId('nested-macro-bar-track')).toHaveLength(3)
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </MemoryRouter>,
  )
}
