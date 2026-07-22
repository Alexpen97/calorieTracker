import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  completeOnboarding,
  type ActivityLevel,
  type Goal,
  type Objective,
  type OnboardingResult,
  type Sex,
} from '../api/client'
import { formatGoalLabel } from '../profile/goalDisplay'

type Step = 'body' | 'goal' | 'results'

type FormState = {
  heightCm: string
  weightKg: string
  objective: Objective | ''
  activityLevel: ActivityLevel | ''
  sex: Sex | ''
  birthDate: string
}

const objectiveOptions: Array<{ value: Objective; label: string; hint: string }> = [
  { value: 'LOSE', label: 'Lose weight', hint: 'Slight calorie deficit' },
  { value: 'MAINTAIN', label: 'Maintain', hint: 'Hold your current weight' },
  { value: 'GAIN', label: 'Gain weight', hint: 'Slight calorie surplus' },
]

const activityOptions: Array<{ value: ActivityLevel; label: string }> = [
  { value: 'SEDENTARY', label: 'Sedentary' },
  { value: 'LIGHT', label: 'Light' },
  { value: 'MODERATE', label: 'Moderate' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'VERY_ACTIVE', label: 'Very active' },
]

const sexOptions: Array<{ value: Sex; label: string }> = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
]

export default function OnboardingPage() {
  const queryClient = useQueryClient()
  const [step, setStep] = useState<Step>('body')
  const [form, setForm] = useState<FormState>({
    heightCm: '',
    weightKg: '',
    objective: '',
    activityLevel: '',
    sex: '',
    birthDate: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<OnboardingResult | null>(null)

  const submit = useMutation({
    mutationFn: completeOnboarding,
    onSuccess: async (payload) => {
      setResult(payload)
      setStep('results')
      queryClient.setQueryData(['me'], payload.profile)
      queryClient.setQueryData(['goals'], payload.goals)
      // Keep weight-history stale until the user leaves results, so route guards
      // do not redirect away from the calculated goals screen.
      await queryClient.invalidateQueries({ queryKey: ['me'] })
      await queryClient.invalidateQueries({ queryKey: ['goals'] })
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  async function goToDashboard() {
    queryClient.setQueryData(['weight-history'], result ? [result.weight] : [])
    await queryClient.invalidateQueries({ queryKey: ['weight-history'] })
  }

  function continueFromBody(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const heightCm = Number(form.heightCm)
    const weightKg = Number(form.weightKg)
    if (!Number.isFinite(heightCm) || heightCm <= 0) {
      setError('Enter a positive height in centimeters.')
      return
    }
    if (!Number.isFinite(weightKg) || weightKg <= 0) {
      setError('Enter a positive weight in kilograms.')
      return
    }
    setStep('goal')
  }

  function calculateGoals(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (!form.objective || !form.activityLevel || !form.sex || !form.birthDate) {
      setError('Choose a dieting goal, activity level, sex, and birth date.')
      return
    }
    submit.mutate({
      heightCm: Number(form.heightCm),
      weightKg: Number(form.weightKg),
      objective: form.objective,
      activityLevel: form.activityLevel,
      sex: form.sex,
      birthDate: form.birthDate,
    })
  }

  return (
    <main className="mobile-page onboarding-page">
      <div className="diary-header">
        <div>
          <p className="sheet-kicker">Welcome</p>
          {step === 'body' && (
            <>
              <h2>Set up NutriTrack</h2>
              <p className="product-meta">
                Tell us your weight, height, and diet goal so we can calculate daily nutrient targets.
              </p>
            </>
          )}
          {step === 'goal' && (
            <>
              <h2>Your diet goal</h2>
              <p className="product-meta">
                Pick your current dieting goal. Sex, birth date, and activity fine-tune the calorie math.
              </p>
            </>
          )}
          {step === 'results' && (
            <>
              <h2>Your nutrient goals</h2>
              <p className="product-meta">Based on Mifflin-St Jeor energy needs plus nutrient reference intakes.</p>
            </>
          )}
        </div>
        <p className="onboarding-step" aria-live="polite">
          Step {step === 'body' ? 1 : step === 'goal' ? 2 : 3} of 3
        </p>
      </div>

      {error && <p className="error">{error}</p>}

      {step === 'body' && (
        <form className="dashboard-card profile-form" onSubmit={continueFromBody}>
          <label htmlFor="onboarding-height">Height (cm)</label>
          <input
            id="onboarding-height"
            inputMode="decimal"
            min="1"
            onChange={(event) => setForm({ ...form, heightCm: event.target.value })}
            required
            type="number"
            value={form.heightCm}
          />

          <label htmlFor="onboarding-weight">Weight (kg)</label>
          <input
            id="onboarding-weight"
            inputMode="decimal"
            min="1"
            onChange={(event) => setForm({ ...form, weightKg: event.target.value })}
            required
            type="number"
            value={form.weightKg}
          />

          <div className="cta-row profile-actions">
            <button className="btn btn-primary" type="submit">
              Continue
            </button>
          </div>
        </form>
      )}

      {step === 'goal' && (
        <form className="dashboard-card profile-form" onSubmit={calculateGoals}>
          <label htmlFor="onboarding-objective">Dieting goal</label>
          <select
            id="onboarding-objective"
            onChange={(event) =>
              setForm({ ...form, objective: event.target.value as Objective | '' })
            }
            required
            value={form.objective}
          >
            <option value="">Select goal</option>
            {objectiveOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label} — {option.hint}
              </option>
            ))}
          </select>

          <label htmlFor="onboarding-activity">Activity level</label>
          <select
            id="onboarding-activity"
            onChange={(event) =>
              setForm({ ...form, activityLevel: event.target.value as ActivityLevel | '' })
            }
            required
            value={form.activityLevel}
          >
            <option value="">Select activity</option>
            {activityOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <label htmlFor="onboarding-sex">Sex</label>
          <select
            id="onboarding-sex"
            onChange={(event) => setForm({ ...form, sex: event.target.value as Sex | '' })}
            required
            value={form.sex}
          >
            <option value="">Select sex</option>
            {sexOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <label htmlFor="onboarding-birth-date">Birth date</label>
          <input
            id="onboarding-birth-date"
            onChange={(event) => setForm({ ...form, birthDate: event.target.value })}
            required
            type="date"
            value={form.birthDate}
          />

          <div className="cta-row profile-actions">
            <button
              className="btn btn-secondary"
              onClick={() => {
                setError(null)
                setStep('body')
              }}
              type="button"
            >
              Back
            </button>
            <button className="btn btn-primary" disabled={submit.isPending} type="submit">
              {submit.isPending ? 'Calculating…' : 'Calculate goals'}
            </button>
          </div>
        </form>
      )}

      {step === 'results' && result && (
        <section className="dashboard-card">
          <ul className="goal-list onboarding-goal-list">
            {highlightGoals(result.goals).map((goal) => (
              <li key={goal.nutrientCode}>
                <span>{formatGoalLabel(goal)}</span>
              </li>
            ))}
          </ul>
          <div className="cta-row profile-actions">
            <Link className="btn btn-primary" onClick={() => void goToDashboard()} to="/today">
              Go to dashboard
            </Link>
          </div>
        </section>
      )}
    </main>
  )
}

function highlightGoals(goals: Goal[]): Goal[] {
  const preferred = ['energy_kcal', 'protein', 'water_ml', 'fiber', 'carbohydrates', 'fat']
  const ranked = [...goals].sort((left, right) => {
    const leftRank = preferred.indexOf(left.nutrientCode)
    const rightRank = preferred.indexOf(right.nutrientCode)
    const leftScore = leftRank === -1 ? preferred.length : leftRank
    const rightScore = rightRank === -1 ? preferred.length : rightRank
    return leftScore - rightScore
  })
  return ranked.slice(0, 8)
}
