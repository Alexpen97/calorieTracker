import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DiaryPage from './DiaryPage'
import * as client from '../api/client'

describe('DiaryPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders diary timeline cards with daily macro summary', async () => {
    vi.spyOn(client, 'fetchDiarySummary').mockResolvedValue({
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 900, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 45, unit: 'g', target: 100 },
        { code: 'vitamin_c', amount: 60, unit: 'mg', target: 80 },
        { code: 'iron', amount: 9, unit: 'mg', target: 14 },
      ],
      water: { amountMl: 750, targetMl: 2500 },
    })
    vi.spyOn(client, 'fetchWater').mockResolvedValue([])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([])
    vi.spyOn(client, 'fetchDiaryEntries').mockResolvedValue([
      {
        id: 'entry-1',
        productId: 'p1',
        submissionId: null,
        productName: 'Morning oats',
        brand: 'Kitchen',
        weightG: 120,
        mealType: 'BREAKFAST',
        consumedAt: '2026-07-22T08:00:00Z',
        createdAt: '2026-07-22T08:00:00Z',
        nutrients: [{ code: 'energy_kcal', amount: 350, amountPer100g: 291.7, unit: 'kcal' }],
      },
    ])

    renderWithClient(<DiaryPage />)

    expect(await screen.findByText('Morning oats')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Food Diary' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Breakfast' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamin Checklist' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Mineral Checklist' })).toBeInTheDocument()
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
