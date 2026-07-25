import type { DaySummary, WeightLog } from '../api/client'
import { DashboardCard, MetricPill } from '../ui/Card'
import { SharedMicronutrientTrendChart, StackedBar, WeightTrendChart } from '../ui/MiniCharts'
import {
  averageMicronutrientRows,
  buildMacroSummaries,
  buildMicronutrientTrendSeries,
  buildWeightTrendAxisLabels,
  buildWeightTrendSeries,
} from '../diary/nutritionDashboard'
import { IconBars, IconLeaf, IconScale } from '../ui/Icons'

type Props = {
  from: string
  to: string
  summaries: DaySummary[]
  weightHistory: WeightLog[]
}

export default function AnalyticsView({ from, to, summaries, weightHistory }: Props) {
  const latest = summaries.at(-1)
  const macros = latest ? buildMacroSummaries(latest.totals) : []
  const vitamins = buildMicronutrientTrendSeries(summaries, 'vitamin')
  const minerals = buildMicronutrientTrendSeries(summaries, 'mineral')
  const vitaminAverages = averageMicronutrientRows(summaries, 'vitamin')
  const mineralAverages = averageMicronutrientRows(summaries, 'mineral')
  const lowVitamin = vitaminAverages.find((item) => item.percent > 0 && item.percent < 60)
  const proteinPercent = macros.find((item) => item.code === 'protein')?.percent ?? 0
  const calcium = mineralAverages.find((item) => item.code === 'calcium')
  const weightPoints = buildWeightTrendSeries(weightHistory)

  return (
    <main className="mobile-page analytics-page mockup-analytics">
      <header className="analytics-header">
        <h1>Analytics</h1>
        <div className="date-pill" aria-label="Date range">
          {from} – {to}
        </div>
      </header>

      <DashboardCard className="dashboard-span" icon={<IconScale />} title="Weight trend" eyebrow="Last 30 days">
        <div className="weight-layout">
          <div className="weight-layout-meta">
            <p className="weight-value">{latestWeight(weightHistory)}</p>
            <p className="weight-sub">Logged weigh-ins</p>
          </div>
          <div className="weight-chart">
            <WeightTrendChart
              label="Weight trend"
              points={weightPoints}
              xLabels={buildWeightTrendAxisLabels()}
            />
          </div>
        </div>
      </DashboardCard>

      <DashboardCard icon={<IconLeaf />} title="Vitamins" eyebrow="Last 30 days · 0–150% RDI">
        <SharedMicronutrientTrendChart label="Vitamin trends, last 30 days" series={vitamins} />
      </DashboardCard>

      <DashboardCard icon={<IconLeaf />} title="Minerals" eyebrow="Last 30 days · 0–150% RDI">
        <SharedMicronutrientTrendChart label="Mineral trends, last 30 days" series={minerals} />
      </DashboardCard>

      <DashboardCard icon={<IconBars />} title="Macro balance" eyebrow="Latest day">
        <StackedBar
          label="Macro balance"
          segments={macros.map((macro) => ({ label: macro.label, percent: macro.percent }))}
        />
      </DashboardCard>

      <DashboardCard icon={<IconLeaf />} title="Insights" eyebrow="Signals">
        <div className="insight-grid">
          <MetricPill label="Protein on target" value={proteinPercent >= 80 ? 'Good' : 'Needs focus'} tone="green" />
          <MetricPill label={lowVitamin?.label ?? 'Vitamins'} value={lowVitamin ? 'Low' : 'Steady'} tone="amber" />
          <MetricPill
            label="Calcium"
            value={calcium && calcium.percent >= 80 ? 'On track' : calcium && calcium.percent > 0 ? 'Improving' : 'Needs focus'}
            tone="blue"
          />
        </div>
      </DashboardCard>
    </main>
  )
}

function latestWeight(weights: WeightLog[]): string {
  if (weights.length === 0) return '—'
  const latest = [...weights].sort((a, b) => new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime())[0]
  return `${formatNumber(latest.weightKg)} kg`
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}
