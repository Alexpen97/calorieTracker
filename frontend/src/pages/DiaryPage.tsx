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

const macroCards: Array<{ code: string; label: string }> = [
  { code: 'protein', label: 'Protein' },
  { code: 'carbohydrates', label: 'Carbs' },
  { code: 'fat', label: 'Fat' },
]

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
    <main className="panel diary-panel">
      <div className="diary-header">
        <div>
          <p className="sheet-kicker">Today</p>
          <h2>Your diary</h2>
          <p className="product-meta">{today}</p>
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
        <>
          <section className="diary-card">
            <div className="progress-heading">
              <div>
                <h3>Energy</h3>
                <p>{energy ? progressLabel(energy.amount, energy.unit, energy.target) : 'No food logged yet'}</p>
              </div>
              {energy?.target && <strong>{energy.percent}%</strong>}
            </div>
            {energy?.target && <ProgressBar percent={energy.percent} />}
          </section>

          <section className="macro-grid" aria-label="Macro progress">
            {macroCards
              .map((macro) => ({
                ...macro,
                progress: getMacroProgress(summary.totals, macro.code),
              }))
              .filter((macro) => macro.progress)
              .map((macro) => (
                <div className="diary-card macro-card" key={macro.code}>
                  <div className="progress-heading">
                    <h3>{macro.label}</h3>
                    <span>
                      {progressLabel(
                        macro.progress!.amount,
                        macro.progress!.unit,
                        macro.progress!.target,
                      )}
                    </span>
                  </div>
                  {macro.progress!.target && <ProgressBar percent={macro.progress!.percent} />}
                </div>
              ))}
          </section>
        </>
      )}

      {water && (
        <section className="diary-card">
          <div className="progress-heading">
            <div>
              <h3>Water</h3>
              <p>{waterLabel(water.amountMl, water.targetMl)}</p>
            </div>
            {water.targetMl && <strong>{water.percent}%</strong>}
          </div>
          {water.targetMl && <ProgressBar percent={water.percent} />}
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
        </section>
      )}

      <section className="entries-section">
        <div className="diary-header">
          <h3>Meals</h3>
          <Link to="/lookup">Look up food</Link>
        </div>
        {mealGroups.map((group) => (
          <section className="meal-group" key={group.mealType}>
            <h4>{mealLabel(group.mealType)}</h4>
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
      </section>
    </main>
  )
}

function ProgressBar({ percent }: { percent: number }) {
  return (
    <div className="progress-track" aria-hidden>
      <div className="progress-fill" style={{ width: `${percent}%` }} />
    </div>
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
            {formatNumber(log.amountMl)} ml · {new Date(log.loggedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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

function progressLabel(amount: number, unit: string, target: number | null) {
  if (!target) {
    return `${formatNumber(amount)} ${unit}`
  }
  return `${formatNumber(amount)} / ${formatNumber(target)} ${unit}`
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
