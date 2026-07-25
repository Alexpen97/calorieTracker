import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { fetchDiarySummaryRange, fetchGoals, fetchWeightHistory } from '../api/client'
import {
  analyticsRangeFromEnd,
  formatAnalyticsRangeLabel,
  formatLocalDate,
  shiftAnalyticsRangeEnd,
} from '../diary/formatDay'
import { mergeSummaryWithGoals } from '../diary/mergeSummaryGoals'
import AnalyticsView from '../screens/AnalyticsView'

export default function AnalyticsPage() {
  const today = formatLocalDate()
  const [rangeEnd, setRangeEnd] = useState(today)
  const { from, to } = analyticsRangeFromEnd(rangeEnd)
  const canGoNext = rangeEnd < today

  const rangeQuery = useQuery({
    queryKey: ['diary-summary-range', from, to],
    queryFn: () => fetchDiarySummaryRange(from, to),
  })
  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history', from, to],
    queryFn: () => fetchWeightHistory({ from, to }),
  })

  const summaries = rangeQuery.data?.map((summary) => mergeSummaryWithGoals(summary, goalsQuery.data))

  return (
    <>
      {(rangeQuery.isLoading || weightQuery.isLoading) && <p className="mobile-page">Loading analytics…</p>}
      {[rangeQuery.error, goalsQuery.error, weightQuery.error].filter(Boolean).map((error, index) => (
        <p className="error mobile-page" key={index}>
          {(error as Error).message}
        </p>
      ))}
      {summaries && (
        <AnalyticsView
          to={to}
          rangeLabel={formatAnalyticsRangeLabel(from, to)}
          summaries={summaries}
          weightHistory={weightQuery.data ?? []}
          canGoNext={canGoNext}
          onPreviousRange={() => setRangeEnd((current) => shiftAnalyticsRangeEnd(current, -1, today))}
          onNextRange={() => setRangeEnd((current) => shiftAnalyticsRangeEnd(current, 1, today))}
        />
      )}
    </>
  )
}
