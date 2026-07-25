import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchGoals, overrideGoals, recalculateGoals, type Goal } from '../../api/client'
import { formatGoalLabel, needsProfileMessage } from '../../profile/goalDisplay'
import { SettingsSectionShell } from './SettingsSectionShell'

export default function SettingsGoalsSection() {
  const queryClient = useQueryClient()
  const [goalInputs, setGoalInputs] = useState<Record<string, string>>({})
  const [goalMessage, setGoalMessage] = useState<string | null>(null)
  const [recalculateMessage, setRecalculateMessage] = useState<string | null>(null)

  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
  })

  useEffect(() => {
    if (goalsQuery.data) {
      setGoalInputs(goalInputValues(goalsQuery.data))
    }
  }, [goalsQuery.data])

  const saveGoal = useMutation({
    mutationFn: overrideGoals,
    onSuccess: (goals) => {
      setGoalMessage('Goal saved.')
      queryClient.setQueryData(['goals'], goals)
      setGoalInputs(goalInputValues(goals))
    },
  })

  const recalculate = useMutation({
    mutationFn: () => recalculateGoals(true),
    onSuccess: (result) => {
      setRecalculateMessage(needsProfileMessage(result.needsProfile) ?? 'Goals recalculated.')
      queryClient.setQueryData(['goals'], result.current)
      setGoalInputs(goalInputValues(result.current))
    },
  })

  function submitGoal(event: FormEvent, goal: Goal) {
    event.preventDefault()
    setGoalMessage(null)
    const dailyTarget = Number(goalInputs[goal.nutrientCode])
    if (!Number.isFinite(dailyTarget) || dailyTarget <= 0) {
      setGoalMessage('Enter a positive target.')
      return
    }
    saveGoal.mutate({
      goals: [{ nutrientCode: goal.nutrientCode, dailyTarget, unit: goal.unit }],
    })
  }

  const goals = goalsQuery.data ?? []

  return (
    <SettingsSectionShell
      title="Daily goals"
      description="Review targets and apply custom overrides."
    >
      {goalsQuery.isLoading && <p>Loading…</p>}
      {goalsQuery.error && <p className="error">{(goalsQuery.error as Error).message}</p>}
      {saveGoal.error && <p className="error">{(saveGoal.error as Error).message}</p>}
      {recalculate.error && <p className="error">{(recalculate.error as Error).message}</p>}

      <section className="dashboard-card">
        <div className="progress-heading">
          <div>
            <p>Current targets and custom overrides.</p>
          </div>
          <button
            className="btn btn-secondary btn-small"
            disabled={recalculate.isPending}
            onClick={() => recalculate.mutate()}
            type="button"
          >
            {recalculate.isPending ? 'Recalculating…' : 'Recalculate'}
          </button>
        </div>
        {recalculateMessage && <p className="product-meta">{recalculateMessage}</p>}
        {goalMessage && <p className="product-meta">{goalMessage}</p>}
        {goals.length === 0 ? (
          <p className="empty-copy">No goals yet. Complete your profile and recalculate.</p>
        ) : (
          <ul className="goal-list">
            {goals.map((goal) => (
              <li key={goal.nutrientCode}>
                <form className="goal-row" onSubmit={(event) => submitGoal(event, goal)}>
                  <span>{formatGoalLabel(goal)}</span>
                  <label className="sr-only" htmlFor={`goal-${goal.nutrientCode}`}>
                    {goal.nutrientCode} target
                  </label>
                  <input
                    id={`goal-${goal.nutrientCode}`}
                    inputMode="decimal"
                    min="1"
                    onChange={(event) =>
                      setGoalInputs({ ...goalInputs, [goal.nutrientCode]: event.target.value })
                    }
                    type="number"
                    value={goalInputs[goal.nutrientCode] ?? ''}
                  />
                  <button className="btn btn-secondary btn-small" disabled={saveGoal.isPending} type="submit">
                    Save
                  </button>
                </form>
              </li>
            ))}
          </ul>
        )}
      </section>
    </SettingsSectionShell>
  )
}

function goalInputValues(goals: Goal[]): Record<string, string> {
  return Object.fromEntries(goals.map((goal) => [goal.nutrientCode, String(goal.dailyTarget)]))
}
