import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AnalyticsPage from './AnalyticsPage'
import * as client from '../api/client'

describe('AnalyticsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders weight, macro, vitamin, mineral, and insight cards', async () => {
    vi.spyOn(client, 'fetchDiarySummaryRange').mockResolvedValue([
      {
        date: '2026-07-21',
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
        date: '2026-07-22',
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
      { id: 'w1', weightKg: 72.3, measuredAt: '2026-07-16T08:00:00Z' },
      { id: 'w2', weightKg: 71.9, measuredAt: '2026-07-22T08:00:00Z' },
    ])

    renderWithClient(<AnalyticsPage />)

    expect(await screen.findByLabelText('Vitamin D')).toHaveAttribute('aria-valuenow', '40')
    expect(screen.getByLabelText('Calcium')).toHaveAttribute('aria-valuenow', '60')
    expect(screen.getByLabelText('Vitamin C')).toBeInTheDocument()
    expect(screen.getByLabelText('Iron')).toBeInTheDocument()
    expect(weightSpy).toHaveBeenCalledWith({
      from: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      to: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    })
    expect(screen.getByRole('heading', { name: 'Weight trend' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Analytics' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Macro balance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}>{children}</QueryClientProvider>)
}
