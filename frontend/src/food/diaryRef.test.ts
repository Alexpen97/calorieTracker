import { describe, expect, it } from 'vitest'

/** Prefer submissionId when logging a pending user product. */
export function diaryRefFromProduct(product: {
  id: string
  submissionId?: string | null
  source: string
}): { productId?: string; submissionId?: string } {
  if (product.submissionId || product.source === 'PENDING_SUBMISSION') {
    return { submissionId: product.submissionId ?? product.id }
  }
  return { productId: product.id }
}

describe('diaryRefFromProduct', () => {
  it('uses productId for catalog products', () => {
    expect(diaryRefFromProduct({ id: 'p1', source: 'OFF' })).toEqual({ productId: 'p1' })
  })

  it('uses submissionId for pending submissions', () => {
    expect(
      diaryRefFromProduct({ id: 's1', submissionId: 's1', source: 'PENDING_SUBMISSION' }),
    ).toEqual({ submissionId: 's1' })
  })
})
