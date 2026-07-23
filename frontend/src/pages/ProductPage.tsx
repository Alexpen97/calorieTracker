import { useMutation } from '@tanstack/react-query'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { createDiaryEntry, fetchProductById, type MealType } from '../api/client'
import { mealLookupPath, parseMealTypeParam } from '../diary/formatDay'
import { useState, type FormEvent } from 'react'
import NutrientSheet from '../food/NutrientSheet'

export default function ProductPage() {
  const { id = '' } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const selectedMeal = parseMealTypeParam(searchParams.get('meal'))
  const [selectedNutrient, setSelectedNutrient] = useState<string | null>(null)
  const [weightG, setWeightG] = useState('100')
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

  async function onAddToDiary(event: FormEvent) {
    event.preventDefault()
    if (!data) {
      return
    }
    const parsedWeight = Number(weightG)
    if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
      setEntryError('Enter a positive gram amount.')
      return
    }
    setEntryError(null)
    if (data.submissionId || data.source === 'PENDING_SUBMISSION') {
      addEntry.mutate({
        submissionId: data.submissionId ?? data.id,
        weightG: parsedWeight,
        mealType,
      })
    } else {
      addEntry.mutate({ productId: data.id, weightG: parsedWeight, mealType })
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
            <form className="lookup-form" onSubmit={onAddToDiary}>
              <label htmlFor="diary-weight">Amount (g)</label>
              <input
                id="diary-weight"
                inputMode="decimal"
                min="1"
                onChange={(event) => setWeightG(event.target.value)}
                type="number"
                value={weightG}
              />
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
                    <span>{n.code.replaceAll('_', ' ')}</span>
                    <span>
                      {n.amountPer100g} {n.unit}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
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
