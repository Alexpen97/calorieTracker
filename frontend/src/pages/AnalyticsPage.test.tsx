import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AnalyticsPage from './AnalyticsPage'
import * as client from '../api/client'
import {
  analyticsRangeFromEnd,
  formatAnalyticsRangeLabel,
  formatLocalDate,
  shiftAnalyticsRangeEnd,
} from '../diary/formatDay'

describe('AnalyticsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders weight, slim macros, vitamin, mineral, and insight cards with a switchable 30-day range', async () => {
    const today = formatLocalDate()
    const current = analyticsRangeFromEnd(today)
    const previousEnd = shiftAnalyticsRangeEnd(today, -1, today)
    const previous = analyticsRangeFromEnd(previousEnd)

    const rangeSpy = vi.spyOn(client, 'fetchDiarySummaryRange').mockImplementation(async (from, to) => [
      {
        date: from,
        totals: [
          { code: 'protein', amount: 80, unit: 'g', target: 100 },
          { code: 'carbohydrates', amount: 200, unit: 'g', target: 250 },
          { code: 'fat', amount: 50, unit: 'g', target: 70 },
          { code: 'vitamin_d', amount: 3, unit: 'ug', target: 15 },
          { code: 'calcium', amount: 400, unit: 'mg', target: 1000 },
        ],
        water: { amountMl: 1800, targetMl: 2500 },
      },
      {
        date: to,
        totals: [
          { code: 'protein', amount: 92, unit: 'g', target: 100 },
          { code: 'carbohydrates', amount: 210, unit: 'g', target: 250 },
          { code: 'fat', amount: 60, unit: 'g', target: 70 },
          { code: 'vitamin_d', amount: 9, unit: 'ug', target: 15 },
          { code: 'calcium', amount: 800, unit: 'mg', target: 1000 },
        ],
        water: { amountMl: 1800, targetMl: 2500 },
      },
    ])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([
      {
        nutrientCode: 'vitamin_c',
        dailyTarget: 80,
        unit: 'mg',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
      {
        nutrientCode: 'iron',
        dailyTarget: 14,
        unit: 'mg',
        origin: 'COMPUTED',
        computedAt: '2026-07-21T00:00:00Z',
      },
    ])
    const weightSpy = vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.3, measuredAt: `${current.from}T08:00:00Z` },
      { id: 'w2', weightKg: 71.9, measuredAt: `${current.to}T08:00:00Z` },
    ])

    renderWithClient(<AnalyticsPage />)

    expect(await screen.findByLabelText('Vitamin trends, last 30 days')).toBeInTheDocument()
    expect(screen.getByLabelText('Mineral trends, last 30 days')).toBeInTheDocument()
    expect(screen.getByTestId('shared-micro-line-vitamin_d')).toBeInTheDocument()
    expect(screen.getByTestId('shared-micro-line-calcium')).toBeInTheDocument()
    expect(screen.getByLabelText('Vitamin trends, last 30 days legend')).toBeInTheDocument()
    expect(rangeSpy).toHaveBeenCalledWith(current.from, current.to)
    expect(weightSpy).toHaveBeenCalledWith({
      from: current.from,
      to: current.to,
    })
    expect(screen.getByRole('heading', { name: 'Weight trend' })).toBeInTheDocument()
    expect(screen.getByTestId('weight-trend-path')).toBeInTheDocument()
    expect(screen.getAllByTestId('weight-trend-point')).toHaveLength(2)
    expect(screen.getByText(/^71[,.]9 kg$/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Analytics' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Macro balance' })).not.toBeInTheDocument()
    expect(screen.getByTestId('analytics-macro-bar')).toBeInTheDocument()
    expect(screen.getByLabelText(/Protein:/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getAllByText(/30 days/).length).toBeGreaterThan(0)

    const rangeNav = screen.getByTestId('analytics-range-nav')
    expect(rangeNav).toHaveTextContent(formatAnalyticsRangeLabel(current.from, current.to))
    expect(screen.getByRole('button', { name: /next 30 days/i })).toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: /previous 30 days/i }))

    await waitFor(() => {
      expect(rangeSpy).toHaveBeenCalledWith(previous.from, previous.to)
    })
    expect(await screen.findByTestId('analytics-range-nav')).toHaveTextContent(
      formatAnalyticsRangeLabel(previous.from, previous.to),
    )
    expect(screen.getByRole('button', { name: /next 30 days/i })).not.toBeDisabled()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}>{children}</QueryClientProvider>)
}
