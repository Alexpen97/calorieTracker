import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchMe,
  updateMe,
  type ActivityLevel,
  type Objective,
  type Sex,
  type UpdateMeInput,
  type UserProfile,
} from '../../api/client'
import { activityOptions, objectiveOptions, SettingsSectionShell, sexOptions } from './SettingsSectionShell'

type ProfileForm = {
  displayName: string
  sex: Sex | ''
  birthDate: string
  heightCm: string
  activityLevel: ActivityLevel | ''
  objective: Objective | ''
}

export default function SettingsProfileSection() {
  const queryClient = useQueryClient()
  const [profileForm, setProfileForm] = useState<ProfileForm>(emptyProfileForm)
  const [profileMessage, setProfileMessage] = useState<string | null>(null)

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
  })

  useEffect(() => {
    if (meQuery.data) {
      setProfileForm(profileToForm(meQuery.data))
    }
  }, [meQuery.data])

  const saveProfile = useMutation({
    mutationFn: updateMe,
    onSuccess: async (profile) => {
      setProfileMessage('Profile saved.')
      queryClient.setQueryData(['me'], profile)
      await queryClient.invalidateQueries({ queryKey: ['me'] })
    },
  })

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

  return (
    <SettingsSectionShell
      title="Profile"
      description="Update your personal details used for goals and tracking."
    >
      {meQuery.isLoading && <p>Loading…</p>}
      {meQuery.error && <p className="error">{(meQuery.error as Error).message}</p>}
      {saveProfile.error && <p className="error">{(saveProfile.error as Error).message}</p>}

      <section className="dashboard-card">
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
    </SettingsSectionShell>
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
