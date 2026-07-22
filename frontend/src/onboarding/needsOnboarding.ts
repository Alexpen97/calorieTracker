import type { UserProfile, WeightLog } from '../api/client'

/** True when the user still needs the first-run onboarding flow before goals can be computed. */
export function needsOnboarding(
  profile: Pick<UserProfile, 'sex' | 'birthDate' | 'heightCm' | 'activityLevel' | 'objective'>,
  weights: WeightLog[],
): boolean {
  return (
    profile.sex == null ||
    profile.birthDate == null ||
    profile.heightCm == null ||
    profile.activityLevel == null ||
    profile.objective == null ||
    weights.length === 0
  )
}
