import { useQuery } from '@tanstack/react-query'
import {
  fetchDiaryEntries,
  fetchDiarySummary,
  fetchMe,
  fetchWeightHistory,
} from '../api/client'
import { formatLocalDate } from '../diary/formatDay'
import DashboardView from '../screens/DashboardView'

export default function DashboardPage() {
  const today = formatLocalDate()
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })
  const summaryQuery = useQuery({
    queryKey: ['diary-summary', today],
    queryFn: () => fetchDiarySummary(today),
  })
  const entriesQuery = useQuery({
    queryKey: ['diary-entries', today],
    queryFn: () => fetchDiaryEntries(today),
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  return (
    <>
      {(meQuery.isLoading || summaryQuery.isLoading || entriesQuery.isLoading || weightQuery.isLoading) && (
        <p className="mobile-page">Loading dashboard…</p>
      )}
      {[meQuery.error, summaryQuery.error, entriesQuery.error, weightQuery.error]
        .filter(Boolean)
        .map((error, index) => (
          <p className="error mobile-page" key={index}>
            {(error as Error).message}
          </p>
        ))}
      {summaryQuery.data && (
        <DashboardView
          me={meQuery.data ?? null}
          summary={summaryQuery.data}
          mealCount={(entriesQuery.data ?? []).length}
          weightHistory={weightQuery.data ?? []}
        />
      )}
    </>
  )
}
