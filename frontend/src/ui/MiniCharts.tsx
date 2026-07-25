import { useState } from 'react'
import { micronutrientRdiNormalized } from '../diary/nutritionDashboard'

type Segment = { label: string; percent: number }
type MacroSegment = Segment & { amountLabel: string }
type ChartPoint = number
type TimedWeightPoint = { weightKg: number; t: number; measuredAt?: string }

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
  calorieAmountLabel,
  macros,
}: {
  calorieLabel: string
  caloriePercent: number
  calorieAmountLabel: string
  macros: MacroSegment[]
}) {
  const calorieBounded = clampPercent(caloriePercent)
  const calorieOffset = 100 * (1 - calorieBounded / 100)

  return (
    <div className="nested-goal-ring">
      <div
        className="nested-goal-ring-chart"
        aria-label={`${calorieLabel}: ${calorieAmountLabel}`}
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={calorieBounded}
        aria-valuetext={calorieAmountLabel}
      >
        <svg className="nested-goal-ring-svg" viewBox="0 0 100 100" aria-hidden="true" focusable="false">
          <circle
            className="nested-calorie-track"
            data-testid="nested-calorie-track"
            cx="50"
            cy="50"
            r="38"
            pathLength={100}
          />
          <circle
            className="nested-calorie-indicator"
            cx="50"
            cy="50"
            r="38"
            pathLength={100}
            strokeDasharray={100}
            strokeDashoffset={calorieOffset}
          />
        </svg>
        <div className="nested-goal-ring-center" aria-hidden>
          <strong>{calorieAmountLabel}</strong>
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
              aria-label={`${macro.label}: ${macro.amountLabel}`}
            >
              <span className="nested-macro-name">{macro.label}</span>
              <div className="nested-macro-bar" aria-hidden>
                <div className="nested-macro-bar-track" data-testid="nested-macro-bar-track">
                  <div className="nested-macro-bar-fill" style={{ width: `${bounded}%` }} />
                </div>
              </div>
              <span className="nested-macro-amount">{macro.amountLabel}</span>
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
  amountLabel?: string
}) {
  const bounded = clampPercent(percent)
  return (
    <div className="progress-row">
      <span>{label}</span>
      <div className="mini-track" aria-hidden>
        <div className="mini-fill" style={{ width: `${bounded}%` }} />
      </div>
      {amountLabel ? <strong>{amountLabel}</strong> : null}
    </div>
  )
}

