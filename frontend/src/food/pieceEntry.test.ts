import { describe, expect, it } from 'vitest'
import {
  canEnterByPieces,
  defaultAmountForUnit,
  parsePieceCount,
  pieceEquivalentLabel,
  piecesToWeightG,
  resolveWeightG,
  type AmountUnit,
} from './pieceEntry'

describe('canEnterByPieces', () => {
  it('is true only for positive servingSizeG', () => {
    expect(canEnterByPieces(15)).toBe(true)
    expect(canEnterByPieces(0.5)).toBe(true)
    expect(canEnterByPieces(null)).toBe(false)
    expect(canEnterByPieces(undefined)).toBe(false)
    expect(canEnterByPieces(0)).toBe(false)
    expect(canEnterByPieces(-10)).toBe(false)
  })
})

describe('piecesToWeightG', () => {
  it('multiplies piece count by grams per piece', () => {
    expect(piecesToWeightG(2, 50)).toBe(100)
    expect(piecesToWeightG(1, 15)).toBe(15)
    expect(piecesToWeightG(3, 12.5)).toBe(37.5)
  })
})

describe('parsePieceCount', () => {
  it('accepts positive integers only', () => {
    expect(parsePieceCount('1')).toBe(1)
    expect(parsePieceCount('3')).toBe(3)
    expect(parsePieceCount('0')).toBeNull()
    expect(parsePieceCount('-1')).toBeNull()
    expect(parsePieceCount('1.5')).toBeNull()
    expect(parsePieceCount('')).toBeNull()
    expect(parsePieceCount('abc')).toBeNull()
    expect(parsePieceCount('2.0')).toBeNull()
  })
})

describe('pieceEquivalentLabel', () => {
  it('describes gram equivalent for the entered count', () => {
    expect(pieceEquivalentLabel(1, 50)).toBe('1 piece ≈ 50 g')
    expect(pieceEquivalentLabel(2, 50)).toBe('2 pieces ≈ 100 g')
  })
})

describe('defaultAmountForUnit', () => {
  it('resets to mode defaults', () => {
    expect(defaultAmountForUnit('grams')).toBe('100')
    expect(defaultAmountForUnit('pieces')).toBe('1')
  })
})

describe('resolveWeightG', () => {
  it('returns grams for grams mode', () => {
    expect(resolveWeightG({ unit: 'grams' as AmountUnit, amount: '125', gramsPerPiece: 50 })).toEqual({
      ok: true,
      weightG: 125,
    })
  })

  it('rejects invalid grams', () => {
    expect(resolveWeightG({ unit: 'grams', amount: '0', gramsPerPiece: 50 })).toEqual({
      ok: false,
      error: 'Enter a positive gram amount.',
    })
  })

  it('converts pieces using serving size', () => {
    expect(resolveWeightG({ unit: 'pieces', amount: '2', gramsPerPiece: 50 })).toEqual({
      ok: true,
      weightG: 100,
    })
  })

  it('rejects invalid piece counts', () => {
    expect(resolveWeightG({ unit: 'pieces', amount: '1.5', gramsPerPiece: 50 })).toEqual({
      ok: false,
      error: 'Enter a positive whole number of pieces.',
    })
  })
})
