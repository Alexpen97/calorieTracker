import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import LookupPage from './LookupPage'
import * as client from '../api/client'

vi.mock('../platform/barcodeScan', () => ({
  isNativeBarcodeScanAvailable: async () => false,
  scanBarcodeNative: async () => null,
}))

describe('LookupPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('defaults to barcode mode and shows a method navbar', () => {
    render(
      <MemoryRouter>
        <LookupPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('navigation', { name: /add method/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Barcode' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /add your own/i })).toBeInTheDocument()

    expect(screen.getByLabelText('Barcode')).toBeInTheDocument()
    expect(screen.queryByLabelText('Product name')).not.toBeInTheDocument()
  })

  it('switches to search mode when selected', () => {
    render(
      <MemoryRouter>
        <LookupPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(screen.getByLabelText('Product name')).toBeInTheDocument()
    expect(screen.queryByLabelText('Barcode')).not.toBeInTheDocument()
  })

  it('links NEVO search results to their reference food detail while preserving meal', async () => {
    vi.spyOn(client, 'searchProducts').mockResolvedValue({
      query: 'paprika',
      page: 1,
      pageSize: 10,
      items: [
        {
          id: '0cf531b7-5978-33b8-b555-0bf50127fbaf',
          submissionId: null,
          barcode: null,
          source: 'NEVO',
          name: 'Sweet pepper green raw',
          brand: null,
          quantityLabel: null,
          servingSizeG: null,
          imageUrl: null,
          nutriScore: null,
          ingredientsText: null,
          allergenTags: [],
          offLastSyncedAt: null,
          nevoCode: '31',
          foodGroup: 'Vegetables',
          nutrients: [],
        },
      ],
    } as client.ProductSearchResult)

    render(
      <MemoryRouter initialEntries={['/lookup?meal=LUNCH']}>
        <LookupPage />
      </MemoryRouter>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    const input = screen.getByLabelText('Product name')
    fireEvent.change(input, { target: { value: 'paprika' } })
    fireEvent.submit(input.closest('form')!)

    const link = await screen.findByRole('link', { name: /sweet pepper green raw/i })
    expect(link).toHaveAttribute('href', '/nevo/31?meal=LUNCH')
    expect(link).toHaveTextContent('NEVO · Vegetables')
  })
})

