import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ProductPage from './ProductPage'
import * as client from '../api/client'

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
      id: 'prod-1',
      submissionId: null,
      barcode: '3017620422003',
      source: 'OFF',
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
      id: 'prod-2',
      submissionId: null,
      barcode: '12345678',
      source: 'OFF',
      name: 'Plain yogurt',
      brand: 'Farm',
      quantityLabel: null,
      servingSizeG: null,
      imageUrl: null,
      nutriScore: null,
      ingredientsText: null,
      allergenTags: [],
      offLastSyncedAt: null,
      nutrients: [
        { code: 'protein', amountPer100g: 4, unit: 'g', estimated: false },
      ],
    })

    renderProduct('prod-2')

    expect(await screen.findByText(/protein/i)).toBeInTheDocument()
    expect(
      screen.queryByText(/≈ estimated from USDA FoodData Central generic data/i),
    ).not.toBeInTheDocument()
  })
})
