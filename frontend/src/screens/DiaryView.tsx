import type { DaySummary, DiaryEntry, WaterLog, WeightLog } from '../api/client'
import { Link } from 'react-router-dom'
import { groupEntriesByMeal, getMacroProgress } from '../diary/formatDay'
import { buildMicronutrientRows, buildWeightTrend } from '../diary/nutritionDashboard'
import { DashboardCard } from '../ui/Card'
import { ProgressRow, Sparkline, StackedFoodBars } from '../ui/MiniCharts'
import { IconBook, IconFlame, IconPie, IconScale } from '../ui/Icons'

type Props = {
  selectedDateLabel: string
  summary: DaySummary
  entries: DiaryEntry[]
  waterLogs: WaterLog[]
  weightHistory: WeightLog[]
  onPreviousDay: () => void
  onNextDay: () => void
  onDeleteEntry: (id: string) => void
  onAddFoodHref?: string
}

export default function DiaryView({
  selectedDateLabel,
  summary,
  entries,
  waterLogs,
  weightHistory,
  onPreviousDay,
  onNextDay,
  onDeleteEntry,
  onAddFoodHref = '/lookup',
}: Props) {
  const energy = getMacroProgress(summary.totals, 'energy_kcal')
  const protein = getMacroProgress(summary.totals, 'protein')
  const carbs = getMacroProgress(summary.totals, 'carbohydrates')
  const fat = getMacroProgress(summary.totals, 'fat')
  const vitamins = buildMicronutrientRows(summary.totals, 'vitamin')
  const minerals = buildMicronutrientRows(summary.totals, 'mineral')
  const groups = groupEntriesByMeal(entries)
  const streak = waterLogs.length

  return (
    <main className="mobile-page diary-page mockup-diary">
      <header className="diary-header">
        <div>
          <h1>Food Diary</h1>
        </div>
        <div className="diary-streak" aria-label="Logs today">
          <IconFlame className="tab-icon" />
          <strong>{streak}</strong>
        </div>
      </header>

      <DashboardCard icon={<IconPie />} title="Food Summary" eyebrow="Eaten">
        <div className="diary-summary">
          <StackedFoodBars
            rows={[
              {
                label: 'Calories',
                percent: energy?.percent ?? 0,
                amountLabel: amountGoalLabel(energy?.amount ?? 0, energy?.target ?? null, ''),
              },
              {
                label: 'Protein',
                percent: protein?.percent ?? 0,
                amountLabel: amountGoalLabel(protein?.amount ?? 0, protein?.target ?? null, 'g'),
              },
              {
                label: 'Carbs',
                percent: carbs?.percent ?? 0,
                amountLabel: amountGoalLabel(carbs?.amount ?? 0, carbs?.target ?? null, 'g'),
              },
              {
                label: 'Fat',
                percent: fat?.percent ?? 0,
                amountLabel: amountGoalLabel(fat?.amount ?? 0, fat?.target ?? null, 'g'),
              },
            ]}
          />

          <Link className="add-food-tile" to={onAddFoodHref}>
            <span className="add-food-plus" aria-hidden>
              +
            </span>
            <span>Add Food</span>
          </Link>
        </div>
      </DashboardCard>

      <div className="diary-day-nav" data-testid="diary-day-nav">
        <button
          className="btn btn-secondary btn-small diary-day-nav-btn"
          type="button"
          aria-label="Previous day"
          onClick={onPreviousDay}
        >
          ‹
        </button>
        <p className="diary-day-nav-label">{selectedDateLabel}</p>
        <button
          className="btn btn-secondary btn-small diary-day-nav-btn"
          type="button"
          aria-label="Next day"
          onClick={onNextDay}
        >
          ›
        </button>
      </div>

      <DashboardCard icon={<IconBook />} title="Meals" eyebrow="Timeline">
        <div className="meal-timeline">
          {groups.map((group) => (
            <section className="meal-block" key={group.mealType}>
              <h2 className="meal-title">{mealLabel(group.mealType)}</h2>
              {group.entries.length === 0 ? (
                <p className="empty-copy">No entries yet.</p>
              ) : (
                <ul className="meal-entry-list">
                  {group.entries.map((entry) => (
                    <li className="meal-entry" key={entry.id}>
                      <div>
                        <strong>{entry.productName}</strong>
                        <p className="product-meta">
                          {entry.brand ? `${entry.brand} · ` : ''}
                          {formatNumber(entry.weightG)} g
                        </p>
                        <div className="macro-chip-row">
                          {entryMacroChips(entry).map((chip) => (
                            <span className={`macro-chip macro-chip-${chip.tone}`} key={`${chip.key}-${chip.label}`}>
                              <span className="macro-chip-key">{chip.key}</span>
                              <span>{chip.label}</span>
                            </span>
                          ))}
                        </div>
                      </div>
                      <button className="btn btn-secondary btn-small" type="button" onClick={() => onDeleteEntry(entry.id)}>
                        Delete
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          ))}
        </div>
      </DashboardCard>

      <div className="grid-three">
        <DashboardCard icon={<IconScale />} title="Vitamin Checklist" eyebrow="Targets">
          <div className="compact-rows">
            {vitamins.slice(0, 4).map((row) => (
              <ProgressRow key={row.code} label={row.label} percent={row.percent} amountLabel={`${row.percent}%`} />
            ))}
          </div>
        </DashboardCard>
        <DashboardCard icon={<IconScale />} title="Mineral Checklist" eyebrow="Targets">
          <div className="compact-rows">
            {minerals.slice(0, 4).map((row) => (
              <ProgressRow key={row.code} label={row.label} percent={row.percent} amountLabel={`${row.percent}%`} />
            ))}
          </div>
        </DashboardCard>
        <DashboardCard icon={<IconScale />} title="Weight Check-in" eyebrow="Trend">
          <p className="weight-value">{latestWeight(weightHistory)}</p>
          <Sparkline label="Weight trend" points={buildWeightTrend(weightHistory)} />
        </DashboardCard>
      </div>
    </main>
  )
}

function amountGoalLabel(amount: number, target: number | null, unit: string): string {
  const amountText = formatNumber(amount)
  const unitSuffix = unit ? ` ${unit}` : ''
  if (target == null) return `${amountText}${unitSuffix}`.trim()
  return `${amountText} / ${formatNumber(target)}${unitSuffix}`
}

function mealLabel(mealType: string) {
  switch (mealType) {
    case 'BREAKFAST':
      return 'Breakfast'
    case 'LUNCH':
      return 'Lunch'
    case 'DINNER':
      return 'Dinner'
    case 'SNACK':
      return 'Snacks'
    default:
      return mealType
  }
}

function entryMacroChips(entry: DiaryEntry) {
  const energy = nutrientAmount(entry, 'energy_kcal')
  const protein = nutrientAmount(entry, 'protein')
  const carbs = nutrientAmount(entry, 'carbohydrates')
  const fat = nutrientAmount(entry, 'fat')
  return [
    { key: 'P', label: `${formatNumber(protein)}g`, tone: 'green' },
    { key: 'C', label: `${formatNumber(carbs)}g`, tone: 'amber' },
    { key: 'F', label: `${formatNumber(fat)}g`, tone: 'purple' },
    { key: 'Cal', label: `${formatNumber(energy)} cal`, tone: 'blue' },
  ]
}

function nutrientAmount(entry: DiaryEntry, code: string): number {
  const nutrient = entry.nutrients.find((item) => item.code === code)
  return nutrient?.amount ?? 0
}

function latestWeight(weights: WeightLog[]): string {
  if (weights.length === 0) return '—'
  const latest = [...weights].sort((a, b) => new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime())[0]
  return `${formatNumber(latest.weightKg)} kg`
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
}
