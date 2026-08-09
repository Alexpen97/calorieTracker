import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ProductPage from './ProductPage'
import * as client from '../api/client'

const baseProduct = {
  id: 'prod-1',
  submissionId: null,
  barcode: '3017620422003',
  source: 'OFF' as const,
  name: 'Nutella',
  brand: 'Ferrero',
  quantityLabel: '400 g',
  servingSizeG: 15 as number | null,
  imageUrl: null,
  nutriScore: 'E' as string | null,
  ingredientsText: null,
  allergenTags: [] as string[],
  offLastSyncedAt: null,
  nutrients: [
    { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
  ],
}

function renderProduct(id = 'prod-1') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/products/${id}`]}>
        <Routes>
          <Route path="/products/:id" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ProductPage estimated nutrients', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows approx marker and footnote when a nutrient is estimated', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue({
      ...baseProduct,
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
      ...baseProduct,
      id: 'prod-2',
      name: 'Plain yogurt',
      brand: 'Farm',
      quantityLabel: null,
      servingSizeG: null,
      nutriScore: null,
      nutrients: [{ code: 'protein', amountPer100g: 4, unit: 'g', estimated: false }],
    })

    renderProduct('prod-2')

    expect(await screen.findByText(/protein/i)).toBeInTheDocument()
    expect(
      screen.queryByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).not.toBeInTheDocument()
  })
})

describe('ProductPage piece entry', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('offers Grams/Pieces when servingSizeG is positive', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)

    renderProduct()

    expect(await screen.findByRole('button', { name: /^Grams$/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Pieces$/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/Amount \(g\)/i)).toBeInTheDocument()
  })

  it('hides unit toggle when servingSizeG is missing', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue({
      ...baseProduct,
      id: 'prod-2',
      servingSizeG: null,
    })

    renderProduct('prod-2')

    expect(await screen.findByLabelText(/Amount \(g\)/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Pieces$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Grams$/i })).not.toBeInTheDocument()
  })

  it('submits weightG = pieces × servingSizeG', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-1',
      productId: 'prod-1',
      submissionId: null,
      productName: 'Nutella',
      brand: 'Ferrero',
      mealType: 'BREAKFAST',
      weightG: 30,
      consumedAt: '2026-08-09T08:00:00Z',
      createdAt: '2026-08-09T08:00:00Z',
      nutrients: [],
    })

    const qc = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/products/prod-1']}>
          <Routes>
            <Route path="/products/:id" element={<ProductPage />} />
            <Route path="/today" element={<p>Today</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await screen.findByRole('button', { name: /^Pieces$/i })
    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))

    expect(screen.getByLabelText(/Amount \(pieces\)/i)).toHaveValue(1)
    expect(screen.getByText(/1 piece ≈ 15 g/i)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText(/Amount \(pieces\)/i), { target: { value: '2' } })
    expect(screen.getByText(/2 pieces ≈ 30 g/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Add to diary/i }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          productId: 'prod-1',
          weightG: 30,
          mealType: 'BREAKFAST',
        }),
      )
    })
  })

  it('rejects non-integer piece amounts', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)
    const createSpy = vi.spyOn(client, 'createDiaryEntry')

    renderProduct()

    await screen.findByRole('button', { name: /^Pieces$/i })
    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))
    fireEvent.change(screen.getByLabelText(/Amount \(pieces\)/i), { target: { value: '1.5' } })
    fireEvent.click(screen.getByRole('button', { name: /Add to diary/i }))

    expect(await screen.findByText(/positive whole number of pieces/i)).toBeInTheDocument()
    expect(createSpy).not.toHaveBeenCalled()
  })

  it('resets amount when switching units', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(baseProduct)

    renderProduct()

    const amount = await screen.findByLabelText(/Amount \(g\)/i)
    fireEvent.change(amount, { target: { value: '250' } })

    fireEvent.click(screen.getByRole('button', { name: /^Pieces$/i }))
    expect(screen.getByLabelText(/Amount \(pieces\)/i)).toHaveValue(1)

    fireEvent.click(screen.getByRole('button', { name: /^Grams$/i }))
    expect(screen.getByLabelText(/Amount \(g\)/i)).toHaveValue(100)
  })
})
