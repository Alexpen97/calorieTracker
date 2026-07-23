import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import LookupPage from './LookupPage'

vi.mock('../platform/barcodeScan', () => ({
  isNativeBarcodeScanAvailable: async () => false,
  scanBarcodeNative: async () => null,
}))

describe('LookupPage', () => {
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
})

