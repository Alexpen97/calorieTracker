import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import * as client from '../api/client'
import { formatLocalDate } from '../diary/formatDay'

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date(2026, 6, 28, 12, 0, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('renders calorie left hero, macro cards, week strip, and weight insight', async () => {
    const today = formatLocalDate()
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
    vi.spyOn(client, 'fetchDiarySummary').mockImplementation(async (date) => ({
      date,
      totals: [
        { code: 'energy_kcal', amount: 1450, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 82, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 180, unit: 'g', target: 250 },
        { code: 'fat', amount: 48, unit: 'g', target: 70 },
        { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
      ],
      water: { amountMl: 1200, targetMl: 2500 },
    }))
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.4, measuredAt: '2026-07-20T08:00:00Z' },
      { id: 'w2', weightKg: 72.1, measuredAt: '2026-07-22T08:00:00Z' },
    ])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument()
    expect(screen.getByLabelText(/^Calories left: 650$|^Calories left: 650$/)).toBeInTheDocument()
    expect(screen.getByText('Calories left')).toBeInTheDocument()
    expect(screen.getByLabelText('This week')).toBeInTheDocument()
    expect(screen.getByLabelText('Macros left')).toBeInTheDocument()
    expect(screen.getByText('Protein left')).toBeInTheDocument()
    expect(screen.getByText('18g')).toBeInTheDocument()
    expect(screen.getByText('Carbs left')).toBeInTheDocument()
    expect(screen.getByText('70g')).toBeInTheDocument()
    expect(screen.getByText('Fat left')).toBeInTheDocument()
    expect(screen.getByText('22g')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Weight' })).toBeInTheDocument()
    expect(screen.getByText('Last 30 days')).toBeInTheDocument()
    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
    expect(screen.getAllByTestId('weight-trend-point')).toHaveLength(2)
    expect(client.fetchDiarySummary).toHaveBeenCalledWith(today)
  })

  it('loads another day when a date in the week strip is clicked', async () => {
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
    const fetchSummary = vi.spyOn(client, 'fetchDiarySummary').mockImplementation(async (date) => ({
      date,
      totals: [
        {
          code: 'energy_kcal',
          amount: date === formatLocalDate() ? 1450 : 900,
          unit: 'kcal',
          target: 2100,
        },
        { code: 'protein', amount: 40, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 100, unit: 'g', target: 250 },
        { code: 'fat', amount: 30, unit: 'g', target: 70 },
      ],
      water: { amountMl: 0, targetMl: 2500 },
    }))
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument()
    const otherDay = screen
      .getAllByRole('button')
      .find((button) => button.getAttribute('aria-pressed') !== 'true')
    expect(otherDay).toBeTruthy()
    fireEvent.click(otherDay!)

    await waitFor(() => {
      expect(fetchSummary.mock.calls.length).toBeGreaterThan(1)
    })
    expect(await screen.findByLabelText(/Calories left: 1[,.]?200/)).toBeInTheDocument()
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
    vi.spyOn(client, 'fetchDiarySummary').mockImplementation(async (date) => ({
      date,
      totals: [
        { code: 'energy_kcal', amount: 2333, unit: 'kcal', target: null },
        { code: 'protein', amount: 233, unit: 'g', target: null },
        { code: 'carbohydrates', amount: 23, unit: 'g', target: null },
        { code: 'fat', amount: 223, unit: 'g', target: null },
      ],
      water: { amountMl: 0, targetMl: null },
    }))
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

    expect(await screen.findByLabelText(/Calories left: 0/)).toBeInTheDocument()
    expect(screen.getAllByText('0g').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByText('Protein left')).toBeInTheDocument()
    expect(screen.getByText('Carbs left')).toBeInTheDocument()
    expect(screen.getByText(/^227g$/)).toBeInTheDocument()
    expect(screen.getByText('Fat left')).toBeInTheDocument()
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
