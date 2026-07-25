import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchWeightHistory, logWeight } from '../../api/client'
import { SettingsSectionShell } from './SettingsSectionShell'

export default function SettingsWeightSection() {
  const queryClient = useQueryClient()
  const [weightKg, setWeightKg] = useState('')
  const [weightError, setWeightError] = useState<string | null>(null)

  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })

  const addWeight = useMutation({
    mutationFn: (input: { weightKg: number }) => logWeight(input),
    onSuccess: async () => {
      setWeightKg('')
      setWeightError(null)
      await queryClient.invalidateQueries({ queryKey: ['weight-history'] })
    },
  })

  function submitWeight(event: FormEvent) {
    event.preventDefault()
    const parsedWeight = Number(weightKg)
    if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
      setWeightError('Enter a positive kilogram amount.')
      return
    }
    addWeight.mutate({ weightKg: parsedWeight })
  }

  const weights = weightQuery.data ?? []

  return (
    <SettingsSectionShell title="Weight" description="Log weigh-ins and review recent history.">
      {weightQuery.isLoading && <p>Loading…</p>}
      {weightQuery.error && <p className="error">{(weightQuery.error as Error).message}</p>}
      {addWeight.error && <p className="error">{(addWeight.error as Error).message}</p>}

      <section className="dashboard-card">
        <form className="water-form" onSubmit={submitWeight}>
          <label htmlFor="profile-weight">Log weight</label>
          <input
            id="profile-weight"
            inputMode="decimal"
            min="1"
            onChange={(event) => setWeightKg(event.target.value)}
            placeholder="kg"
            type="number"
            value={weightKg}
          />
          <button className="btn btn-primary" disabled={addWeight.isPending} type="submit">
            {addWeight.isPending ? 'Logging…' : 'Log'}
          </button>
        </form>
        {weightError && <p className="error">{weightError}</p>}
        {weights.length === 0 ? (
          <p className="empty-copy">No weight entries yet.</p>
        ) : (
          <ul className="water-log-list">
            {weights.slice(0, 5).map((weight) => (
              <li key={weight.id}>
                <span>
                  {formatNumber(weight.weightKg)} kg · {formatDateTime(weight.measuredAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </SettingsSectionShell>
  )
}

function formatNumber(value: number) {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString([], {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}
