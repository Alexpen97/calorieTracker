import { useQuery } from '@tanstack/react-query'
import { fetchDiarySummaryRange, fetchWeightHistory } from '../api/client'
import {
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrend,
  dateDaysAgo,
} from '../diary/nutritionDashboard'
import { formatLocalDate } from '../diary/formatDay'
import { DashboardCard, MetricPill } from '../ui/Card'
import { GroupedBars, Sparkline, StackedBar } from '../ui/MiniCharts'

export default function AnalyticsPage() {
  const to = formatLocalDate()
  const from = dateDaysAgo(6)
  const rangeQuery = useQuery({
    queryKey: ['diary-summary-range', from, to],
    queryFn: () => fetchDiarySummaryRange(from, to),
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory({ from, to }),
  })

  const latest = rangeQuery.data?.at(-1)
  const macros = latest ? buildMacroSummaries(latest.totals) : []
  const vitamins = latest ? buildMicronutrientRows(latest.totals, 'vitamin') : []
  const minerals = latest ? buildMicronutrientRows(latest.totals, 'mineral') : []
  const lowVitamin = vitamins.find((item) => item.percent > 0 && item.percent < 60)
  const proteinPercent = macros.find((item) => item.code === 'protein')?.percent ?? 0

  return (
    <main className="mobile-page analytics-page">
      <div className="mobile-hero">
        <p className="sheet-kicker">This week</p>
        <h1>Analytics</h1>
        <p>
          {from} to {to}
        </p>
      </div>

      {(rangeQuery.isLoading || weightQuery.isLoading) && <p>Loading analytics…</p>}
      {[rangeQuery.error, weightQuery.error].filter(Boolean).map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      <DashboardCard title="Weight trend" eyebrow="Goal progress">
        <Sparkline label="Weight trend" points={buildWeightTrend(weightQuery.data ?? [])} />
      </DashboardCard>

      <DashboardCard title="Macros" eyebrow="Weekly balance">
        <StackedBar
          label="Macro balance"
          segments={macros.map((macro) => ({ label: macro.label, percent: macro.percent }))}
        />
      </DashboardCard>

      <DashboardCard title="Vitamins" eyebrow="Latest day">
        <GroupedBars label="Vitamins" groups={vitamins} />
      </DashboardCard>

      <DashboardCard title="Minerals" eyebrow="Latest day">
        <GroupedBars label="Minerals" groups={minerals} />
      </DashboardCard>

      <DashboardCard title="Insights" eyebrow="Signals">
        <div className="insight-grid">
          <MetricPill
            label="Protein"
            value={proteinPercent >= 80 ? 'On target' : 'Needs focus'}
            tone="green"
          />
          <MetricPill
            label={lowVitamin?.label ?? 'Vitamins'}
            value={lowVitamin ? 'Low' : 'Steady'}
            tone="amber"
          />
        </div>
      </DashboardCard>
    </main>
  )
}
