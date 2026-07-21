import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NutrientSheet from './NutrientSheet'
import * as client from '../api/client'

describe('NutrientSheet', () => {
  it('renders education fields from the API', async () => {
    const spy = vi.spyOn(client, 'fetchNutrient').mockResolvedValue({
      code: 'protein',
      displayName: 'Protein',
      category: 'MACRO',
      defaultUnit: 'g',
      description: 'Amino acid chains.',
      bodyEffects: 'Builds tissues.',
      deficiencyEffects: 'Muscle loss.',
      excessEffects: 'Kidney stress in disease.',
      commonSources: 'Eggs, legumes.',
      contentSource: 'https://example.test/protein',
    })

    const qc = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    render(
      <QueryClientProvider client={qc}>
        <NutrientSheet code="protein" onClose={() => undefined} />
      </QueryClientProvider>,
    )

    expect(await screen.findByRole('heading', { name: 'Protein' })).toBeInTheDocument()
    expect(screen.getByText('Builds tissues.')).toBeInTheDocument()
    expect(screen.getByText(/not medical advice/i)).toBeInTheDocument()
    spy.mockRestore()
  })
})
