import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  fetchGoals,
  fetchMe,
  fetchWeightHistory,
  logWeight,
  overrideGoals,
  recalculateGoals,
  updateMe,
  type ActivityLevel,
  type Goal,
  type Objective,
  type Sex,
  type UpdateMeInput,
  type UserProfile,
} from '../api/client'
import { clearTokens } from '../auth/tokenStorage'
import { formatGoalLabel, needsProfileMessage } from '../profile/goalDisplay'

type ProfileForm = {
  displayName: string
  sex: Sex | ''
  birthDate: string
  heightCm: string
  activityLevel: ActivityLevel | ''
  objective: Objective | ''
}

const sexOptions: Array<{ value: Sex; label: string }> = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
]

const activityOptions: Array<{ value: ActivityLevel; label: string }> = [
  { value: 'SEDENTARY', label: 'Sedentary' },
  { value: 'LIGHT', label: 'Light' },
  { value: 'MODERATE', label: 'Moderate' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'VERY_ACTIVE', label: 'Very active' },
]

const objectiveOptions: Array<{ value: Objective; label: string }> = [
  { value: 'CUT', label: 'Cut' },
  { value: 'LOSE', label: 'Lose weight' },
  { value: 'MAINTAIN', label: 'Maintain' },
  { value: 'GAIN', label: 'Gentle gain' },
  { value: 'MUSCLE_GAIN', label: 'Lean muscle' },
  { value: 'BULK', label: 'Bulk' },
]

