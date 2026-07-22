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

type ObjectiveOption = {
  value: Objective
  kicker: string
  label: string
  hint: string
  macro: string
}

const objectiveOptions: ObjectiveOption[] = [
  {
    value: 'CUT',
    kicker: 'Deficit',
    label: 'Cut',
    hint: 'A more focused fat-loss phase with higher protein.',
    macro: 'High protein · lower calories',
  },
  {
    value: 'LOSE',
    kicker: 'Steady',
    label: 'Lose weight',
    hint: 'A moderate deficit for gradual weight loss.',
    macro: 'Protein-forward · balanced fats',
  },
  {
    value: 'MAINTAIN',
    kicker: 'Baseline',
    label: 'Maintain',
    hint: 'Hold your current weight with balanced macros.',
    macro: 'Balanced protein, carbs, and fat',
  },
  {
    value: 'GAIN',
    kicker: 'Gentle',
    label: 'Gentle gain',
    hint: 'A smaller surplus for scale weight increases.',
    macro: 'Moderate protein · steady surplus',
  },
  {
    value: 'MUSCLE_GAIN',
    kicker: 'Training',
    label: 'Lean muscle',
    hint: 'A lean surplus with extra protein for strength work.',
    macro: 'Higher protein · controlled fats',
  },
  {
    value: 'BULK',
    kicker: 'Surplus',
    label: 'Bulk',
    hint: 'A larger surplus with more carbs to support training.',
    macro: 'High energy · carb-supportive',
  },
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
                Tell us your weight, height, and training focus so we can calculate daily nutrient targets.
              </p>
            </>
          )}
          {step === 'goal' && (
            <>
              <h2>Choose your goal</h2>
              <p className="product-meta">
                Pick the phase that matches how you want calories and macros to behave.
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

      <div className="onboarding-progress" aria-hidden>
        {(['body', 'goal', 'results'] as Step[]).map((item) => (
          <span className={step === item ? 'is-active' : ''} key={item} />
        ))}
      </div>

      {step === 'body' && (
        <form className="dashboard-card profile-form onboarding-card" onSubmit={continueFromBody}>
          <div className="onboarding-card-intro">
            <p className="card-eyebrow">Body basics</p>
            <h3>Start with your current stats</h3>
            <p>These numbers anchor the calorie estimate and water target.</p>
          </div>

          <div className="onboarding-input-grid">
            <label htmlFor="onboarding-height">
              Height (cm)
              <input
                id="onboarding-height"
                inputMode="decimal"
                min="1"
                onChange={(event) => setForm({ ...form, heightCm: event.target.value })}
                required
                type="number"
                value={form.heightCm}
              />
            </label>

            <label htmlFor="onboarding-weight">
              Weight (kg)
              <input
                id="onboarding-weight"
                inputMode="decimal"
                min="1"
                onChange={(event) => setForm({ ...form, weightKg: event.target.value })}
                required
                type="number"
                value={form.weightKg}
              />
            </label>
          </div>

          <div className="cta-row profile-actions">
            <button className="btn btn-primary" type="submit">
              Continue
            </button>
          </div>
        </form>
      )}

      {step === 'goal' && (
        <form className="dashboard-card profile-form onboarding-card" onSubmit={calculateGoals}>
          <fieldset className="goal-card-fieldset">
            <legend>Goal style</legend>
            <div className="goal-card-grid">
              {objectiveOptions.map((option) => (
                <label
                  className={`goal-choice-card ${form.objective === option.value ? 'is-selected' : ''}`}
                  key={option.value}
                >
                  <input
                    checked={form.objective === option.value}
                    name="objective"
                    onChange={() => setForm({ ...form, objective: option.value })}
                    required
                    type="radio"
                    value={option.value}
                  />
                  <span className="goal-choice-topline">
                    <span className="goal-choice-kicker">{option.kicker}</span>
                    {form.objective === option.value && (
                      <span className="goal-choice-selected">Selected</span>
                    )}
                  </span>
                  <strong>{option.label}</strong>
                  <span>{option.hint}</span>
                  <em>{option.macro}</em>
                </label>
              ))}
            </div>
          </fieldset>

          <div className="onboarding-input-grid">
            <label htmlFor="onboarding-activity">
              Activity level
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
            </label>

            <label htmlFor="onboarding-sex">
              Sex
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
            </label>

            <label htmlFor="onboarding-birth-date">
              Birth date
              <input
                id="onboarding-birth-date"
                onChange={(event) => setForm({ ...form, birthDate: event.target.value })}
                required
                type="date"
                value={form.birthDate}
              />
            </label>
          </div>

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
        <section className="dashboard-card onboarding-card">
          <div className="onboarding-card-intro">
            <p className="card-eyebrow">{selectedObjective(form.objective)?.label ?? 'Goals ready'}</p>
            <h3>Your first daily targets are set</h3>
            <p>We saved the profile details, logged your starting weight, and applied computed goals.</p>
          </div>
          <ul className="onboarding-summary-grid">
            {highlightGoals(result.goals)
              .filter((goal) => ['energy_kcal', 'protein', 'carbohydrates', 'fat'].includes(goal.nutrientCode))
              .map((goal) => (
                <li key={goal.nutrientCode}>
                  <span>{humanizeCode(goal.nutrientCode)}</span>
                  <strong>{formatGoalValue(goal)}</strong>
                </li>
              ))}
          </ul>
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

function selectedObjective(objective: Objective | ''): ObjectiveOption | undefined {
  return objectiveOptions.find((option) => option.value === objective)
}

function formatGoalValue(goal: Goal): string {
  const target = new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(
    goal.dailyTarget,
  )
  return `${target} ${goal.unit}`
}

function humanizeCode(code: string): string {
  const words = code.replaceAll('_', ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}
