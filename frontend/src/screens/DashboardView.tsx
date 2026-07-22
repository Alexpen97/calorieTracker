import type { DaySummary, UserProfile, WeightLog } from '../api/client'
import { buildMacroSummaries, buildMicronutrientRows, buildWeightTrend } from '../diary/nutritionDashboard'
import { Link } from 'react-router-dom'
import { DashboardCard, MetricPill } from '../ui/Card'
import { NestedCalorieMacroRing, ProgressRow, Sparkline } from '../ui/MiniCharts'
import { IconFlame, IconLeaf, IconPie, IconScale } from '../ui/Icons'

type Props = {
  me: Pick<UserProfile, 'displayName' | 'avatarUrl'> | null
  summary: DaySummary
  weightHistory: WeightLog[]
}

export default function DashboardView({ me, summary, weightHistory }: Props) {
  const name = me?.displayName?.trim() || 'there'
  const macros = buildMacroSummaries(summary.totals)
  const vitamins = buildMicronutrientRows(summary.totals, 'vitamin')
  const minerals = buildMicronutrientRows(summary.totals, 'mineral')
  const energy = summary.totals.find((item) => item.code === 'energy_kcal')
  const percent = energy?.target ? Math.min(100, Math.round((energy.amount / energy.target) * 100)) : 0

  return (
    <main className="mobile-page dashboard-page mockup-dashboard">
      <header className="screen-header">
        <div>
          <h1>Good morning, {name}!</h1>
          <p>Let’s make today count</p>
        </div>
        <div className="avatar" aria-label="Profile">
          {me?.avatarUrl ? <img src={me.avatarUrl} alt="" /> : <span aria-hidden>🙂</span>}
        </div>
      </header>

      <DashboardCard icon={<IconFlame />} title="Today Summary">
        <div className="summary-layout summary-layout-nutrients">
          <NestedCalorieMacroRing
            calorieLabel="Calories"
            caloriePercent={percent}
            calorieValue={formatNumber((energy?.target ?? 0) - (energy?.amount ?? 0))}
            macros={macros.map((macro) => ({
              label: macro.label,
              percent: macro.percent,
              amountLabel: macro.amountLabel,
            }))}
          />
        </div>
      </DashboardCard>

      <DashboardCard icon={<IconScale />} title="Weight Progress" eyebrow="Trend">
        <div className="weight-layout">
          <div>
            <p className="weight-value">{latestWeight(weightHistory)}</p>
            <p className="weight-sub">Last 2 weeks</p>
          </div>
          <div className="weight-chart">
            <Sparkline label="Weight trend" points={buildWeightTrend(weightHistory)} />
          </div>
        </div>
      </DashboardCard>

      <div className="grid-two">
        <DashboardCard icon={<IconPie />} title="Macros" action={<Link className="card-action" to="/analytics">Details</Link>}>
          <div className="macro-ring-row">
            {macros.map((macro) => (
              <MetricPill key={macro.code} label={macro.label} value={macro.amountLabel} tone="green" />
            ))}
          </div>
        </DashboardCard>
        <DashboardCard icon={<IconLeaf />} title="Vitamins" action={<Link className="card-action" to="/analytics">Details</Link>}>
          <div className="compact-rows">
            {vitamins.map((row) => (
              <ProgressRow key={row.code} label={row.label} percent={row.percent} amountLabel={`${row.percent}%`} />
            ))}
          </div>
        </DashboardCard>
      </div>

      <DashboardCard icon={<IconLeaf />} title="Minerals" action={<Link className="card-action" to="/analytics">Details</Link>}>
        <div className="minerals-row">
          {minerals.map((row) => (
            <div key={row.code} className="mineral-mini">
              <span>{row.label}</span>
              <div className="mini-track" aria-hidden>
                <div className="mini-fill" style={{ width: `${row.percent}%` }} />
              </div>
              <strong>{row.percent}%</strong>
            </div>
          ))}
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
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
}
