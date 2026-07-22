import { useQuery } from '@tanstack/react-query'
import { fetchDiarySummaryRange, fetchWeightHistory } from '../api/client'
import { dateDaysAgo } from '../diary/nutritionDashboard'
import { formatLocalDate } from '../diary/formatDay'
import AnalyticsView from '../screens/AnalyticsView'

export default function AnalyticsPage() {
  const to = formatLocalDate()
  const from = dateDaysAgo(6)
  const rangeQuery = useQuery({
    queryKey: ['diary-summary-range', from, to],
    queryFn: () => fetchDiarySummaryRange(from, to),
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history', from, to],
    queryFn: () => fetchWeightHistory({ from, to }),
  })

  return (
    <>
      {(rangeQuery.isLoading || weightQuery.isLoading) && <p className="mobile-page">Loading analytics…</p>}
      {[rangeQuery.error, weightQuery.error].filter(Boolean).map((error, index) => (
        <p className="error mobile-page" key={index}>
          {(error as Error).message}
        </p>
      ))}
      {rangeQuery.data && (
        <AnalyticsView from={from} to={to} summaries={rangeQuery.data} weightHistory={weightQuery.data ?? []} />
      )}
    </>
  )
}
