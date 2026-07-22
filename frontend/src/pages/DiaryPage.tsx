import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  deleteDiaryEntry,
  deleteWater,
  fetchDiaryEntries,
  fetchDiarySummary,
  fetchWater,
  logWater,
  type DiaryEntry,
  type WaterLog,
} from '../api/client'
import {
  formatLocalDate,
  getMacroProgress,
  groupEntriesByMeal,
  waterProgress,
  type MealType,
} from '../diary/formatDay'
import { buildMacroSummaries, buildMicronutrientRows } from '../diary/nutritionDashboard'
import { DashboardCard, MetricPill } from '../ui/Card'
import { GroupedBars, ProgressRing, ProgressRow } from '../ui/MiniCharts'

export default function DiaryPage() {
  const today = formatLocalDate()
  const queryClient = useQueryClient()
  const [customWaterMl, setCustomWaterMl] = useState('')
  const [waterError, setWaterError] = useState<string | null>(null)

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

  const addWater = useMutation({
    mutationFn: (amountMl: number) => logWater({ amountMl }),
    onSuccess: async () => {
      setCustomWaterMl('')
      setWaterError(null)
      await invalidateDay(queryClient, today)
    },
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

  const removeWater = useMutation({
    mutationFn: deleteWater,
    onSuccess: async () => {
      await invalidateDay(queryClient, today)
    },
  })

  const summary = summaryQuery.data
  const entries = entriesQuery.data ?? []
  const waterLogs = waterQuery.data ?? []
  const energy = summary ? getMacroProgress(summary.totals, 'energy_kcal') : null
  const water = summary ? waterProgress(summary.water) : null
  const mealGroups = groupEntriesByMeal(entries)
  const macros = summary ? buildMacroSummaries(summary.totals) : []
  const vitamins = summary ? buildMicronutrientRows(summary.totals, 'vitamin') : []
  const minerals = summary ? buildMicronutrientRows(summary.totals, 'mineral') : []
  const errors = [
    summaryQuery.error,
    entriesQuery.error,
    waterQuery.error,
    addWater.error,
    removeEntry.error,
    removeWater.error,
  ].filter(Boolean)

  async function submitCustomWater(event: FormEvent) {
    event.preventDefault()
    const amountMl = Number(customWaterMl)
    if (!Number.isFinite(amountMl) || amountMl <= 0) {
      setWaterError('Enter a positive water amount.')
      return
    }
    addWater.mutate(amountMl)
  }

  return (
    <main className="mobile-page diary-page">
      <div className="mobile-hero diary-mobile-hero">
        <div>
          <p className="sheet-kicker">Today</p>
          <h1>Food Diary</h1>
          <p>{today}</p>
        </div>
        <Link className="btn btn-primary" to="/lookup">
          Add food
        </Link>
      </div>

      {(summaryQuery.isLoading || entriesQuery.isLoading || waterQuery.isLoading) && (
        <p>Loading today…</p>
      )}
      {errors.map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      {summary && (
        <DashboardCard title="Today summary" eyebrow="Logged nutrition">
          <div className="summary-card-grid">
            <ProgressRing
              label="Calories"
              percent={energy?.percent ?? 0}
              value={energy ? formatNumber(energy.amount) : '0'}
            />
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
          </div>
        </DashboardCard>
      )}

      {water && (
        <DashboardCard title="Water" eyebrow="Hydration">
          <div className="progress-heading">
            <p>{waterLabel(water.amountMl, water.targetMl)}</p>
            {water.targetMl && <strong>{water.percent}%</strong>}
          </div>
          {water.targetMl && (
            <div className="progress-track" aria-hidden>
              <div className="progress-fill" style={{ width: `${water.percent}%` }} />
            </div>
          )}
          <div className="water-actions">
            {[250, 500].map((amountMl) => (
              <button
                className="btn btn-secondary"
                disabled={addWater.isPending}
                key={amountMl}
                onClick={() => addWater.mutate(amountMl)}
                type="button"
              >
                +{amountMl} ml
              </button>
            ))}
          </div>
          <form className="water-form" onSubmit={submitCustomWater}>
            <label htmlFor="custom-water">Custom water</label>
            <input
              id="custom-water"
              inputMode="decimal"
              min="1"
              onChange={(event) => setCustomWaterMl(event.target.value)}
              placeholder="ml"
              type="number"
              value={customWaterMl}
            />
            <button className="btn btn-primary" disabled={addWater.isPending} type="submit">
              Add
            </button>
          </form>
          {waterError && <p className="error">{waterError}</p>}
          {waterLogs.length > 0 && (
            <WaterLogList logs={waterLogs} onDelete={(id) => removeWater.mutate(id)} />
          )}
        </DashboardCard>
      )}

      <DashboardCard title="Meals" eyebrow="Timeline">
        {mealGroups.map((group) => (
          <section className="meal-card" key={group.mealType}>
            <h3>{mealLabel(group.mealType)}</h3>
            {group.entries.length === 0 ? (
              <p className="empty-copy">No entries yet.</p>
            ) : (
              <ul className="entry-list">
                {group.entries.map((entry) => (
                  <DiaryEntryRow
                    entry={entry}
                    key={entry.id}
                    onDelete={(id) => removeEntry.mutate(id)}
                  />
                ))}
              </ul>
            )}
          </section>
        ))}
      </DashboardCard>

      <DashboardCard title="Vitamins" eyebrow="Checklist">
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

      <DashboardCard title="Minerals" eyebrow="Checklist">
        {minerals.length > 0 ? (
          <GroupedBars label="Minerals" groups={minerals} />
        ) : (
          <p className="empty-copy">No mineral targets yet.</p>
        )}
      </DashboardCard>
    </main>
  )
}

function DiaryEntryRow({ entry, onDelete }: { entry: DiaryEntry; onDelete: (id: string) => void }) {
  const energy = entry.nutrients.find((nutrient) => nutrient.code === 'energy_kcal')
  return (
    <li className="entry-row">
      <div>
        <strong>{entry.productName}</strong>
        <p>
          {[entry.brand, `${formatNumber(entry.weightG)} g`, energy && `${formatNumber(energy.amount)} ${energy.unit}`]
            .filter(Boolean)
            .join(' · ')}
        </p>
      </div>
      <button className="btn btn-secondary btn-small" onClick={() => onDelete(entry.id)} type="button">
        Delete
      </button>
    </li>
  )
}

function WaterLogList({ logs, onDelete }: { logs: WaterLog[]; onDelete: (id: string) => void }) {
  return (
    <ul className="water-log-list">
      {logs.map((log) => (
        <li key={log.id}>
          <span>
            {formatNumber(log.amountMl)} ml ·{' '}
            {new Date(log.loggedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
          <button className="sheet-close" onClick={() => onDelete(log.id)} type="button">
            Delete
          </button>
        </li>
      ))}
    </ul>
  )
}

async function invalidateDay(queryClient: QueryClient, today: string) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['diary-summary', today] }),
    queryClient.invalidateQueries({ queryKey: ['diary-water', today] }),
  ])
}

function waterLabel(amountMl: number, targetMl: number | null) {
  if (!targetMl) {
    return `${formatNumber(amountMl)} ml`
  }
  return `${formatNumber(amountMl)} / ${formatNumber(targetMl)} ml`
}

function mealLabel(mealType: MealType) {
  return mealType
    .toLowerCase()
    .split('_')
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(' ')
}

function formatNumber(value: number) {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}