export function MicroProgressGrid({
  rows,
}: {
  rows: Array<{ code: string; label: string; percent: number }>
}) {
  return (
    <div className="micro-grid">
      {rows.map((row) => (
        <div className="micro-cell" key={row.code}>
          <span>{row.label}</span>
          <div
            className="mini-track micro-track"
            role="progressbar"
            aria-label={row.label}
            aria-valuenow={clampPercent(row.percent)}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div className="mini-fill" style={{ width: `${clampPercent(row.percent)}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export function NutrientTrendLineChart({
  label,
  points,
}: {
  label: string
  points: Array<{ percent: number }>
}) {
  const path = nutrientTrendPath(points.map((point) => clampPercent(point.percent)))
  return (
    <svg className="micro-trend-chart" viewBox="0 0 100 36" role="img" aria-label={label}>
      <line className="micro-trend-baseline" x1="0" y1="34" x2="100" y2="34" />
      {path ? <path className="micro-trend-line" d={path} /> : null}
    </svg>
  )
}

export function MicroTrendGrid({
  rows,
}: {
  rows: Array<{
    code: string
    label: string
    latestPercent: number
    latestAmountLabel: string
    points: Array<{ percent: number }>
  }>
}) {
  return (
    <div className="micro-trend-grid">
      {rows.map((row) => (
        <div
          className={`micro-trend-cell micro-trend-${progressTone(row.latestPercent)}`}
          key={row.code}
        >
          <span>{row.label}</span>
          <NutrientTrendLineChart label={`${row.label} trend, last 30 days`} points={row.points} />
          <strong className="micro-trend-meta">{row.latestAmountLabel}</strong>
        </div>
      ))}
    </div>
  )
}

const MICRO_LINE_COLORS = [
  '#2f6b4f',
  '#c48a1a',
  '#3d7ea6',
  '#b4543c',
  '#6b5b95',
  '#2f8f5b',
  '#8d6e4c',
  '#4a90a4',
  '#a65d7b',
  '#5a7d4a',
  '#d08a3e',
  '#3f6f8f',
  '#7a5c45',
]

const SHARED_MICRO = {
  w: 360,
  h: 180,
  left: 36,
  right: 348,
  top: 14,
  bottom: 150,
} as const

export function SharedMicronutrientTrendChart({
  label,
  series,
}: {
  label: string
  series: Array<{
    code: string
    label: string
    target: number | null
    points: Array<{ date: string; amount: number }>
  }>
}) {
  const dayCount = series[0]?.points.length ?? 0
  const plotWidth = SHARED_MICRO.right - SHARED_MICRO.left
  const plotHeight = SHARED_MICRO.bottom - SHARED_MICRO.top
  const centerY = SHARED_MICRO.top + plotHeight / 2

  const paths = series.map((item, index) => {
    const color = MICRO_LINE_COLORS[index % MICRO_LINE_COLORS.length]
    if (!item.target || item.points.length === 0) {
      return { ...item, color, d: '' }
    }
    const d = item.points
      .map((point, pointIndex) => {
        const normalized = micronutrientRdiNormalized(point.amount, item.target)
        const yRatio = normalized ?? 0
        const x =
          item.points.length === 1
            ? SHARED_MICRO.left + plotWidth / 2
            : SHARED_MICRO.left + (pointIndex / (item.points.length - 1)) * plotWidth
        const y = SHARED_MICRO.bottom - yRatio * plotHeight
        return `${pointIndex === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
      })
      .join(' ')
    return { ...item, color, d }
  })

  return (
    <div className="shared-micro-trend">
      <svg
        className="shared-micro-trend-chart"
        viewBox={`0 0 ${SHARED_MICRO.w} ${SHARED_MICRO.h}`}
        role="img"
        aria-label={label}
        data-testid="shared-micro-trend-chart"
      >
        <line
          className="shared-micro-guide"
          x1={SHARED_MICRO.left}
          x2={SHARED_MICRO.right}
          y1={SHARED_MICRO.top}
          y2={SHARED_MICRO.top}
        />
        <line
          className="shared-micro-guide shared-micro-guide-center"
          x1={SHARED_MICRO.left}
          x2={SHARED_MICRO.right}
          y1={centerY}
          y2={centerY}
        />
        <line
          className="shared-micro-guide"
          x1={SHARED_MICRO.left}
          x2={SHARED_MICRO.right}
          y1={SHARED_MICRO.bottom}
          y2={SHARED_MICRO.bottom}
        />
        <text
          className="shared-micro-axis-label"
          x={SHARED_MICRO.left - 4}
          y={SHARED_MICRO.top + 4}
          textAnchor="end"
        >
          150%
        </text>
        <text className="shared-micro-axis-label" x={SHARED_MICRO.left - 4} y={centerY + 3} textAnchor="end">
          RDI
        </text>
        <text
          className="shared-micro-axis-label"
          x={SHARED_MICRO.left - 4}
          y={SHARED_MICRO.bottom + 3}
          textAnchor="end"
        >
          50%
        </text>
        {dayCount > 0 ? (
          <text className="shared-micro-axis-label" x={SHARED_MICRO.left} y={SHARED_MICRO.h - 8} textAnchor="start">
            {series[0]?.points[0]?.date.slice(5) ?? ''}
          </text>
        ) : null}
        {dayCount > 1 ? (
          <text className="shared-micro-axis-label" x={SHARED_MICRO.right} y={SHARED_MICRO.h - 8} textAnchor="end">
            {series[0]?.points[dayCount - 1]?.date.slice(5) ?? ''}
          </text>
        ) : null}
        {paths.map((item) =>
          item.d ? (
            <path
              key={item.code}
              className="shared-micro-line"
              data-testid={`shared-micro-line-${item.code}`}
              d={item.d}
              stroke={item.color}
            >
              <title>{item.label}</title>
            </path>
          ) : null,
        )}
      </svg>
      <ul className="shared-micro-legend" aria-label={`${label} legend`}>
        {paths.map((item) => (
          <li key={item.code}>
            <span className="shared-micro-swatch" style={{ background: item.color }} aria-hidden />
            {item.label}
          </li>
        ))}
      </ul>
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

export function WeightTrendChart({
  label,
  points,
  xLabels = [],
}: {
  label: string
  points: TimedWeightPoint[]
  xLabels?: string[]
}) {
  const plotted = timedWeightLayout(points)
  const [hover, setHover] = useState<{ x: number; y: number; text: string } | null>(null)

  return (
    <svg
      className="weight-trend-chart"
      viewBox={`0 0 ${VIEW.w} ${VIEW.h}`}
      role="img"
      aria-label={label}
    >
      <g data-testid="weight-trend-grid" className="weight-trend-grid" aria-hidden>
        {plotted.yTicks.map((tick) => (
          <line
            key={`h-${tick.value}`}
            className="weight-trend-gridline"
            x1={PLOT.left}
            x2={PLOT.right}
            y1={tick.y}
            y2={tick.y}
          />
        ))}
        {[0, 0.5, 1].map((t) => {
          const x = PLOT.left + t * PLOT.width
          return (
            <line
              key={`v-${t}`}
              className="weight-trend-gridline weight-trend-gridline-vertical"
              x1={x}
              x2={x}
              y1={PLOT.top}
              y2={PLOT.bottom}
            />
          )
        })}
      </g>
      {plotted.yTicks.map((tick) => (
        <text
          key={`y-${tick.value}`}
          className="weight-trend-axis-label"
          data-testid="weight-trend-y-tick"
          x={PLOT.left - 4}
          y={tick.y + 3}
          textAnchor="end"
        >
          {formatKgTick(tick.value)}
        </text>
      ))}
      {xLabels.slice(0, 3).map((tickLabel, index) => {
        const t = index / Math.max(1, Math.min(2, xLabels.length - 1))
        const x = PLOT.left + t * PLOT.width
        const anchor = index === 0 ? 'start' : index === xLabels.length - 1 ? 'end' : 'middle'
        return (
          <text
            key={`x-${tickLabel}-${index}`}
            className="weight-trend-axis-label"
            data-testid="weight-trend-x-tick"
            x={x}
            y={PLOT.bottom + 16}
            textAnchor={anchor}
          >
            {tickLabel}
          </text>
        )
      })}
      {plotted.path ? (
        <path className="weight-trend-line" data-testid="weight-trend-path" d={plotted.path} />
      ) : null}
      {plotted.markers.map((marker, index) => (
        <g key={`${marker.x}-${marker.y}-${index}`}>
          <circle
            className="weight-trend-hit"
            cx={marker.x}
            cy={marker.y}
            r={10}
            aria-label={marker.title}
            onMouseEnter={() => setHover({ x: marker.x, y: marker.y, text: marker.label })}
            onMouseLeave={() => setHover(null)}
            onFocus={() => setHover({ x: marker.x, y: marker.y, text: marker.label })}
            onBlur={() => setHover(null)}
            tabIndex={0}
          >
            <title>{marker.title}</title>
          </circle>
          <circle
            className="weight-trend-point"
            data-testid="weight-trend-point"
            cx={marker.x}
            cy={marker.y}
            r={2}
            pointerEvents="none"
          />
        </g>
      ))}
      {hover ? (
        <g
          className="weight-trend-tooltip"
          data-testid="weight-trend-tooltip"
          transform={`translate(${tooltipAnchor(hover.x)}, ${Math.max(18, hover.y - 14)})`}
          pointerEvents="none"
        >
          <rect className="weight-trend-tooltip-bg" x={-26} y={-14} width={52} height={18} rx={4} />
          <text className="weight-trend-tooltip-text" textAnchor="middle" y={-1}>
            {hover.text}
          </text>
        </g>
      ) : null}
    </svg>
  )
}

const FOOD_BAR_TONES = ['calories', 'protein', 'carbs', 'fat'] as const

export function StackedFoodBars({
  rows,
}: {
  rows: Array<{ label: string; percent: number; amountLabel: string }>
}) {
  return (
    <ul className="stacked-food-bars" data-testid="diary-macro-bars">
      {rows.map((row, index) => {
        const bounded = clampPercent(row.percent)
        const tone = FOOD_BAR_TONES[index % FOOD_BAR_TONES.length]
        return (
          <li
            key={row.label}
            className={`stacked-food-bar stacked-food-bar-${tone}`}
            aria-label={`${row.label}: ${row.amountLabel}`}
          >
            <div className="stacked-food-bar-meta">
              <span className="stacked-food-bar-name">{row.label}</span>
              <span className="stacked-food-bar-amount">{row.amountLabel}</span>
            </div>
            <div
              className="stacked-food-bar-track"
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={bounded}
              aria-valuetext={row.amountLabel}
            >
              <div className="stacked-food-bar-fill" style={{ width: `${bounded}%` }} />
            </div>
          </li>
        )
      })}
    </ul>
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

function nutrientTrendPath(points: number[]): string {
  if (points.length === 0) return ''
  return points
    .map((point, index) => {
      const x = points.length === 1 ? 50 : (index / (points.length - 1)) * 100
      const y = 34 - (point / 100) * 30
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

function progressTone(percent: number): 'good' | 'mid' | 'low' {
  const bounded = clampPercent(percent)
  if (bounded >= 80) return 'good'
  if (bounded >= 40) return 'mid'
  return 'low'
}

const VIEW = { w: 360, h: 140 } as const

const PLOT = {
  left: 40,
  right: 352,
  top: 14,
  bottom: 112,
  width: 312,
  height: 98,
} as const

function timedWeightLayout(points: TimedWeightPoint[]): {
  path: string
  markers: Array<{ x: number; y: number; label: string; title: string }>
  yTicks: Array<{ value: number; y: number }>
} {
  if (points.length === 0) {
    return {
      path: '',
      markers: [],
      yTicks: [],
    }
  }
  const weights = points.map((point) => point.weightKg)
  const rawMin = Math.min(...weights)
  const rawMax = Math.max(...weights)
  const pad = rawMax === rawMin ? Math.max(0.5, rawMin * 0.01) : (rawMax - rawMin) * 0.12
  const min = rawMin - pad
  const max = rawMax + pad
  const spread = max - min || 1
  const markers = points.map((point) => {
    const label = `${formatKgTick(point.weightKg)} kg`
    return {
      x: PLOT.left + Math.max(0, Math.min(1, point.t)) * PLOT.width,
      y: PLOT.bottom - ((point.weightKg - min) / spread) * PLOT.height,
      label,
      title: formatPointTitle(point, label),
    }
  })
  const path = markers
    .map((marker, index) => `${index === 0 ? 'M' : 'L'} ${marker.x.toFixed(2)} ${marker.y.toFixed(2)}`)
    .join(' ')
  const tickValues = [rawMax, (rawMin + rawMax) / 2, rawMin]
  const uniqueTicks = [...new Set(tickValues.map((value) => Number(value.toFixed(1))))]
  const yTicks = uniqueTicks.map((value) => ({
    value,
    y: PLOT.bottom - ((value - min) / spread) * PLOT.height,
  }))
  return { path, markers, yTicks }
}

function formatPointTitle(point: TimedWeightPoint, label: string): string {
  if (!point.measuredAt) return label
  const date = new Date(point.measuredAt)
  if (Number.isNaN(date.getTime())) return label
  const day = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(date)
  return `${label} · ${day}`
}

function tooltipAnchor(x: number): number {
  return Math.max(PLOT.left + 28, Math.min(PLOT.right - 28, x))
}

function formatKgTick(value: number): string {
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 1,
    minimumFractionDigits: 1,
  }).format(value)
}
