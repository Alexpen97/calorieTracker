import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  fetchDiaryEntries,
  fetchDiarySummary,
  fetchWater,
  fetchWeightHistory,
} from '../api/client'
import {
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrend,
} from '../diary/nutritionDashboard'
import { formatLocalDate, getMacroProgress, waterProgress } from '../diary/formatDay'
import { DashboardCard, EmptyCard, MetricPill } from '../ui/Card'
import { GroupedBars, ProgressRing, ProgressRow, Sparkline } from '../ui/MiniCharts'

export default function DashboardPage() {
  const today = formatLocalDate()
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

  const summary = summaryQuery.data
  const energy = summary ? getMacroProgress(summary.totals, 'energy_kcal') : null
  const water = summary ? waterProgress(summary.water) : null
  const macros = summary ? buildMacroSummaries(summary.totals) : []
  const vitamins = summary ? buildMicronutrientRows(summary.totals, 'vitamin') : []
  const minerals = summary ? buildMicronutrientRows(summary.totals, 'mineral') : []
  const weightTrend = buildWeightTrend(weightQuery.data ?? [])
  const mealCount = entriesQuery.data?.length ?? 0

  return (
    <main className="mobile-page dashboard-page">
      <div className="mobile-hero">
        <p className="sheet-kicker">Dashboard</p>
        <h1>Today</h1>
        <p>{today}</p>
      </div>

      {(summaryQuery.isLoading ||
        entriesQuery.isLoading ||
        waterQuery.isLoading ||
        weightQuery.isLoading) && <p>Loading dashboard…</p>}
      {[summaryQuery.error, entriesQuery.error, waterQuery.error, weightQuery.error]
        .filter(Boolean)
        .map((error, index) => (
          <p className="error" key={index}>
            {(error as Error).message}
          </p>
        ))}

      {summary ? (
        <>
          <DashboardCard
            title="Daily summary"
            eyebrow="Calories"
            action={<Link to="/diary">Add food</Link>}
          >
            <div className="summary-card-grid">
              <ProgressRing
                label="Calories"
                percent={energy?.percent ?? 0}
                value={energy ? formatValue(energy.amount) : '0'}
              />
              <div className="summary-metrics">
                <MetricPill label="Meals" value={String(mealCount)} tone="green" />
                <MetricPill label="Water" value={water ? `${water.percent}%` : '0%'} tone="blue" />
              </div>
            </div>
          </DashboardCard>

          <DashboardCard title="Weight" eyebrow="Progress">
            {weightTrend.length > 0 ? (
              <Sparkline label="Weight trend" points={weightTrend} />
            ) : (
              <p className="empty-copy">Log weight from Profile to see your trend.</p>
            )}
          </DashboardCard>

          <DashboardCard title="Macros" eyebrow="Balance">
            <div className="macro-pill-grid">
              {macros.map((macro) => (
                <MetricPill
                  key={macro.code}
                  label={macro.label}
                  value={macro.amountLabel}
                  tone="green"
                />
              ))}
            </div>
          </DashboardCard>

          <DashboardCard title="Vitamins" eyebrow="Daily targets">
            {vitamins.length > 0 ? (
              vitamins.map((row) => (
                <ProgressRow
                  key={row.code}
                  label={row.label}
                  percent={row.percent}
                  amountLabel={row.amountLabel}
                />
              ))
            ) : (
              <p className="empty-copy">No vitamin targets yet.</p>
            )}
          </DashboardCard>

          <DashboardCard title="Minerals" eyebrow="Daily targets">
            {minerals.length > 0 ? (
              <GroupedBars label="Minerals" groups={minerals} />
            ) : (
              <p className="empty-copy">No mineral targets yet.</p>
            )}
          </DashboardCard>
        </>
      ) : (
        !summaryQuery.isLoading && (
          <EmptyCard title="No summary yet" copy="Add food to start your dashboard." />
        )
      )}
    </main>
  )
}

function formatValue(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
}
