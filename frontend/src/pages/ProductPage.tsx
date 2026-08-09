import { useMutation } from '@tanstack/react-query'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { createDiaryEntry, fetchProductById, type MealType } from '../api/client'
import { mealLookupPath, parseMealTypeParam } from '../diary/formatDay'
import { useEffect, useState, type FormEvent } from 'react'
import NutrientSheet from '../food/NutrientSheet'
import {
  canEnterByPieces,
  defaultAmountForUnit,
  pieceEquivalentLabel,
  resolveWeightG as resolvePieceWeightG,
  type AmountUnit as PieceAmountUnit,
} from '../food/pieceEntry'
import {
  isVolumeCapable,
  resolveWeightG as resolveVolumeWeightG,
  type AmountUnit as VolumeAmountUnit,
} from '../food/volumeConversion'

export default function ProductPage() {
  const { id = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const selectedMeal = parseMealTypeParam(searchParams.get('meal'))
  const [selectedNutrient, setSelectedNutrient] = useState<string | null>(null)
  const [amountUnit, setAmountUnit] = useState<PieceAmountUnit>('grams')
  const [volumeUnit, setVolumeUnit] = useState<VolumeAmountUnit>('g')
  const [amount, setAmount] = useState('100')
  const [mealType, setMealType] = useState<MealType>(() => selectedMeal ?? 'BREAKFAST')
  const [entryError, setEntryError] = useState<string | null>(null)
  const { data, error, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => fetchProductById(id),
    enabled: Boolean(id),
  })
  const addEntry = useMutation({
    mutationFn: (input: {
      productId?: string
      submissionId?: string
      weightG: number
      mealType: MealType
    }) => createDiaryEntry(input),
    onSuccess: () => navigate('/today'),
  })

  const volumeCapable = isVolumeCapable(data?.densityGPerMl)
  const pieceModeAvailable = canEnterByPieces(data?.servingSizeG)
  const activePieceUnit: PieceAmountUnit = pieceModeAvailable ? amountUnit : 'grams'
  const pieceCountForLabel = activePieceUnit === 'pieces' ? Number(amount) : NaN
  const pieceHelperText =
    activePieceUnit === 'pieces' &&
    pieceModeAvailable &&
    Number.isInteger(pieceCountForLabel) &&
    pieceCountForLabel > 0
      ? pieceEquivalentLabel(pieceCountForLabel, data!.servingSizeG!)
      : null
  const volumeHelperWeightG =
    data &&
    activePieceUnit === 'grams' &&
    volumeUnit === 'ml' &&
    volumeCapable &&
    Number.isFinite(Number(amount)) &&
    Number(amount) > 0
      ? resolveVolumeWeightG(Number(amount), 'ml', data.densityGPerMl)
      : null

  useEffect(() => {
    if (!data) {
      return
    }
    setAmountUnit('grams')
    setVolumeUnit(isVolumeCapable(data.densityGPerMl) ? 'ml' : 'g')
    setAmount('100')
    setEntryError(null)
  }, [data?.id, data?.densityGPerMl])

  function switchPieceUnit(next: PieceAmountUnit) {
    setAmountUnit(next)
    setAmount(defaultAmountForUnit(next))
    setEntryError(null)
  }

  async function onAddToDiary(event: FormEvent) {
    event.preventDefault()
    if (!data) {
      return
    }

    let weightG: number
    if (activePieceUnit === 'pieces') {
      const resolved = resolvePieceWeightG({
        unit: 'pieces',
        amount,
        gramsPerPiece: data.servingSizeG,
      })
      if (!resolved.ok) {
        setEntryError(resolved.error)
        return
      }
      weightG = resolved.weightG
    } else if (volumeCapable && volumeUnit === 'ml') {
      const parsedAmount = Number(amount)
      if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
        setEntryError('Enter a positive amount.')
        return
      }
      weightG = resolveVolumeWeightG(parsedAmount, 'ml', data.densityGPerMl)
    } else {
      const resolved = resolvePieceWeightG({
        unit: 'grams',
        amount,
        gramsPerPiece: data.servingSizeG,
      })
      if (!resolved.ok) {
        setEntryError(resolved.error)
        return
      }
      weightG = resolved.weightG
    }

    setEntryError(null)
    if (data.submissionId || data.source === 'PENDING_SUBMISSION') {
      addEntry.mutate({
        submissionId: data.submissionId ?? data.id,
        weightG,
        mealType,
      })
    } else {
      addEntry.mutate({ productId: data.id, weightG, mealType })
    }
  }

  const amountLabel =
    activePieceUnit === 'pieces'
      ? 'Amount (pieces)'
      : volumeCapable
        ? 'Amount'
        : 'Amount (g)'

  return (
    <main className="panel product-panel">
      <p className="crumb">
        <Link to={selectedMeal ? mealLookupPath(selectedMeal) : '/lookup'}>← Look up</Link>
      </p>
      {isLoading && <p>Loading product…</p>}
      {error && <p className="error">{(error as Error).message}</p>}
      {data && (
        <>
          <div className="product-hero">
            {data.imageUrl ? (
              <img className="product-image" src={data.imageUrl} alt="" />
            ) : (
              <div className="product-image placeholder" aria-hidden />
            )}
            <div>
              <h2>{data.name}</h2>
              {data.source === 'PENDING_SUBMISSION' && (
                <p className="product-meta">Awaiting review — visible only to you until approved.</p>
              )}
              <p className="product-meta">
                {[data.brand, data.quantityLabel, data.barcode && `EAN ${data.barcode}`]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
              {data.nutriScore && (
                <p className="nutri-score">
                  Nutri-Score <strong>{data.nutriScore}</strong>
                </p>
              )}
            </div>
          </div>

          <section className="nutrient-section">
            <h3>Add to today</h3>
            <form className="lookup-form" noValidate onSubmit={onAddToDiary}>
              {pieceModeAvailable && (
                <div className="unit-toggle" role="group" aria-label="Amount unit">
                  <button
                    aria-pressed={activePieceUnit === 'grams'}
                    className={activePieceUnit === 'grams' ? 'unit-toggle-btn active' : 'unit-toggle-btn'}
                    onClick={() => switchPieceUnit('grams')}
                    type="button"
                  >
                    Grams
                  </button>
                  <button
                    aria-pressed={activePieceUnit === 'pieces'}
                    className={activePieceUnit === 'pieces' ? 'unit-toggle-btn active' : 'unit-toggle-btn'}
                    onClick={() => switchPieceUnit('pieces')}
                    type="button"
                  >
                    Pieces
                  </button>
                </div>
              )}
              <label htmlFor="diary-amount">{amountLabel}</label>
              <input
                id="diary-amount"
                inputMode={activePieceUnit === 'pieces' ? 'numeric' : 'decimal'}
                min="1"
                onChange={(event) => setAmount(event.target.value)}
                step="any"
                type="number"
                value={amount}
              />
              {activePieceUnit === 'grams' && volumeCapable && (
                <>
                  <label htmlFor="diary-unit">Unit</label>
                  <select
                    id="diary-unit"
                    onChange={(event) => setVolumeUnit(event.target.value as VolumeAmountUnit)}
                    value={volumeUnit}
                  >
                    <option value="g">g</option>
                    <option value="ml">ml</option>
                  </select>
                  {volumeHelperWeightG !== null && (
                    <p className="product-meta">
                      ≈ {volumeHelperWeightG} g at {data.densityGPerMl} g/ml
                    </p>
                  )}
                </>
              )}
              {pieceHelperText && <p className="product-meta">{pieceHelperText}</p>}
              <label htmlFor="diary-meal">Meal</label>
              <select
                id="diary-meal"
                onChange={(event) => setMealType(event.target.value as MealType)}
                value={mealType}
              >
                <option value="BREAKFAST">Breakfast</option>
                <option value="LUNCH">Lunch</option>
                <option value="DINNER">Dinner</option>
                <option value="SNACK">Snack</option>
              </select>
              <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
                <button className="btn btn-primary" disabled={addEntry.isPending} type="submit">
                  {addEntry.isPending ? 'Adding…' : 'Add to diary'}
                </button>
              </div>
            </form>
            {(entryError || addEntry.error) && (
              <p className="error">{entryError ?? (addEntry.error as Error).message}</p>
            )}
          </section>

          <section className="nutrient-section">
            <h3>Nutrition per 100 g</h3>
            <p>Tap a nutrient for what it does in the body.</p>
            <ul className="nutrient-list">
              {data.nutrients.map((n) => (
                <li key={n.code}>
                  <button
                    type="button"
                    className="nutrient-row"
                    onClick={() => setSelectedNutrient(n.code)}
                  >
                    <span>
                      {n.estimated ? '≈ ' : ''}
                      {n.code.replaceAll('_', ' ')}
                    </span>
                    <span>
                      {n.amountPer100g} {n.unit}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {data.nutrients.some((n) => n.estimated) && (
              <p className="product-meta">
                ≈ estimated from USDA FoodData Central generic data
              </p>
            )}
          </section>

          {data.ingredientsText && (
            <section className="nutrient-section">
              <h3>Ingredients</h3>
              <p>{data.ingredientsText}</p>
            </section>
          )}

          <p className="attribution">
            Data source: {data.source === 'OFF' ? 'Open Food Facts' : data.source}. Not medical
            advice.
          </p>
        </>
      )}

      {selectedNutrient && (
        <NutrientSheet code={selectedNutrient} onClose={() => setSelectedNutrient(null)} />
      )}
    </main>
  )
}
