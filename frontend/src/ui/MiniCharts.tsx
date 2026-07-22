type Segment = { label: string; percent: number }
type ChartPoint = number

function clampPercent(value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

export function ProgressRing({
  label,
  percent,
  value,
}: {
  label: string
  percent: number
  value: string
}) {
  const bounded = clampPercent(percent)
  const strokePercent = bounded / 100
  const dashoffset = 100 * (1 - strokePercent)
  return (
    <div
      className="progress-ring"
      aria-label={`${label}: ${bounded}%`}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={bounded}
      aria-valuetext={`${bounded}%`}
    >
      <svg className="progress-ring-svg" viewBox="0 0 36 36" aria-hidden="true" focusable="false">
        <circle className="progress-ring-track" cx="18" cy="18" r="15.9155" />
        <circle
          className="progress-ring-indicator"
          cx="18"
          cy="18"
          r="15.9155"
          strokeDasharray={100}
          strokeDashoffset={dashoffset}
        />
      </svg>
      <div className="progress-ring-center" aria-hidden>
        <strong>{value}</strong>
        <span>{label}</span>
      </div>
    </div>
  )
}

const MACRO_RING_TONES = ['protein', 'carbs', 'fat'] as const

export function NestedCalorieMacroRing({
  calorieLabel,
  caloriePercent,
  calorieValue,
  macros,
}: {
  calorieLabel: string
  caloriePercent: number
  calorieValue: string
  macros: Segment[]
}) {
  const calorieBounded = clampPercent(caloriePercent)
  const calorieOffset = 100 * (1 - calorieBounded / 100)
  const macroCount = Math.max(macros.length, 1)
  const gap = 2.4
  const segmentSize = 100 / macroCount
  const usable = Math.max(segmentSize - gap, 0)

  return (
    <div className="nested-goal-ring">
      <div
        className="nested-goal-ring-chart"
        aria-label={`${calorieLabel}: ${calorieBounded}%`}
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={calorieBounded}
        aria-valuetext={`${calorieBounded}%`}
      >
        <svg className="nested-goal-ring-svg" viewBox="0 0 100 100" aria-hidden="true" focusable="false">
          {macros.map((macro, index) => {
            const bounded = clampPercent(macro.percent)
            const filled = (usable * bounded) / 100
            const offset = -(index * segmentSize)
            const tone = MACRO_RING_TONES[index % MACRO_RING_TONES.length]
            return (
              <g key={macro.label}>
                <circle
                  className="nested-macro-track"
                  data-testid="nested-macro-track"
                  cx="50"
                  cy="50"
                  r="42"
                  pathLength={100}
                  strokeDasharray={`${usable} ${100 - usable}`}
                  strokeDashoffset={offset}
                />
                <circle
                  className={`nested-macro-indicator nested-macro-indicator-${tone}`}
                  cx="50"
                  cy="50"
                  r="42"
                  pathLength={100}
                  strokeDasharray={`${filled} ${100 - filled}`}
                  strokeDashoffset={offset}
                />
              </g>
            )
          })}
          <circle
            className="nested-calorie-track"
            data-testid="nested-calorie-track"
            cx="50"
            cy="50"
            r="28"
            pathLength={100}
          />
          <circle
            className="nested-calorie-indicator"
            cx="50"
            cy="50"
            r="28"
            pathLength={100}
            strokeDasharray={100}
            strokeDashoffset={calorieOffset}
          />
        </svg>
        <div className="nested-goal-ring-center" aria-hidden>
          <strong>{calorieValue}</strong>
          <span>{calorieLabel}</span>
        </div>
      </div>
      <ul className="nested-macro-legend">
        {macros.map((macro, index) => {
          const bounded = clampPercent(macro.percent)
          const tone = MACRO_RING_TONES[index % MACRO_RING_TONES.length]
          return (
            <li
              key={macro.label}
              className={`nested-macro-legend-item nested-macro-legend-${tone}`}
              aria-label={`${macro.label}: ${bounded}%`}
            >
              <span className="nested-macro-swatch" aria-hidden />
              <span className="nested-macro-name">{macro.label}</span>
              <strong>{bounded}%</strong>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export function ProgressRow({
  label,
  percent,
  amountLabel,
}: {
  label: string
  percent: number
  amountLabel: string
}) {
  const bounded = clampPercent(percent)
  return (
    <div className="progress-row">
      <span>{label}</span>
      <div className="mini-track" aria-hidden>
        <div className="mini-fill" style={{ width: `${bounded}%` }} />
      </div>
      <strong>{amountLabel}</strong>
    </div>
  )
}

export function Sparkline({ label, points }: { label: string; points: ChartPoint[] }) {
  const path = sparklinePath(points)
  return (
    <svg className="sparkline" viewBox="0 0 100 36" role="img" aria-label={label}>
      <path d={path} />
    </svg>
  )
}

export function StackedBar({ label, segments }: { label: string; segments: Segment[] }) {
  return (
    <div className="stacked-bar">
      <p>{label}</p>
      <div className="stacked-track" aria-hidden>
        {segments.map((segment) => (
          <span key={segment.label} style={{ width: `${clampPercent(segment.percent)}%` }} />
        ))}
      </div>
      <div className="stacked-legend">
        {segments.map((segment) => (
          <span key={segment.label}>{segment.label}</span>
        ))}
      </div>
    </div>
  )
}

export function GroupedBars({ label, groups }: { label: string; groups: Segment[] }) {
  return (
    <div className="grouped-bars" aria-label={label}>
      {groups.map((group) => (
        <ProgressRow
          key={group.label}
          label={group.label}
          percent={group.percent}
          amountLabel={`${clampPercent(group.percent)}%`}
        />
      ))}
    </div>
  )
}

function sparklinePath(points: ChartPoint[]): string {
  if (points.length === 0) return ''
  const min = Math.min(...points)
  const max = Math.max(...points)
  const spread = max - min || 1
  return points
    .map((point, index) => {
      const x = points.length === 1 ? 50 : (index / (points.length - 1)) * 100
      const y = 32 - ((point - min) / spread) * 28
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}
