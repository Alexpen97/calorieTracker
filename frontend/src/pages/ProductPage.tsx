import { useMutation } from '@tanstack/react-query'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { createDiaryEntry, fetchProductById, type MealType } from '../api/client'
import { mealLookupPath, parseMealTypeParam } from '../diary/formatDay'
import { useEffect, useState, type FormEvent } from 'react'
import NutrientSheet from '../food/NutrientSheet'
import {
  isVolumeCapable,
  resolveWeightG,
  type AmountUnit,
} from '../food/volumeConversion'

export default function ProductPage() {
  const { id = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const selectedMeal = parseMealTypeParam(searchParams.get('meal'))
  const [selectedNutrient, setSelectedNutrient] = useState<string | null>(null)
  const [amount, setAmount] = useState('100')
  const [unit, setUnit] = useState<AmountUnit>('g')
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
  const helperWeightG =
    data && unit === 'ml' && volumeCapable && Number.isFinite(Number(amount)) && Number(amount) > 0
      ? resolveWeightG(Number(amount), unit, data.densityGPerMl)
      : null

  useEffect(() => {
    if (!data) {
      return
    }
    setAmount('100')
    setUnit(isVolumeCapable(data.densityGPerMl) ? 'ml' : 'g')
    setEntryError(null)
  }, [data?.id, data?.densityGPerMl])

  async function onAddToDiary(event: FormEvent) {
    event.preventDefault()
    if (!data) {
      return
    }
    const parsedAmount = Number(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setEntryError('Enter a positive amount.')
      return
    }
    const selectedUnit = volumeCapable ? unit : 'g'
    const weightG = resolveWeightG(parsedAmount, selectedUnit, data.densityGPerMl)
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
              <label htmlFor="diary-amount">{volumeCapable ? 'Amount' : 'Amount (g)'}</label>
              <input
                id="diary-amount"
                inputMode="decimal"
                min="1"
                onChange={(event) => setAmount(event.target.value)}
                type="number"
                value={amount}
              />
              {volumeCapable && (
                <>
                  <label htmlFor="diary-unit">Unit</label>
                  <select
                    id="diary-unit"
                    onChange={(event) => setUnit(event.target.value as AmountUnit)}
                    value={unit}
                  >
                    <option value="g">g</option>
                    <option value="ml">ml</option>
                  </select>
                  {helperWeightG !== null && (
                    <p className="product-meta">
                      ≈ {helperWeightG} g at {data.densityGPerMl} g/ml
                    </p>
                  )}
                </>
              )}
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
