import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DiaryPage from './DiaryPage'
import * as client from '../api/client'
import { formatLocalDate, shiftLocalDate } from '../diary/formatDay'

describe('DiaryPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('renders diary timeline cards with stacked food summary bars', async () => {
    vi.spyOn(client, 'fetchDiarySummary').mockResolvedValue({
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 900, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 45, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 80, unit: 'g', target: 250 },
        { code: 'fat', amount: 30, unit: 'g', target: 70 },
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
    expect(screen.getByLabelText(/Calories:/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Protein:/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Carbs:/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/Fat:/i)).toBeInTheDocument()
    expect(screen.queryByRole('progressbar', { name: /Calories: \d+%/ })).not.toBeInTheDocument()
    expect(document.querySelector('.progress-ring')).not.toBeInTheDocument()
    expect(screen.getByTestId('diary-macro-bars')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /add food$/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Add food to Breakfast' })).toHaveAttribute(
      'href',
      '/lookup?meal=BREAKFAST',
    )
    expect(screen.getByRole('link', { name: 'Add food to Lunch' })).toHaveAttribute(
      'href',
      '/lookup?meal=LUNCH',
    )
    expect(screen.getByRole('link', { name: 'Add food to Dinner' })).toHaveAttribute(
      'href',
      '/lookup?meal=DINNER',
    )
    expect(screen.getByRole('link', { name: 'Add food to Snacks' })).toHaveAttribute(
      'href',
      '/lookup?meal=SNACK',
    )
  })

  it('shows the selected day between summary and meals and loads adjacent days', async () => {
    const today = formatLocalDate()
    const yesterday = shiftLocalDate(today, -1)

    const summarySpy = vi.spyOn(client, 'fetchDiarySummary').mockImplementation(async (date) => ({
      date,
      totals: [
        { code: 'energy_kcal', amount: date === yesterday ? 400 : 900, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 45, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 80, unit: 'g', target: 250 },
        { code: 'fat', amount: 30, unit: 'g', target: 70 },
      ],
      water: { amountMl: 750, targetMl: 2500 },
    }))
    vi.spyOn(client, 'fetchWater').mockResolvedValue([])
    vi.spyOn(client, 'fetchGoals').mockResolvedValue([])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([])
    vi.spyOn(client, 'fetchDiaryEntries').mockImplementation(async (date) =>
      date === yesterday
        ? [
            {
              id: 'entry-y',
              productId: 'p2',
              submissionId: null,
              productName: 'Yesterday salad',
              brand: null,
              weightG: 200,
              mealType: 'LUNCH',
              consumedAt: `${yesterday}T12:00:00Z`,
              createdAt: `${yesterday}T12:00:00Z`,
              nutrients: [{ code: 'energy_kcal', amount: 400, amountPer100g: 200, unit: 'kcal' }],
            },
          ]
        : [
            {
              id: 'entry-1',
              productId: 'p1',
              submissionId: null,
              productName: 'Morning oats',
              brand: 'Kitchen',
              weightG: 120,
              mealType: 'BREAKFAST',
              consumedAt: `${today}T08:00:00Z`,
              createdAt: `${today}T08:00:00Z`,
              nutrients: [{ code: 'energy_kcal', amount: 350, amountPer100g: 291.7, unit: 'kcal' }],
            },
          ],
    )

    renderWithClient(<DiaryPage />)

    expect(await screen.findByText('Morning oats')).toBeInTheDocument()
    const dayNav = screen.getByTestId('diary-day-nav')
    expect(dayNav).toHaveTextContent(/Today/)
    expect(dayNav.compareDocumentPosition(screen.getByRole('heading', { name: 'Meals' }))).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )

    fireEvent.click(screen.getByRole('button', { name: /previous day/i }))

    expect(await screen.findByText('Yesterday salad')).toBeInTheDocument()
    expect(screen.queryByText('Morning oats')).not.toBeInTheDocument()
    expect(screen.getByTestId('diary-day-nav')).toHaveTextContent(/Yesterday/)
    await waitFor(() => {
      expect(summarySpy).toHaveBeenCalledWith(yesterday)
    })

    fireEvent.click(screen.getByRole('button', { name: /next day/i }))
    expect(await screen.findByText('Morning oats')).toBeInTheDocument()
    expect(screen.getByTestId('diary-day-nav')).toHaveTextContent(/Today/)
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
