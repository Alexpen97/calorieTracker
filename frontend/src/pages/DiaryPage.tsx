import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  deleteDiaryEntry,
  fetchDiaryEntries,
  fetchDiarySummary,
  fetchGoals,
  fetchWater,
  fetchWeightHistory,
} from '../api/client'
import {
  formatDiaryDayLabel,
  formatLocalDate,
  shiftLocalDate,
} from '../diary/formatDay'
import { mergeSummaryWithGoals } from '../diary/mergeSummaryGoals'
import DiaryView from '../screens/DiaryView'

export default function DiaryPage() {
  const [selectedDate, setSelectedDate] = useState(() => formatLocalDate())
  const queryClient = useQueryClient()

  const summaryQuery = useQuery({
    queryKey: ['diary-summary', selectedDate],
    queryFn: () => fetchDiarySummary(selectedDate),
  })
  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
  })
  const entriesQuery = useQuery({
    queryKey: ['diary-entries', selectedDate],
    queryFn: () => fetchDiaryEntries(selectedDate),
  })
  const waterQuery = useQuery({
    queryKey: ['diary-water', selectedDate],
    queryFn: () => fetchWater(selectedDate),
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  const removeEntry = useMutation({
    mutationFn: deleteDiaryEntry,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['diary-summary', selectedDate] }),
        queryClient.invalidateQueries({ queryKey: ['diary-entries', selectedDate] }),
      ])
    },
  })

  const summary = summaryQuery.data
  const entries = entriesQuery.data ?? []
  const waterLogs = waterQuery.data ?? []
  const weights = weightQuery.data ?? []
  const errors = [
    summaryQuery.error,
    goalsQuery.error,
    entriesQuery.error,
    waterQuery.error,
    weightQuery.error,
    removeEntry.error,
  ].filter(Boolean)

  return (
    <main className="mobile-page diary-page">
      {(summaryQuery.isLoading || entriesQuery.isLoading || waterQuery.isLoading || weightQuery.isLoading) && (
        <p>Loading diary…</p>
      )}
      {errors.map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      {summary && (
        <DiaryView
          selectedDateLabel={formatDiaryDayLabel(selectedDate)}
          summary={mergeSummaryWithGoals(summary, goalsQuery.data)}
          entries={entries}
          waterLogs={waterLogs}
          weightHistory={weights}
          onPreviousDay={() => setSelectedDate((current) => shiftLocalDate(current, -1))}
          onNextDay={() => setSelectedDate((current) => shiftLocalDate(current, 1))}
          onDeleteEntry={(id) => removeEntry.mutate(id)}
        />
      )}
    </main>
  )
}
