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
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.4, measuredAt: '2026-07-20T08:00:00Z' },
      { id: 'w2', weightKg: 72.1, measuredAt: '2026-07-22T08:00:00Z' },
    ])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Macros' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Today Summary' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^Calories: 1.450 \/ 2.100$|^Calories: 1,450 \/ 2,100$/)).toBeInTheDocument()
    expect(screen.getByText(/^1.450 \/ 2.100$|^1,450 \/ 2,100$/)).toBeInTheDocument()
    expect(screen.getByLabelText('Protein: 82 / 100 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Carbs: 180 / 250 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Fat: 48 / 70 g')).toBeInTheDocument()
    expect(screen.queryByTestId('nested-macro-track')).not.toBeInTheDocument()
    expect(screen.getAllByTestId('nested-macro-bar-track')).toHaveLength(3)
    expect(screen.queryByRole('link', { name: 'View Diary' })).not.toBeInTheDocument()
    expect(screen.queryByText('Meals')).not.toBeInTheDocument()
    expect(screen.queryByText('Goal')).not.toBeInTheDocument()
    expect(screen.queryByText('Consumed')).not.toBeInTheDocument()
    expect(screen.queryByText(/water/i)).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
  })

  it('fills calorie and macro goals from fetchGoals when summary targets are null', async () => {
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
        { code: 'energy_kcal', amount: 2333, unit: 'kcal', target: null },
        { code: 'protein', amount: 233, unit: 'g', target: null },
        { code: 'carbohydrates', amount: 23, unit: 'g', target: null },
        { code: 'fat', amount: 223, unit: 'g', target: null },
      ],
      water: { amountMl: 0, targetMl: null },
    })
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([
      {
        nutrientCode: 'energy_kcal',
        dailyTarget: 2100,
        unit: 'kcal',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
      {
        nutrientCode: 'protein',
        dailyTarget: 100,
        unit: 'g',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
      {
        nutrientCode: 'carbohydrates',
        dailyTarget: 250,
        unit: 'g',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
      {
        nutrientCode: 'fat',
        dailyTarget: 70,
        unit: 'g',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
    ])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByLabelText(/^Calories: 2.333 \/ 2.100$|^Calories: 2,333 \/ 2,100$/)).toBeInTheDocument()
    expect(screen.getByLabelText('Protein: 233 / 100 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Carbs: 23 / 250 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Fat: 223 / 70 g')).toBeInTheDocument()
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
