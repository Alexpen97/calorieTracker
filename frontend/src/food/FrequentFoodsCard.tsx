import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import {
  createDiaryEntry,
  fetchFrequentProducts,
  type FrequentProduct,
  type MealType,
} from '../api/client'
import { productPathWithMeal } from '../diary/formatDay'
import { DashboardCard } from '../ui/Card'

type FrequentFoodsCardProps = {
  mealFromUrl: MealType | null
}

function resolveMeal(item: FrequentProduct, mealFromUrl: MealType | null): MealType {
  return mealFromUrl ?? item.lastMealType
}

function productLinkId(item: FrequentProduct): string | null {
  return item.productId ?? item.submissionId
}

export default function FrequentFoodsCard({ mealFromUrl }: FrequentFoodsCardProps) {
  const navigate = useNavigate()
  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['diary', 'frequent'],
    queryFn: () => fetchFrequentProducts(),
    retry: false,
  })

  const add = useMutation({
    mutationFn: (item: FrequentProduct) => {
      const mealType = resolveMeal(item, mealFromUrl)
      if (item.productId) {
        return createDiaryEntry({
          productId: item.productId,
          weightG: item.usualWeightG,
          mealType,
        })
      }
      if (item.submissionId) {
        return createDiaryEntry({
          submissionId: item.submissionId,
          weightG: item.usualWeightG,
          mealType,
        })
      }
      return Promise.reject(new Error('Frequent item is missing product identity'))
    },
    onSuccess: () => navigate('/today'),
  })

  if (!isLoading && !isError && (data?.length ?? 0) === 0) {
    return null
  }

  return (
    <DashboardCard density="list" title="Quick add" className="frequent-foods-card">
      {isLoading ? <p className="frequent-foods-status">Loading…</p> : null}
      {isError ? (
        <p className="frequent-foods-status">
          Couldn’t load quick add.{' '}
          <button
            type="button"
            className="frequent-foods-retry"
            onClick={() => void refetch()}
            disabled={isFetching}
          >
            Retry
          </button>
        </p>
      ) : null}
      {!isLoading && !isError && data && data.length > 0 ? (
        <ul className="frequent-food-list">
          {data.map((item) => {
            const id = productLinkId(item)
            const meal = resolveMeal(item, mealFromUrl)
            const busy = add.isPending
            return (
              <li key={item.productId ?? item.submissionId ?? item.productName}>
                <div className="frequent-food-row">
                  <div className="frequent-food-copy">
                    {id ? (
                      <Link
                        className="frequent-food-name"
                        to={productPathWithMeal(id, meal)}
                      >
                        {item.productName}
                      </Link>
                    ) : (
                      <span className="frequent-food-name">{item.productName}</span>
                    )}
                    <span className="frequent-food-meta">
                      {[item.brand, `${item.usualWeightG} g`, item.lastMealType.toLowerCase()]
                        .filter(Boolean)
                        .join(' · ')}
                    </span>
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary frequent-food-add"
                    aria-label={`Add ${item.productName}, ${item.usualWeightG} grams`}
                    disabled={busy}
                    onClick={() => add.mutate(item)}
                  >
                    +
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      ) : null}
      {add.isError ? (
        <p className="error" role="alert">
          {add.error instanceof Error ? add.error.message : 'Could not add to diary'}
        </p>
      ) : null}
    </DashboardCard>
  )
}
