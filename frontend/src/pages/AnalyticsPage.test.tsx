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
        date: '2026-07-22',
        totals: [
          { code: 'protein', amount: 92, unit: 'g', target: 100 },
          { code: 'carbohydrates', amount: 210, unit: 'g', target: 250 },
          { code: 'fat', amount: 60, unit: 'g', target: 70 },
          { code: 'vitamin_d', amount: 5, unit: 'ug', target: 15 },
          { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
        ],
        water: { amountMl: 1800, targetMl: 2500 },
      },
    ])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.3, measuredAt: '2026-07-16T08:00:00Z' },
      { id: 'w2', weightKg: 71.9, measuredAt: '2026-07-22T08:00:00Z' },
    ])

    renderWithClient(<AnalyticsPage />)

    expect(await screen.findByText('Low')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Weight trend' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Analytics' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Macro balance' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getAllByText(/Vitamin D/i).length).toBeGreaterThan(0)
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}>{children}</QueryClientProvider>)
}