export default function ProfilePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [profileForm, setProfileForm] = useState<ProfileForm>(emptyProfileForm)
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [weightKg, setWeightKg] = useState('')
  const [weightError, setWeightError] = useState<string | null>(null)
  const [goalInputs, setGoalInputs] = useState<Record<string, string>>({})
  const [goalMessage, setGoalMessage] = useState<string | null>(null)
  const [recalculateMessage, setRecalculateMessage] = useState<string | null>(null)

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })
  const weightQuery = useQuery({
    queryKey: ['weight-history'],
    queryFn: () => fetchWeightHistory(),
  })
  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: fetchGoals,
  })

  useEffect(() => {
    if (meQuery.data) {
      setProfileForm(profileToForm(meQuery.data))
    }
  }, [meQuery.data])

  useEffect(() => {
    if (goalsQuery.data) {
      setGoalInputs(goalInputValues(goalsQuery.data))
    }
  }, [goalsQuery.data])

  const saveProfile = useMutation({
    mutationFn: updateMe,
    onSuccess: async (profile) => {
      setProfileMessage('Profile saved.')
      queryClient.setQueryData(['me'], profile)
      await queryClient.invalidateQueries({ queryKey: ['me'] })
    },
  })

  const addWeight = useMutation({
    mutationFn: (input: { weightKg: number }) => logWeight(input),
    onSuccess: async () => {
      setWeightKg('')
      setWeightError(null)
      await queryClient.invalidateQueries({ queryKey: ['weight-history'] })
    },
  })

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

  function logout() {
    void clearTokens().then(() => {
      navigate('/', { replace: true })
    })
  }

  function submitProfile(event: FormEvent) {
    event.preventDefault()
    setProfileMessage(null)
    const displayName = profileForm.displayName.trim()
    if (!displayName) {
      setProfileMessage('Display name is required.')
      return
    }
    const heightCm = parsePositiveOptional(profileForm.heightCm)
    if (heightCm === null) {
      setProfileMessage('Enter a positive height in centimeters.')
      return
    }
    const input: UpdateMeInput = {
      displayName,
      sex: emptyToUndefined(profileForm.sex),
      birthDate: profileForm.birthDate || undefined,
      heightCm,
      activityLevel: emptyToUndefined(profileForm.activityLevel),
      objective: emptyToUndefined(profileForm.objective),
    }
    saveProfile.mutate(input)
  }

  function submitWeight(event: FormEvent) {
    event.preventDefault()
    const parsedWeight = Number(weightKg)
    if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
      setWeightError('Enter a positive kilogram amount.')
      return
    }
    addWeight.mutate({ weightKg: parsedWeight })
  }

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

  const errors = [
    meQuery.error,
    weightQuery.error,
    goalsQuery.error,
    saveProfile.error,
    addWeight.error,
    saveGoal.error,
    recalculate.error,
  ].filter(Boolean)
  const weights = weightQuery.data ?? []
  const goals = goalsQuery.data ?? []

  return (
    <main className="mobile-page profile-panel">
      <div className="diary-header">
        <div>
          <p className="sheet-kicker">Account</p>
          <h2>Your profile</h2>
          <p className="product-meta">Manage profile details, weight, and daily goals.</p>
        </div>
      </div>

      {(meQuery.isLoading || weightQuery.isLoading || goalsQuery.isLoading) && <p>Loading…</p>}
      {errors.map((error, index) => (
        <p className="error" key={index}>
          {(error as Error).message}
        </p>
      ))}

      <section className="dashboard-card">
        <h3>Profile details</h3>
        {meQuery.data && (
          <dl className="meta compact-meta">
            <dt>Email</dt>
            <dd>{meQuery.data.email}</dd>
            <dt>Role</dt>
            <dd>{meQuery.data.role}</dd>
          </dl>
        )}
        <form className="lookup-form profile-form" onSubmit={submitProfile}>
          <label htmlFor="profile-name">Display name</label>
          <input
            id="profile-name"
            onChange={(event) => setProfileForm({ ...profileForm, displayName: event.target.value })}
            type="text"
            value={profileForm.displayName}
          />

          <label htmlFor="profile-sex">Sex</label>
          <select
            id="profile-sex"
            onChange={(event) => setProfileForm({ ...profileForm, sex: event.target.value as Sex | '' })}
            value={profileForm.sex}
          >
            <option value="">Select sex</option>
            {sexOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <label htmlFor="profile-birth-date">Birth date</label>
          <input
            id="profile-birth-date"
            onChange={(event) => setProfileForm({ ...profileForm, birthDate: event.target.value })}
            type="date"
            value={profileForm.birthDate}
          />

          <label htmlFor="profile-height">Height (cm)</label>
          <input
            id="profile-height"
            inputMode="decimal"
            min="1"
            onChange={(event) => setProfileForm({ ...profileForm, heightCm: event.target.value })}
            type="number"
            value={profileForm.heightCm}
          />

          <label htmlFor="profile-activity">Activity level</label>
          <select
            id="profile-activity"
            onChange={(event) =>
              setProfileForm({ ...profileForm, activityLevel: event.target.value as ActivityLevel | '' })
            }
            value={profileForm.activityLevel}
          >
            <option value="">Select activity</option>
            {activityOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <label htmlFor="profile-objective">Objective</label>
          <select
            id="profile-objective"
            onChange={(event) =>
              setProfileForm({ ...profileForm, objective: event.target.value as Objective | '' })
            }
            value={profileForm.objective}
          >
            <option value="">Select objective</option>
            {objectiveOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <div className="cta-row profile-actions">
            <button className="btn btn-primary" disabled={saveProfile.isPending} type="submit">
              {saveProfile.isPending ? 'Saving…' : 'Save profile'}
            </button>
          </div>
        </form>
        {profileMessage && <p className="product-meta">{profileMessage}</p>}
      </section>

      <section className="dashboard-card">
        <h3>Weight</h3>
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

      <section className="dashboard-card">
        <div className="progress-heading">
          <div>
            <h3>Daily goals</h3>
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

      <div className="cta-row" style={{ justifyContent: 'flex-start', marginTop: '1.5rem' }}>
        <button className="btn btn-primary" type="button" onClick={() => navigate('/lookup')}>
          Look up food
        </button>
        <button className="btn btn-secondary" type="button" onClick={logout}>
          Sign out
        </button>
      </div>
    </main>
  )
}

function emptyProfileForm(): ProfileForm {
  return {
    displayName: '',
    sex: '',
    birthDate: '',
    heightCm: '',
    activityLevel: '',
    objective: '',
  }
}

function profileToForm(profile: UserProfile): ProfileForm {
  return {
    displayName: profile.displayName,
    sex: profile.sex ?? '',
    birthDate: profile.birthDate ?? '',
    heightCm: profile.heightCm == null ? '' : String(profile.heightCm),
    activityLevel: profile.activityLevel ?? '',
    objective: profile.objective,
  }
}

function goalInputValues(goals: Goal[]): Record<string, string> {
  return Object.fromEntries(goals.map((goal) => [goal.nutrientCode, String(goal.dailyTarget)]))
}

function emptyToUndefined<T extends string>(value: T | ''): T | undefined {
  return value === '' ? undefined : value
}

function parsePositiveOptional(value: string): number | undefined | null {
  if (!value) {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return null
  }
  return parsed
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
