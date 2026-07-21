export type GoalForDisplay = {
  nutrientCode: string
  dailyTarget: number
  unit: string
  origin: string
  computedAt: string | null
}

export function formatGoalLabel(goal: GoalForDisplay): string {
  const label = humanizeCode(goal.nutrientCode)
  const target = new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(
    goal.dailyTarget,
  )
  return `${label}: ${target} ${goal.unit} (${originLabel(goal.origin)})`
}

export function needsProfileMessage(needsProfile: boolean): string | null {
  if (!needsProfile) {
    return null
  }
  return 'Complete sex, birth date, height, activity, objective, and weight to calculate goals.'
}

function humanizeCode(code: string): string {
  const words = code.replaceAll('_', ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function originLabel(origin: string): string {
  if (origin === 'USER_OVERRIDE') {
    return 'custom'
  }
  return 'computed'
}
