import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  deleteDiaryEntry,
  fetchDiaryEntries,
  fetchDiarySummary,
  fetchWater,
  fetchWeightHistory,
} from '../api/client'
import {
  formatLocalDate,
} from '../diary/formatDay'
import DiaryView from '../screens/DiaryView'

export default function DiaryPage() {
  const today = formatLocalDate()
  const queryClient = useQueryClient()

  const summaryQuery = useQuery({
    queryKey: ['diary-summary', today],
    queryFn: () => fetchDiarySummary(today),
  })
  const entriesQuery = useQuery({
    queryKey: ['diary-entries', today],
    queryFn: () => fetchDiaryEntries(today),
  })
  const waterQuery = useQuery({
    queryKey: ['diary-water', today],
    queryFn: () => fetchWater(today),
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  const removeEntry = useMutation({
    mutationFn: deleteDiaryEntry,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['diary-summary', today] }),
        queryClient.invalidateQueries({ queryKey: ['diary-entries', today] }),
      ])
    },
  })

  const summary = summaryQuery.data
  const entries = entriesQuery.data ?? []
  const waterLogs = waterQuery.data ?? []
  const weights = weightQuery.data ?? []
  const errors = [
    summaryQuery.error,
    entriesQuery.error,
    waterQuery.error,
    weightQuery.error,
    removeEntry.error,
  ].filter(Boolean)

  return (
    <main className="mobile-page diary-page">
      {(summaryQuery.isLoading || entriesQuery.isLoading || waterQuery.isLoading || weightQuery.isLoading) && (
        <p>Loading today…</p>
      )}
      {errors.map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      {summary && (
        <DiaryView
          dateLabel={`Today, ${today}`}
          summary={summary}
          entries={entries}
          waterLogs={waterLogs}
          weightHistory={weights}
          onDeleteEntry={(id) => removeEntry.mutate(id)}
        />
      )}
    </main>
  )
}
