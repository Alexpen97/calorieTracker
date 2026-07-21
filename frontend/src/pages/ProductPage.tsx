import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { fetchProductById } from '../api/client'
import { useState } from 'react'
import NutrientSheet from '../food/NutrientSheet'

export default function ProductPage() {
  const { id = '' } = useParams()
  const [selectedNutrient, setSelectedNutrient] = useState<string | null>(null)
  const { data, error, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => fetchProductById(id),
    enabled: Boolean(id),
  })

  return (
    <main className="panel product-panel">
      <p className="crumb">
        <Link to="/lookup">← Look up</Link>
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
