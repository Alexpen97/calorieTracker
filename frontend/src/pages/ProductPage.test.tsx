import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProductPage from './ProductPage'
import * as client from '../api/client'

function renderProduct(initialEntry = '/products/prod-1') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/products/:id" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function mockProduct(id = 'prod-1') {
  return {
    id,
    submissionId: null,
    barcode: '3017620422003',
    source: 'OFF' as const,
    name: 'Nutella',
    brand: 'Ferrero',
    quantityLabel: '400 g',
    servingSizeG: 15,
    imageUrl: null,
    nutriScore: 'E',
    ingredientsText: null,
    allergenTags: [],
    offLastSyncedAt: null,
    nutrients: [
      { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
    ],
  }
}

describe('ProductPage estimated nutrients', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows approx marker and footnote when a nutrient is estimated', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue({
      ...mockProduct(),
      nutrients: [
        { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
        { code: 'vitamin_b3', amountPer100g: 1.2, unit: 'mg', estimated: true },
      ],
    })

    renderProduct()

    expect(await screen.findByText(/≈ vitamin b3/i)).toBeInTheDocument()
    expect(
      screen.getByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).toBeInTheDocument()
  })

  it('hides footnote when no nutrients are estimated', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue({
      ...mockProduct('prod-2'),
      name: 'Plain yogurt',
      brand: 'Farm',
      barcode: '12345678',
      quantityLabel: null,
      servingSizeG: null,
      nutriScore: null,
      nutrients: [
        { code: 'protein', amountPer100g: 4, unit: 'g', estimated: false },
      ],
    })

    renderProduct('/products/prod-2')

    expect(await screen.findByText(/protein/i)).toBeInTheDocument()
    expect(
      screen.queryByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).not.toBeInTheDocument()
  })
})

describe('ProductPage meal selection', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('defaults the meal select from local browser time when no meal param exists', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1')

    expect(await screen.findByLabelText('Meal')).toHaveValue('LUNCH')
  })

  it('uses an explicit valid meal param instead of local time inference', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1?meal=DINNER')

    expect(await screen.findByLabelText('Meal')).toHaveValue('DINNER')
  })

  it('falls back to local time when the meal param is invalid', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 23, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1?meal=brunch')

    expect(await screen.findByLabelText('Meal')).toHaveValue('SNACK')
  })

  it('submits the user-selected meal override', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-1',
      productId: 'prod-1',
      submissionId: null,
      productName: 'Nutella',
      brand: 'Ferrero',
      weightG: 100,
      mealType: 'SNACK',
      consumedAt: '2026-07-22T12:00:00Z',
      createdAt: '2026-07-22T12:00:00Z',
      nutrients: [],
    })

    renderProduct('/products/prod-1')

    fireEvent.change(await screen.findByLabelText('Meal'), { target: { value: 'SNACK' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add to diary' }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        productId: 'prod-1',
        weightG: 100,
        mealType: 'SNACK',
      })
    })
  })
})
