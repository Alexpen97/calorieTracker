import type { DaySummary, Goal, NutrientTotal } from '../api/client'

const WATER_CODE = 'water_ml'

/** Fill null diary summary targets from /users/me/goals when diary omitted them. */
export function mergeSummaryWithGoals(
  summary: DaySummary,
  goals: Goal[] | undefined,
): DaySummary {
  if (!goals || goals.length === 0) {
    return summary
  }

  const targets = new Map(
    goals
      .filter((goal) => goal.nutrientCode !== WATER_CODE)
      .map((goal) => [goal.nutrientCode, goal] as const),
  )
  const waterGoal = goals.find((goal) => goal.nutrientCode === WATER_CODE)

  const totalsByCode = new Map(
    summary.totals.map((total) => [
      total.code,
      {
        ...total,
        target: total.target ?? targets.get(total.code)?.dailyTarget ?? null,
      } satisfies NutrientTotal,
    ]),
  )

  for (const [code, goal] of targets) {
    if (!totalsByCode.has(code)) {
      totalsByCode.set(code, {
        code,
        amount: 0,
        unit: goal.unit,
        target: goal.dailyTarget,
      })
    }
  }

  return {
    ...summary,
    totals: [...totalsByCode.values()].sort((left, right) => left.code.localeCompare(right.code)),
    water: {
      ...summary.water,
      targetMl: summary.water.targetMl ?? waterGoal?.dailyTarget ?? null,
    },
  }
}
