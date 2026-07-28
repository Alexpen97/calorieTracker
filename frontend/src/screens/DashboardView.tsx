import type { DaySummary, UserProfile, WeightLog } from '../api/client'
import { buildWeightTrendAxisLabels, buildWeightTrendSeries } from '../diary/nutritionDashboard'
import { Link } from 'react-router-dom'
import { DashboardCard, MetricCard } from '../ui/Card'
import { CalorieHeroRing, WeightTrendChart } from '../ui/MiniCharts'
import { IconScale } from '../ui/Icons'

type Props = {
  me: Pick<UserProfile, 'displayName' | 'avatarUrl'> | null
  summary: DaySummary
  weightHistory: WeightLog[]
}

const MACRO_CARDS = [
  { code: 'protein', label: 'Protein', tone: 'protein' as const },
  { code: 'carbohydrates', label: 'Carbs', tone: 'carbs' as const },
  { code: 'fat', label: 'Fat', tone: 'fat' as const },
]

export default function DashboardView({ me, summary, weightHistory }: Props) {
  const name = me?.displayName?.trim() || 'there'
  const energy = summary.totals.find((item) => item.code === 'energy_kcal')
  const remaining =
    energy?.target != null ? Math.max(0, energy.target - energy.amount) : null
  const caloriePercent = energy?.target
    ? Math.min(100, Math.round((energy.amount / energy.target) * 100))
    : 0
  const weekDays = buildWeekStrip(summary.date)

  return (
    <main className="mobile-page dashboard-page mockup-dashboard">
      <header className="screen-header dashboard-span">
        <div>
          <h1>Good morning, {name}</h1>
          <p>Stay on track today</p>
        </div>
        <div className="avatar" aria-label="Profile">
          {me?.avatarUrl ? <img src={me.avatarUrl} alt="" /> : <span aria-hidden>NT</span>}
        </div>
      </header>

      <div className="day-strip dashboard-span" aria-label="This week">
        {weekDays.map((day) => (
          <div
            key={day.iso}
            className={`day-strip-item${day.active ? ' is-active' : ''}`}
            aria-current={day.active ? 'date' : undefined}
          >
            <span className="day-strip-weekday">{day.weekday}</span>
            <span className="day-strip-date">{day.day}</span>
          </div>
        ))}
      </div>

      <DashboardCard className="dashboard-span" density="hero" title="Today" eyebrow="Energy">
        <CalorieHeroRing
          valueLabel={
            remaining != null
              ? formatNumber(remaining)
              : formatNumber(energy?.amount ?? 0)
          }
          detailLabel={remaining != null ? 'Calories left' : 'Calories logged'}
          percent={caloriePercent}
        />
      </DashboardCard>

      <div className="macro-card-grid dashboard-span" aria-label="Macros left">
        {MACRO_CARDS.map((macro) => {
          const total = summary.totals.find((item) => item.code === macro.code)
          const amount = total?.amount ?? 0
          const target = total?.target ?? null
          const left = target != null ? Math.max(0, target - amount) : null
          const percent = target ? Math.min(100, Math.round((amount / target) * 100)) : 0
          return (
            <MetricCard
              key={macro.code}
              label={`${macro.label} left`}
              value={
                left != null
                  ? `${formatNumber(left)}g`
                  : `${formatNumber(amount)}g`
              }
              tone={macro.tone}
              progress={percent}
            />
          )
        })}
      </div>

      <DashboardCard
        className="dashboard-span"
        density="insight"
        icon={<IconScale />}
        title="Weight"
        eyebrow="Trend"
        action={
          <Link className="card-action" to="/settings/weight">
            Log
          </Link>
        }
      >
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

function buildWeekStrip(activeIso: string) {
  const active = parseIsoDate(activeIso)
  const weekday = active.getDay()
  const mondayOffset = weekday === 0 ? -6 : 1 - weekday
  const monday = new Date(active)
  monday.setDate(active.getDate() + mondayOffset)

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(monday)
    date.setDate(monday.getDate() + index)
    const iso = formatIso(date)
    return {
      iso,
      day: String(date.getDate()),
      weekday: date.toLocaleDateString(undefined, { weekday: 'short' }).slice(0, 2),
      active: iso === activeIso,
    }
  })
}

function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function formatIso(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function latestWeight(weights: WeightLog[]): string {
  if (weights.length === 0) return '—'
  const latest = [...weights].sort(
    (a, b) => new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime(),
  )[0]
  return `${formatNumber(latest.weightKg)} kg`
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
}
