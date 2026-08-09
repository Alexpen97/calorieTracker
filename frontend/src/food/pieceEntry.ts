export type AmountUnit = 'grams' | 'pieces'

export function canEnterByPieces(servingSizeG: number | null | undefined): boolean {
  return typeof servingSizeG === 'number' && Number.isFinite(servingSizeG) && servingSizeG > 0
}

export function piecesToWeightG(pieceCount: number, gramsPerPiece: number): number {
  return pieceCount * gramsPerPiece
}

export function parsePieceCount(raw: string): number | null {
  if (!/^\d+$/.test(raw)) {
    return null
  }
  const value = Number(raw)
  if (!Number.isInteger(value) || value <= 0) {
    return null
  }
  return value
}

export function pieceEquivalentLabel(pieceCount: number, gramsPerPiece: number): string {
  const totalG = piecesToWeightG(pieceCount, gramsPerPiece)
  const pieceWord = pieceCount === 1 ? 'piece' : 'pieces'
  return `${pieceCount} ${pieceWord} ≈ ${totalG} g`
}

export function defaultAmountForUnit(unit: AmountUnit): string {
  return unit === 'grams' ? '100' : '1'
}

export type ResolveWeightResult =
  | { ok: true; weightG: number }
  | { ok: false; error: string }

export function resolveWeightG(input: {
  unit: AmountUnit
  amount: string
  gramsPerPiece: number | null | undefined
}): ResolveWeightResult {
  if (input.unit === 'grams') {
    const parsedWeight = Number(input.amount)
    if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
      return { ok: false, error: 'Enter a positive gram amount.' }
    }
    return { ok: true, weightG: parsedWeight }
  }

  const pieceCount = parsePieceCount(input.amount)
  if (pieceCount == null || !canEnterByPieces(input.gramsPerPiece)) {
    return { ok: false, error: 'Enter a positive whole number of pieces.' }
  }
  return { ok: true, weightG: piecesToWeightG(pieceCount, input.gramsPerPiece as number) }
}
