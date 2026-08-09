import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { fetchNevoFood } from '../api/client'
import { mealLookupPath, parseMealTypeParam } from '../diary/formatDay'
import NutrientSheet from '../food/NutrientSheet'

export default function NevoFoodPage() {
  const { code = '' } = useParams()
  const [searchParams] = useSearchParams()
  const selectedMeal = parseMealTypeParam(searchParams.get('meal'))
  const [selectedNutrient, setSelectedNutrient] = useState<string | null>(null)
  const { data, error, isLoading } = useQuery({
    queryKey: ['nevo-food', code],
    queryFn: () => fetchNevoFood(code),
    enabled: Boolean(code),
  })
  const nevoVersion = data?.nevoVersion ?? '2025/9.0'

  return (
    <main className="panel product-panel">
      <p className="crumb">
        <Link to={selectedMeal ? mealLookupPath(selectedMeal) : '/lookup'}>← Look up</Link>
      </p>
      {isLoading && <p>Loading NEVO food…</p>}
      {error && <p className="error">{(error as Error).message}</p>}
      {data && (
        <>
          <div className="product-hero">
            <div className="product-image placeholder" aria-hidden />
            <div>
              <h2>{data.foodName}</h2>
              <p className="product-meta">{data.foodGroup ?? 'NEVO reference food'}</p>
              <p className="product-meta">NEVO code {data.nevoCode}</p>
            </div>
          </div>

          <section className="nutrient-section">
            <h3>Add to today</h3>
            <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
              <button className="btn btn-primary" disabled type="button">
                Add to diary
              </button>
            </div>
            <p className="product-meta">Diary logging for NEVO foods tracked separately.</p>
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

          <p className="attribution">Data source: NEVO-online {nevoVersion}, RIVM. Not medical advice.</p>
        </>
      )}

      {selectedNutrient && (
        <NutrientSheet code={selectedNutrient} onClose={() => setSelectedNutrient(null)} />
      )}
    </main>
  )
}
