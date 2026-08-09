import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import LookupPage from './LookupPage'
import * as client from '../api/client'

vi.mock('../platform/barcodeScan', () => ({
  isNativeBarcodeScanAvailable: async () => false,
  scanBarcodeNative: async () => null,
}))

function renderLookup(initialEntry = '/lookup') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/lookup" element={<LookupPage />} />
          <Route path="/today" element={<div>Today page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const frequentItem: client.FrequentProduct = {
  productId: 'prod-yogurt',
  submissionId: null,
  productName: 'Greek yogurt',
  brand: 'Farm',
  logCount: 3,
  usualWeightG: 150,
  lastMealType: 'LUNCH',
  lastConsumedAt: '2026-08-01T12:30:00Z',
}

describe('LookupPage', () => {
  beforeEach(() => {
    vi.spyOn(client, 'fetchFrequentProducts').mockResolvedValue([])
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('defaults to barcode mode and shows a method navbar', async () => {
    renderLookup()

    expect(screen.getByRole('navigation', { name: /add method/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Barcode' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /add your own/i })).toBeInTheDocument()

    expect(screen.getByLabelText('Barcode')).toBeInTheDocument()
    expect(screen.queryByLabelText('Product name')).not.toBeInTheDocument()
    await waitFor(() => expect(client.fetchFrequentProducts).toHaveBeenCalled())
  })

  it('switches to search mode when selected', async () => {
    renderLookup()

    fireEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(screen.getByLabelText('Product name')).toBeInTheDocument()
    expect(screen.queryByLabelText('Barcode')).not.toBeInTheDocument()
    await waitFor(() => expect(client.fetchFrequentProducts).toHaveBeenCalled())
  })

  it('renders Quick add rows from frequent products', async () => {
    vi.spyOn(client, 'fetchFrequentProducts').mockResolvedValue([frequentItem])

    renderLookup()

    expect(
      await screen.findByRole('button', { name: /Add Greek yogurt, 150 grams/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Quick add' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Greek yogurt' })).toBeInTheDocument()
  })

  it('hides Quick add when frequent list is empty', async () => {
    renderLookup()

    await waitFor(() => expect(client.fetchFrequentProducts).toHaveBeenCalled())
    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Quick add' })).not.toBeInTheDocument(),
    )
  })

  it('one-tap add uses usual weight and lastMealType when meal param absent', async () => {
    vi.spyOn(client, 'fetchFrequentProducts').mockResolvedValue([frequentItem])
    const create = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-1',
      productId: frequentItem.productId,
      submissionId: null,
      productName: frequentItem.productName,
      brand: frequentItem.brand,
      weightG: 150,
      mealType: 'LUNCH',
      consumedAt: '2026-08-09T12:00:00Z',
      createdAt: '2026-08-09T12:00:00Z',
      nutrients: [],
    })

    renderLookup()

    fireEvent.click(await screen.findByRole('button', { name: /Add Greek yogurt, 150 grams/i }))

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        productId: 'prod-yogurt',
        weightG: 150,
        mealType: 'LUNCH',
      }),
    )
    expect(await screen.findByText('Today page')).toBeInTheDocument()
  })

  it('one-tap add prefers meal query param over lastMealType', async () => {
    vi.spyOn(client, 'fetchFrequentProducts').mockResolvedValue([frequentItem])
    const create = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-2',
      productId: frequentItem.productId,
      submissionId: null,
      productName: frequentItem.productName,
      brand: frequentItem.brand,
      weightG: 150,
      mealType: 'DINNER',
      consumedAt: '2026-08-09T12:00:00Z',
      createdAt: '2026-08-09T12:00:00Z',
      nutrients: [],
    })

    renderLookup('/lookup?meal=DINNER')

    fireEvent.click(await screen.findByRole('button', { name: /Add Greek yogurt, 150 grams/i }))

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        productId: 'prod-yogurt',
        weightG: 150,
        mealType: 'DINNER',
      }),
    )
  })

  it('keeps barcode/search nav when frequent fetch fails', async () => {
    vi.spyOn(client, 'fetchFrequentProducts').mockRejectedValue(new Error('network'))

    renderLookup()

    expect(await screen.findByRole('button', { name: /retry/i })).toBeInTheDocument()
    expect(screen.getByText(/load quick add/i)).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: /add method/i })).toBeInTheDocument()
    expect(screen.getByLabelText('Barcode')).toBeInTheDocument()
  })
})
