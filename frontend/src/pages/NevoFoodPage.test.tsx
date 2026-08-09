import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import NevoFoodPage from './NevoFoodPage'
import * as client from '../api/client'

function renderNevoFoodPage(code = '31') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/nevo/${code}?meal=LUNCH`]}>
        <Routes>
          <Route path="/nevo/:code" element={<NevoFoodPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('NevoFoodPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows NEVO reference details and disables diary logging', async () => {
    vi.spyOn(client, 'fetchNevoFood').mockResolvedValue({
      matched: true,
      nevoCode: '31',
      foodName: 'Sweet pepper green raw',
      foodGroup: 'Vegetables',
      nevoVersion: '2025/9.0',
      confidence: 'EXACT',
      score: 9.5,
      reasons: [],
      nutrients: [
        { code: 'energy_kcal', amountPer100g: 20, unit: 'kcal' },
        { code: 'protein', amountPer100g: 0.9, unit: 'g' },
      ],
    })

    renderNevoFoodPage()

    expect(await screen.findByRole('heading', { name: /sweet pepper green raw/i })).toBeInTheDocument()
    expect(screen.getByText('Vegetables')).toBeInTheDocument()
    expect(screen.getByText(/energy kcal/i)).toBeInTheDocument()
    expect(screen.getByText(/20 kcal/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /add to diary/i })).toBeDisabled()
    expect(screen.getByText(/Diary logging for NEVO foods tracked separately/i)).toBeInTheDocument()
    expect(screen.getByText(/NEVO-online 2025\/9.0, RIVM/i)).toBeInTheDocument()
  })
})
