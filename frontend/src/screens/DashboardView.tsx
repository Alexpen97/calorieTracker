import type { DaySummary, UserProfile, WeightLog } from '../api/client'
import {
  buildCalorieDisplayState,
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrendAxisLabels,
  buildWeightTrendSeries,
} from '../diary/nutritionDashboard'
import { Link } from 'react-router-dom'
import { DashboardCard } from '../ui/Card'
import { MicroProgressGrid, NestedCalorieMacroRing, WeightTrendChart } from '../ui/MiniCharts'
import { IconFlame, IconLeaf, IconScale } from '../ui/Icons'

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
  const calorieDisplay = buildCalorieDisplayState(energy, summary.energyAdjustment)

  return (
    <main className="mobile-page dashboard-page mockup-dashboard">
      <header className="screen-header dashboard-span">
        <div>
          <h1>Good morning, {name}!</h1>
          <p>Let’s make today count</p>
        </div>
        <div className="avatar" aria-label="Profile">
          {me?.avatarUrl ? <img src={me.avatarUrl} alt="" /> : <span aria-hidden>🙂</span>}
        </div>
      </header>

      <DashboardCard className="dashboard-span" icon={<IconFlame />} title="Today Summary">
        <div className="summary-layout summary-layout-nutrients">
          <NestedCalorieMacroRing
            calorieLabel="Calories"
            caloriePercent={calorieDisplay.caloriePercent}
            calorieAmountLabel={calorieDisplay.amountLabel}
            adjustmentPercent={calorieDisplay.adjustmentPercent}
            burnedLabel={calorieDisplay.burnedLabel}
            macros={macros.map((macro) => ({
              label: macro.label,
              percent: macro.percent,
              amountLabel: macro.amountLabel,
            }))}
          />
        </div>
      </DashboardCard>

      <DashboardCard
        icon={<IconLeaf />}
        title="Vitamins"
        action={<Link className="card-action" to="/analytics">Details</Link>}
      >
        <MicroProgressGrid rows={vitamins} />
      </DashboardCard>

      <DashboardCard
        icon={<IconLeaf />}
        title="Minerals"
        action={<Link className="card-action" to="/analytics">Details</Link>}
      >
        <MicroProgressGrid rows={minerals} />
      </DashboardCard>

      <DashboardCard className="dashboard-span" icon={<IconScale />} title="Weight Progress" eyebrow="Trend">
        <div className="weight-layout">
          <div className="weight-layout-meta">
            <p className="weight-value">{latestWeight(weightHistory)}</p>
            <p className="weight-sub">Last 30 days</p>
          </div>
          <div className="weight-chart">
            <WeightTrendChart
              label="Weight trend"
              points={buildWeightTrendSeries(weightHistory)}
              xLabels={buildWeightTrendAxisLabels()}
            />
          </div>
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
