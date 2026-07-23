import type { DaySummary, WeightLog } from '../api/client'
import { DashboardCard, MetricPill } from '../ui/Card'
import { MicroProgressGrid, Sparkline, StackedBar } from '../ui/MiniCharts'
import {
  averageMicronutrientRows,
  buildMacroSummaries,
  buildWeightTrend,
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
  const vitamins = averageMicronutrientRows(summaries, 'vitamin')
  const minerals = averageMicronutrientRows(summaries, 'mineral')
  const lowVitamin = vitamins.find((item) => item.percent > 0 && item.percent < 60)
  const proteinPercent = macros.find((item) => item.code === 'protein')?.percent ?? 0
  const calcium = minerals.find((item) => item.code === 'calcium')

  return (
    <main className="mobile-page analytics-page mockup-analytics">
      <header className="analytics-header">
        <h1>Analytics</h1>
        <div className="date-pill" aria-label="Date range">
          {from} – {to}
        </div>
      </header>

      <DashboardCard icon={<IconScale />} title="Weight trend" eyebrow="Goal range">
        <Sparkline label="Weight trend" points={buildWeightTrend(weightHistory)} />
        <p className="product-meta">You’re within your goal range. Great job!</p>
      </DashboardCard>

      <DashboardCard icon={<IconBars />} title="Macro balance" eyebrow="This week">
        <StackedBar
          label="Macro balance"
          segments={macros.map((macro) => ({ label: macro.label, percent: macro.percent }))}
        />
      </DashboardCard>

      <DashboardCard icon={<IconLeaf />} title="Vitamins" eyebrow="Intake (avg)">
        <MicroProgressGrid rows={vitamins} />
      </DashboardCard>

      <DashboardCard icon={<IconLeaf />} title="Minerals" eyebrow="Intake (avg)">
        <MicroProgressGrid rows={minerals} />
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

