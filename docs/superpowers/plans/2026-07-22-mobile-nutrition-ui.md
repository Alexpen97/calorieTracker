# Mobile Nutrition UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the NutriTrack front end around the committed mobile-first card mockups for dashboard, analytics, diary, weight, macros, vitamins, and minerals.

**Architecture:** Keep the existing React/Vite app and TanStack Query data flow. Add small reusable UI primitives, pure nutrition-display helpers, and focused page components instead of introducing a charting dependency. Use CSS/SVG for lightweight rings, bars, sparklines, and trend charts, with the existing diary/profile APIs as the live data source.

**Tech Stack:** React 19.1, React Router 7.7, TanStack React Query 5.83, Vite 7, Vitest 3.2.4, jsdom, React Testing Library, plain CSS.

## Global Constraints

- Mobile is the primary platform; all new screens must be usable at 360px width before desktop refinement.
- Match `docs/design/mockups/mobile-nutrition-dashboard.png`, `mobile-nutrition-analytics.png`, and `mobile-nutrition-diary.png`.
- Preserve current auth behavior and protected routes in `frontend/src/App.tsx`.
- Do not add chart libraries; implement charts as accessible HTML/CSS/SVG components.
- Keep backend behavior unchanged unless an existing endpoint is only missing from `frontend/src/api/client.ts`.
- Always write or update tests for new functionality.
- Use Testing Library role/text/label queries rather than class selectors.
- Run `npm test` and `npm run build` from `frontend/` before final completion.
- Update `AI/` notes after implementation.

---

## Current project facts

- Existing routes live in `frontend/src/App.tsx`: `/today`, `/lookup`, `/products/:id`, `/me`, `/submit-product`, and `/moderation`.
- Current diary UI lives in `frontend/src/pages/DiaryPage.tsx`.
- Current profile weight and goals UI lives in `frontend/src/pages/ProfilePage.tsx`.
- Current helper tests live beside helpers, for example `frontend/src/diary/formatDay.test.ts`.
- Current component tests use `@testing-library/react`, `@testing-library/jest-dom/vitest`, `QueryClientProvider`, and `vi.spyOn`.
- The backend already exposes `GET /api/diary/summary/range?from=&to=&zone=`; `frontend/src/api/client.ts` only needs a client wrapper.

## File structure

- Create `frontend/src/ui/Card.tsx` for reusable card, metric, progress, and empty-state primitives.
- Create `frontend/src/ui/Card.test.tsx` for primitive rendering tests.
- Create `frontend/src/ui/MiniCharts.tsx` for ring, progress row, sparkline, stacked bars, and grouped bars.
- Create `frontend/src/ui/MiniCharts.test.tsx` for chart accessibility and bounded-value tests.
- Create `frontend/src/navigation/AppNavigation.tsx` for top brand and bottom mobile tab navigation.
- Create `frontend/src/navigation/AppNavigation.test.tsx` for visible tab/link tests.
- Create `frontend/src/diary/nutritionDashboard.ts` for pure display helpers.
- Create `frontend/src/diary/nutritionDashboard.test.ts` for macro/micro/weight/range helper tests.
- Modify `frontend/src/api/client.ts` to add `fetchDiarySummaryRange(from, to)`.
- Modify `frontend/src/api/client.authRefresh.test.ts` or create `frontend/src/api/client.diaryRange.test.ts` to cover the new client URL.
- Create `frontend/src/pages/DashboardPage.tsx` for `/today`.
- Create `frontend/src/pages/DashboardPage.test.tsx` for mocked dashboard data rendering.
- Refactor `frontend/src/pages/DiaryPage.tsx` into the mobile diary timeline screen.
- Create `frontend/src/pages/DiaryPage.test.tsx` for meal cards, macro summary, and add-food affordance.
- Create `frontend/src/pages/AnalyticsPage.tsx` for `/analytics`.
- Create `frontend/src/pages/AnalyticsPage.test.tsx` for range summaries, weight trend, and insight cards.
- Modify `frontend/src/pages/ProfilePage.tsx` only where needed to reuse shared card styles and keep weight logging intact.
- Modify `frontend/src/App.tsx` to route `/today` to `DashboardPage`, add `/diary` and `/analytics`, and use `AppNavigation`.
- Modify `frontend/src/index.css` to add design tokens, mobile shell, card layout, bottom nav, and responsive desktop rules.
- Update `AI/mobile-ui-mockups.md` with implementation status and any deviations from the mockups.

---

### Task 1: Shared card and chart primitives

**Files:**
- Create: `frontend/src/ui/Card.tsx`
- Create: `frontend/src/ui/Card.test.tsx`
- Create: `frontend/src/ui/MiniCharts.tsx`
- Create: `frontend/src/ui/MiniCharts.test.tsx`
- Modify: `frontend/src/index.css`

**Interfaces:**
- Produces `DashboardCard`, `MetricPill`, `EmptyCard`, `ProgressRing`, `ProgressRow`, `Sparkline`, `StackedBar`, and `GroupedBars`.
- Consumed by dashboard, diary, analytics, and profile screens.

- [ ] **Step 1: Write failing primitive tests**

Add `frontend/src/ui/Card.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DashboardCard, EmptyCard, MetricPill } from './Card'

describe('card primitives', () => {
  it('renders a titled dashboard card with optional action content', () => {
    render(
      <DashboardCard eyebrow="Today" title="Macros" action={<a href="/diary">Open</a>}>
        <p>Protein is on target.</p>
      </DashboardCard>,
    )

    expect(screen.getByText('Today')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Macros' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open' })).toHaveAttribute('href', '/diary')
  })

  it('renders metric pills and empty states accessibly', () => {
    render(
      <>
        <MetricPill label="Protein" value="82g" tone="green" />
        <EmptyCard title="No meals yet" copy="Add food to start tracking today." />
      </>,
    )

    expect(screen.getByText('Protein')).toBeInTheDocument()
    expect(screen.getByText('82g')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'No meals yet' })).toBeInTheDocument()
  })
})
```

Add `frontend/src/ui/MiniCharts.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { GroupedBars, ProgressRing, ProgressRow, Sparkline, StackedBar } from './MiniCharts'

describe('mini chart primitives', () => {
  it('bounds progress values and exposes readable labels', () => {
    render(
      <>
        <ProgressRing label="Calories" percent={128} value="1,920" />
        <ProgressRow label="Vitamin D" percent={43} amountLabel="43%" />
      </>,
    )

    expect(screen.getByLabelText('Calories: 100%')).toBeInTheDocument()
    expect(screen.getByText('1,920')).toBeInTheDocument()
    expect(screen.getByText('Vitamin D')).toBeInTheDocument()
    expect(screen.getByText('43%')).toBeInTheDocument()
  })

  it('renders sparkline, stacked, and grouped chart labels', () => {
    render(
      <>
        <Sparkline label="Weight trend" points={[72.4, 72.1, 71.8]} />
        <StackedBar label="Macro balance" segments={[{ label: 'Protein', percent: 30 }, { label: 'Carbs', percent: 45 }, { label: 'Fat', percent: 25 }]} />
        <GroupedBars label="Minerals" groups={[{ label: 'Iron', percent: 70 }, { label: 'Calcium', percent: 55 }]} />
      </>,
    )

    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
    expect(screen.getByText('Macro balance')).toBeInTheDocument()
    expect(screen.getByText('Iron')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: Run tests and confirm failure**

Run: `npm test -- --run src/ui/Card.test.tsx src/ui/MiniCharts.test.tsx`

Expected: FAIL because `frontend/src/ui/Card.tsx` and `frontend/src/ui/MiniCharts.tsx` do not exist.

- [ ] **Step 3: Implement primitives**

Create `frontend/src/ui/Card.tsx`:

```tsx
import type { ReactNode } from 'react'

type CardTone = 'green' | 'amber' | 'purple' | 'blue'

export function DashboardCard({
  eyebrow,
  title,
  action,
  children,
  className = '',
}: {
  eyebrow?: string
  title: string
  action?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`dashboard-card ${className}`.trim()}>
      <div className="card-heading">
        <div>
          {eyebrow && <p className="card-eyebrow">{eyebrow}</p>}
          <h2>{title}</h2>
        </div>
        {action}
      </div>
      {children}
    </section>
  )
}

export function MetricPill({ label, value, tone }: { label: string; value: string; tone: CardTone }) {
  return (
    <div className={`metric-pill metric-pill-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export function EmptyCard({ title, copy }: { title: string; copy: string }) {
  return (
    <section className="dashboard-card empty-card">
      <h2>{title}</h2>
      <p>{copy}</p>
    </section>
  )
}
```

Create `frontend/src/ui/MiniCharts.tsx`:

```tsx
import type { CSSProperties } from 'react'

type Segment = { label: string; percent: number }
type ChartPoint = number

function clampPercent(value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(100, Math.round(value)))
}

export function ProgressRing({ label, percent, value }: { label: string; percent: number; value: string }) {
  const bounded = clampPercent(percent)
  return (
    <div className="progress-ring" aria-label={`${label}: ${bounded}%`} style={{ '--progress': `${bounded}%` } as CSSProperties}>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  )
}

export function ProgressRow({ label, percent, amountLabel }: { label: string; percent: number; amountLabel: string }) {
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
      <div className="stacked-legend">{segments.map((segment) => <span key={segment.label}>{segment.label}</span>)}</div>
    </div>
  )
}

export function GroupedBars({ label, groups }: { label: string; groups: Segment[] }) {
  return (
    <div className="grouped-bars" aria-label={label}>
      {groups.map((group) => (
        <ProgressRow key={group.label} label={group.label} percent={group.percent} amountLabel={`${clampPercent(group.percent)}%`} />
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
```

- [ ] **Step 4: Add primitive CSS**

Append styles in `frontend/src/index.css` near the existing diary styles:

```css
.dashboard-card {
  margin-top: 1rem;
  padding: 1rem;
  border: 1px solid rgba(31, 74, 55, 0.1);
  border-radius: 1.35rem;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 40px rgba(31, 74, 55, 0.08);
}

.card-heading,
.progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
}

.card-heading h2,
.empty-card h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.25rem;
}

.card-eyebrow {
  margin: 0 0 0.2rem;
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #5d7166;
}

.metric-pill {
  display: grid;
  gap: 0.15rem;
  padding: 0.7rem 0.85rem;
  border-radius: 999px;
  background: rgba(47, 107, 79, 0.1);
}

.metric-pill span {
  font-size: 0.72rem;
  color: #5d7166;
}

.metric-pill strong {
  color: var(--leaf-deep);
}

.metric-pill-green {
  background: rgba(47, 107, 79, 0.1);
}

.metric-pill-amber {
  background: rgba(226, 176, 74, 0.16);
}

.metric-pill-purple {
  background: rgba(141, 121, 184, 0.14);
}

.metric-pill-blue {
  background: rgba(87, 137, 170, 0.14);
}

.progress-ring {
  width: 7rem;
  height: 7rem;
  border-radius: 50%;
  display: grid;
  place-items: center;
  text-align: center;
  background: conic-gradient(var(--leaf) var(--progress), rgba(31, 74, 55, 0.1) 0);
}

.progress-ring strong,
.progress-ring span {
  grid-area: 1 / 1;
}

.progress-ring span {
  transform: translateY(1.1rem);
  font-size: 0.75rem;
  color: #5d7166;
}

.mini-track,
.stacked-track {
  flex: 1;
  height: 0.65rem;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(31, 74, 55, 0.12);
}

.mini-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--leaf), var(--sun));
}

.sparkline {
  width: 100%;
  height: 3rem;
  fill: none;
  stroke: var(--leaf);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}
```

- [ ] **Step 5: Verify and commit**

Run: `npm test -- --run src/ui/Card.test.tsx src/ui/MiniCharts.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

Commit:

```bash
git add frontend/src/ui/Card.tsx frontend/src/ui/Card.test.tsx frontend/src/ui/MiniCharts.tsx frontend/src/ui/MiniCharts.test.tsx frontend/src/index.css
git commit -m "feat: add mobile nutrition ui primitives"
```

---

### Task 2: Nutrition display helpers and diary range client

**Files:**
- Create: `frontend/src/diary/nutritionDashboard.ts`
- Create: `frontend/src/diary/nutritionDashboard.test.ts`
- Modify: `frontend/src/api/client.ts`
- Create: `frontend/src/api/client.diaryRange.test.ts`

**Interfaces:**
- Produces `buildMacroSummaries(totals)`, `buildMicronutrientRows(totals, kind)`, `buildWeightTrend(weights)`, `dateDaysAgo(days)`, and `fetchDiarySummaryRange(from, to)`.
- Consumed by dashboard and analytics pages.

- [ ] **Step 1: Write failing helper tests**

Add `frontend/src/diary/nutritionDashboard.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import {
  buildMacroSummaries,
  buildMicronutrientRows,
  buildWeightTrend,
  dateDaysAgo,
} from './nutritionDashboard'
import type { NutrientTotalForDisplay } from './formatDay'
import type { WeightLog } from '../api/client'

describe('nutrition dashboard helpers', () => {
  const totals: NutrientTotalForDisplay[] = [
    { code: 'protein', amount: 80, unit: 'g', target: 100 },
    { code: 'carbohydrates', amount: 220, unit: 'g', target: 250 },
    { code: 'fat', amount: 55, unit: 'g', target: 70 },
    { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
    { code: 'calcium', amount: 650, unit: 'mg', target: 1000 },
  ]

  it('builds macro summaries in display order', () => {
    expect(buildMacroSummaries(totals).map((item) => item.label)).toEqual(['Protein', 'Carbs', 'Fat'])
    expect(buildMacroSummaries(totals)[0]).toMatchObject({ code: 'protein', percent: 80, amountLabel: '80 / 100 g' })
  })

  it('builds vitamin and mineral progress rows', () => {
    expect(buildMicronutrientRows(totals, 'vitamin')).toEqual([
      { code: 'vitamin_d', label: 'Vitamin D', percent: 40, amountLabel: '6 / 15 ug' },
    ])
    expect(buildMicronutrientRows(totals, 'mineral')[0]).toMatchObject({ label: 'Calcium', percent: 65 })
  })

  it('builds oldest-to-newest weight trend points', () => {
    const weights: WeightLog[] = [
      { id: '2', weightKg: 71.8, measuredAt: '2026-07-22T08:00:00Z' },
      { id: '1', weightKg: 72.3, measuredAt: '2026-07-20T08:00:00Z' },
    ]

    expect(buildWeightTrend(weights)).toEqual([72.3, 71.8])
  })

  it('formats dates relative to a provided clock', () => {
    expect(dateDaysAgo(6, new Date(2026, 6, 22))).toBe('2026-07-16')
  })
})
```

Add `frontend/src/api/client.diaryRange.test.ts`:

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchDiarySummaryRange } from './client'
import { saveTokens } from '../auth/tokenStorage'

describe('diary range API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('calls the range summary endpoint with from, to, and browser zone', async () => {
    saveTokens({ accessToken: 'access', refreshToken: 'refresh' })
    vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({ timeZone: 'Europe/Amsterdam' } as Intl.ResolvedDateTimeFormatOptions)
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify([]), { status: 200 }))

    await fetchDiarySummaryRange('2026-07-16', '2026-07-22')

    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/diary/summary/range?from=2026-07-16&to=2026-07-22&zone=Europe%2FAmsterdam',
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer access' }) }),
    )
  })
})
```

- [ ] **Step 2: Run tests and confirm failure**

Run: `npm test -- --run src/diary/nutritionDashboard.test.ts src/api/client.diaryRange.test.ts`

Expected: FAIL because the helper module and range client do not exist.

- [ ] **Step 3: Implement helper module**

Create `frontend/src/diary/nutritionDashboard.ts`:

```ts
import type { WeightLog } from '../api/client'
import type { NutrientTotalForDisplay } from './formatDay'

export type MicronutrientKind = 'vitamin' | 'mineral'

export type NutritionProgressRow = {
  code: string
  label: string
  percent: number
  amountLabel: string
}

const macroDisplay = [
  ['protein', 'Protein'],
  ['carbohydrates', 'Carbs'],
  ['fat', 'Fat'],
] as const

const vitaminCodes = new Map([
  ['vitamin_a', 'Vitamin A'],
  ['vitamin_c', 'Vitamin C'],
  ['vitamin_d', 'Vitamin D'],
  ['vitamin_b12', 'B12'],
])

const mineralCodes = new Map([
  ['iron', 'Iron'],
  ['calcium', 'Calcium'],
  ['magnesium', 'Magnesium'],
  ['potassium', 'Potassium'],
])

export function buildMacroSummaries(totals: NutrientTotalForDisplay[]): NutritionProgressRow[] {
  return macroDisplay
    .map(([code, label]) => progressRow(totals, code, label))
    .filter((row): row is NutritionProgressRow => row !== null)
}

export function buildMicronutrientRows(
  totals: NutrientTotalForDisplay[],
  kind: MicronutrientKind,
): NutritionProgressRow[] {
  const labels = kind === 'vitamin' ? vitaminCodes : mineralCodes
  return [...labels.entries()]
    .map(([code, label]) => progressRow(totals, code, label))
    .filter((row): row is NutritionProgressRow => row !== null)
}

export function buildWeightTrend(weights: WeightLog[]): number[] {
  return [...weights]
    .sort((left, right) => new Date(left.measuredAt).getTime() - new Date(right.measuredAt).getTime())
    .slice(-14)
    .map((weight) => weight.weightKg)
}

export function dateDaysAgo(days: number, clock = new Date()): string {
  const date = new Date(clock)
  date.setDate(date.getDate() - days)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function progressRow(
  totals: NutrientTotalForDisplay[],
  code: string,
  label: string,
): NutritionProgressRow | null {
  const total = totals.find((item) => item.code === code)
  if (!total) return null
  return {
    code,
    label,
    percent: total.target ? Math.min(100, Math.round((total.amount / total.target) * 100)) : 0,
    amountLabel: total.target
      ? `${formatNumber(total.amount)} / ${formatNumber(total.target)} ${total.unit}`
      : `${formatNumber(total.amount)} ${total.unit}`,
  }
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 1 }).format(value)
}
```

- [ ] **Step 4: Implement API range client**

Modify `frontend/src/api/client.ts` after `fetchDiarySummary`:

```ts
export async function fetchDiarySummaryRange(from: string, to: string): Promise<DaySummary[]> {
  const params = new URLSearchParams({ from, to, zone: browserTimeZone() })
  const response = await authenticatedFetch(`${apiBase}/api/diary/summary/range?${params}`)
  return parseJson<DaySummary[]>(response)
}
```

- [ ] **Step 5: Verify and commit**

Run: `npm test -- --run src/diary/nutritionDashboard.test.ts src/api/client.diaryRange.test.ts`

Expected: PASS.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

Commit:

```bash
git add frontend/src/diary/nutritionDashboard.ts frontend/src/diary/nutritionDashboard.test.ts frontend/src/api/client.ts frontend/src/api/client.diaryRange.test.ts
git commit -m "feat: add nutrition dashboard data helpers"
```

---

### Task 3: Mobile app navigation and routes

**Files:**
- Create: `frontend/src/navigation/AppNavigation.tsx`
- Create: `frontend/src/navigation/AppNavigation.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/index.css`

**Interfaces:**
- Produces `AppNavigation({ loggedIn, canModerate })`.
- Routes `/today` to `DashboardPage`, `/diary` to `DiaryPage`, and `/analytics` to `AnalyticsPage`.

- [ ] **Step 1: Write failing navigation test**

Add `frontend/src/navigation/AppNavigation.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import AppNavigation from './AppNavigation'

describe('AppNavigation', () => {
  it('renders mobile-first primary tabs for signed-in users', () => {
    render(
      <MemoryRouter>
        <AppNavigation loggedIn canModerate={false} />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/today')
    expect(screen.getByRole('link', { name: 'Diary' })).toHaveAttribute('href', '/diary')
    expect(screen.getByRole('link', { name: 'Analytics' })).toHaveAttribute('href', '/analytics')
    expect(screen.getByRole('link', { name: 'Profile' })).toHaveAttribute('href', '/me')
  })

  it('keeps moderation available only for moderators and admins', () => {
    render(
      <MemoryRouter>
        <AppNavigation loggedIn canModerate />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Moderation' })).toHaveAttribute('href', '/moderation')
  })
})
```

- [ ] **Step 2: Run test and confirm failure**

Run: `npm test -- --run src/navigation/AppNavigation.test.tsx`

Expected: FAIL because `AppNavigation` does not exist.

- [ ] **Step 3: Implement navigation**

Create `frontend/src/navigation/AppNavigation.tsx`:

```tsx
import { Link, NavLink } from 'react-router-dom'

export default function AppNavigation({ loggedIn, canModerate }: { loggedIn: boolean; canModerate: boolean }) {
  return (
    <>
      <header className="topbar app-topbar">
        <Link to={loggedIn ? '/today' : '/'} className="brand">
          NutriTrack
        </Link>
        {loggedIn && (
          <nav className="topnav desktop-nav" aria-label="Primary">
            <NavLink to="/today">Dashboard</NavLink>
            <NavLink to="/diary">Diary</NavLink>
            <NavLink to="/analytics">Analytics</NavLink>
            <NavLink to="/lookup">Lookup</NavLink>
            {canModerate && <NavLink to="/moderation">Moderation</NavLink>}
            <NavLink to="/me">Profile</NavLink>
          </nav>
        )}
      </header>
      {loggedIn && (
        <nav className="bottom-nav" aria-label="Primary mobile">
          <NavLink to="/today">Dashboard</NavLink>
          <NavLink to="/diary">Diary</NavLink>
          <NavLink to="/analytics">Analytics</NavLink>
          <NavLink to="/me">Profile</NavLink>
        </nav>
      )}
    </>
  )
}
```

- [ ] **Step 4: Wire App routes**

Modify `frontend/src/App.tsx` imports:

```tsx
import { Navigate, Route, Routes } from 'react-router-dom'
import AppNavigation from './navigation/AppNavigation'
import DashboardPage from './pages/DashboardPage'
import AnalyticsPage from './pages/AnalyticsPage'
```

Replace the header block with:

```tsx
<AppNavigation loggedIn={loggedIn} canModerate={canModerate} />
```

Change `/today` to render `DashboardPage`, add `/diary`, and add `/analytics`:

```tsx
<Route
  path="/today"
  element={
    <RequireAuth>
      <DashboardPage />
    </RequireAuth>
  }
/>
<Route
  path="/diary"
  element={
    <RequireAuth>
      <DiaryPage />
    </RequireAuth>
  }
/>
<Route
  path="/analytics"
  element={
    <RequireAuth>
      <AnalyticsPage />
    </RequireAuth>
  }
/>
```

- [ ] **Step 5: Add navigation CSS**

Append to `frontend/src/index.css`:

```css
.app-shell {
  padding-bottom: 5.5rem;
}

.desktop-nav {
  display: none;
}

.bottom-nav {
  position: fixed;
  left: 1rem;
  right: 1rem;
  bottom: 1rem;
  z-index: 15;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.35rem;
  padding: 0.45rem;
  border-radius: 1.5rem;
  background: rgba(255, 252, 245, 0.92);
  box-shadow: 0 18px 40px rgba(31, 74, 55, 0.18);
  backdrop-filter: blur(10px);
}

.bottom-nav a {
  padding: 0.75rem 0.35rem;
  border-radius: 1.1rem;
  text-align: center;
  text-decoration: none;
  font-size: 0.8rem;
  font-weight: 700;
  color: #5d7166;
}

.bottom-nav a.active {
  background: var(--leaf);
  color: #f7fff9;
}

@media (min-width: 760px) {
  .app-shell {
    padding-bottom: 0;
  }

  .desktop-nav {
    display: flex;
  }

  .bottom-nav {
    display: none;
  }
}
```

- [ ] **Step 6: Verify and commit**

Run: `npm test -- --run src/navigation/AppNavigation.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: PASS only after Task 4 and Task 5 have created placeholder or full `DashboardPage` and `AnalyticsPage`. If implementing this task independently, create temporary minimal components with headings and replace them in later tasks before committing.

Commit:

```bash
git add frontend/src/navigation/AppNavigation.tsx frontend/src/navigation/AppNavigation.test.tsx frontend/src/App.tsx frontend/src/index.css
git commit -m "feat: add mobile app navigation"
```

---

### Task 4: Mobile dashboard screen

**Files:**
- Create: `frontend/src/pages/DashboardPage.tsx`
- Create: `frontend/src/pages/DashboardPage.test.tsx`
- Modify: `frontend/src/index.css`

**Interfaces:**
- Consumes `fetchDiarySummary`, `fetchDiaryEntries`, `fetchWater`, `fetchWeightHistory`, `DashboardCard`, `ProgressRing`, `ProgressRow`, `Sparkline`, and nutrition helpers.
- Produces the primary `/today` dashboard from the mobile mockup.

- [ ] **Step 1: Write failing dashboard test**

Add `frontend/src/pages/DashboardPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import * as client from '../api/client'

describe('DashboardPage', () => {
  it('renders today summary, macros, vitamins, minerals, and weight cards', async () => {
    vi.spyOn(client, 'fetchDiarySummary').mockResolvedValue({
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 1450, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 82, unit: 'g', target: 100 },
        { code: 'carbohydrates', amount: 180, unit: 'g', target: 250 },
        { code: 'fat', amount: 48, unit: 'g', target: 70 },
        { code: 'vitamin_d', amount: 6, unit: 'ug', target: 15 },
        { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
      ],
      water: { amountMl: 1200, targetMl: 2500 },
    })
    vi.spyOn(client, 'fetchDiaryEntries').mockResolvedValue([])
    vi.spyOn(client, 'fetchWater').mockResolvedValue([])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.4, measuredAt: '2026-07-20T08:00:00Z' },
      { id: 'w2', weightKg: 72.1, measuredAt: '2026-07-22T08:00:00Z' },
    ])

    renderWithClient(<DashboardPage />)

    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Macros' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </MemoryRouter>,
  )
}
```

- [ ] **Step 2: Run test and confirm failure**

Run: `npm test -- --run src/pages/DashboardPage.test.tsx`

Expected: FAIL because `DashboardPage` does not exist.

- [ ] **Step 3: Implement dashboard page**

Create `frontend/src/pages/DashboardPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchDiaryEntries, fetchDiarySummary, fetchWater, fetchWeightHistory } from '../api/client'
import { buildMacroSummaries, buildMicronutrientRows, buildWeightTrend } from '../diary/nutritionDashboard'
import { formatLocalDate, getMacroProgress, waterProgress } from '../diary/formatDay'
import { DashboardCard, EmptyCard, MetricPill } from '../ui/Card'
import { GroupedBars, ProgressRing, ProgressRow, Sparkline } from '../ui/MiniCharts'

export default function DashboardPage() {
  const today = formatLocalDate()
  const summaryQuery = useQuery({ queryKey: ['diary-summary', today], queryFn: () => fetchDiarySummary(today) })
  const entriesQuery = useQuery({ queryKey: ['diary-entries', today], queryFn: () => fetchDiaryEntries(today) })
  const waterQuery = useQuery({ queryKey: ['diary-water', today], queryFn: () => fetchWater(today) })
  const weightQuery = useQuery({ queryKey: ['weight-history'], queryFn: () => fetchWeightHistory() })

  const summary = summaryQuery.data
  const energy = summary ? getMacroProgress(summary.totals, 'energy_kcal') : null
  const water = summary ? waterProgress(summary.water) : null
  const macros = summary ? buildMacroSummaries(summary.totals) : []
  const vitamins = summary ? buildMicronutrientRows(summary.totals, 'vitamin') : []
  const minerals = summary ? buildMicronutrientRows(summary.totals, 'mineral') : []
  const weightTrend = buildWeightTrend(weightQuery.data ?? [])
  const mealCount = entriesQuery.data?.length ?? 0

  return (
    <main className="mobile-page dashboard-page">
      <div className="mobile-hero">
        <p className="sheet-kicker">Dashboard</p>
        <h1>Today</h1>
        <p>{today}</p>
      </div>

      {(summaryQuery.isLoading || entriesQuery.isLoading || waterQuery.isLoading || weightQuery.isLoading) && <p>Loading dashboard...</p>}
      {[summaryQuery.error, entriesQuery.error, waterQuery.error, weightQuery.error].filter(Boolean).map((error, index) => (
        <p className="error" key={index}>{(error as Error).message}</p>
      ))}

      {summary ? (
        <>
          <DashboardCard title="Daily summary" eyebrow="Calories" action={<Link to="/diary">Add food</Link>}>
            <div className="summary-card-grid">
              <ProgressRing label="Calories" percent={energy?.percent ?? 0} value={energy ? formatValue(energy.amount) : '0'} />
              <div className="summary-metrics">
                <MetricPill label="Meals" value={String(mealCount)} tone="green" />
                <MetricPill label="Water" value={water ? `${water.percent}%` : '0%'} tone="blue" />
              </div>
            </div>
          </DashboardCard>

          <DashboardCard title="Weight" eyebrow="Progress">
            {weightTrend.length > 0 ? <Sparkline label="Weight trend" points={weightTrend} /> : <p className="empty-copy">Log weight from Profile to see your trend.</p>}
          </DashboardCard>

          <DashboardCard title="Macros" eyebrow="Balance">
            <div className="macro-pill-grid">
              {macros.map((macro) => <MetricPill key={macro.code} label={macro.label} value={macro.amountLabel} tone="green" />)}
            </div>
          </DashboardCard>

          <DashboardCard title="Vitamins" eyebrow="Daily targets">
            {vitamins.length > 0 ? vitamins.map((row) => <ProgressRow key={row.code} label={row.label} percent={row.percent} amountLabel={row.amountLabel} />) : <p className="empty-copy">No vitamin targets yet.</p>}
          </DashboardCard>

          <DashboardCard title="Minerals" eyebrow="Daily targets">
            {minerals.length > 0 ? <GroupedBars label="Minerals" groups={minerals} /> : <p className="empty-copy">No mineral targets yet.</p>}
          </DashboardCard>
        </>
      ) : (
        !summaryQuery.isLoading && <EmptyCard title="No summary yet" copy="Add food to start your dashboard." />
      )}
    </main>
  )
}

function formatValue(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(value)
}
```

- [ ] **Step 4: Add dashboard CSS**

Append to `frontend/src/index.css`:

```css
.mobile-page {
  width: min(760px, 100%);
  margin: 0 auto;
  padding: 0 1rem 1.5rem;
}

.mobile-hero {
  padding: 0.5rem 0 0.25rem;
}

.mobile-hero h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 2rem;
  color: var(--leaf-deep);
}

.mobile-hero p {
  margin: 0.25rem 0 0;
  color: #5d7166;
}

.summary-card-grid {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
}

.summary-metrics,
.macro-pill-grid {
  display: grid;
  gap: 0.7rem;
}
```

- [ ] **Step 5: Verify and commit**

Run: `npm test -- --run src/pages/DashboardPage.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

Commit:

```bash
git add frontend/src/pages/DashboardPage.tsx frontend/src/pages/DashboardPage.test.tsx frontend/src/index.css
git commit -m "feat: add mobile nutrition dashboard"
```

---

### Task 5: Mobile diary timeline screen

**Files:**
- Modify: `frontend/src/pages/DiaryPage.tsx`
- Create: `frontend/src/pages/DiaryPage.test.tsx`
- Modify: `frontend/src/index.css`

**Interfaces:**
- Consumes current diary entry, summary, and water mutations.
- Keeps existing delete entry, add water, custom water, and lookup navigation behavior.
- Produces the mockup-style food diary timeline at `/diary`.

- [ ] **Step 1: Write failing diary page test**

Add `frontend/src/pages/DiaryPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import DiaryPage from './DiaryPage'
import * as client from '../api/client'

describe('DiaryPage', () => {
  it('renders diary timeline cards with daily macro summary', async () => {
    vi.spyOn(client, 'fetchDiarySummary').mockResolvedValue({
      date: '2026-07-22',
      totals: [
        { code: 'energy_kcal', amount: 900, unit: 'kcal', target: 2100 },
        { code: 'protein', amount: 45, unit: 'g', target: 100 },
        { code: 'vitamin_c', amount: 60, unit: 'mg', target: 80 },
        { code: 'iron', amount: 9, unit: 'mg', target: 14 },
      ],
      water: { amountMl: 750, targetMl: 2500 },
    })
    vi.spyOn(client, 'fetchWater').mockResolvedValue([])
    vi.spyOn(client, 'fetchDiaryEntries').mockResolvedValue([
      {
        id: 'entry-1',
        productId: 'p1',
        submissionId: null,
        productName: 'Morning oats',
        brand: 'Kitchen',
        weightG: 120,
        mealType: 'BREAKFAST',
        consumedAt: '2026-07-22T08:00:00Z',
        createdAt: '2026-07-22T08:00:00Z',
        nutrients: [{ code: 'energy_kcal', amount: 350, amountPer100g: 291.7, unit: 'kcal' }],
      },
    ])

    renderWithClient(<DiaryPage />)

    expect(await screen.findByRole('heading', { name: 'Food Diary' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Breakfast' })).toBeInTheDocument()
    expect(screen.getByText('Morning oats')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </MemoryRouter>,
  )
}
```

- [ ] **Step 2: Run test and confirm failure**

Run: `npm test -- --run src/pages/DiaryPage.test.tsx`

Expected: FAIL because the current heading is `Your diary`, and micronutrient cards are not present.

- [ ] **Step 3: Refactor diary UI**

Modify `frontend/src/pages/DiaryPage.tsx`:

- Keep existing queries, mutations, `submitCustomWater`, `invalidateDay`, `DiaryEntryRow`, and `WaterLogList`.
- Change `<main className="panel diary-panel">` to `<main className="mobile-page diary-page">`.
- Change the main heading to `Food Diary`.
- Add summary cards using `DashboardCard`, `MetricPill`, `ProgressRing`, `ProgressRow`, `GroupedBars`, `buildMacroSummaries`, and `buildMicronutrientRows`.
- Render meal groups as rounded timeline cards with the existing `DiaryEntryRow`.
- Keep `Add food` pointing to `/lookup`.

Implementation shape:

```tsx
const macros = summary ? buildMacroSummaries(summary.totals) : []
const vitamins = summary ? buildMicronutrientRows(summary.totals, 'vitamin') : []
const minerals = summary ? buildMicronutrientRows(summary.totals, 'mineral') : []

return (
  <main className="mobile-page diary-page">
    <div className="mobile-hero diary-mobile-hero">
      <div>
        <p className="sheet-kicker">Today</p>
        <h1>Food Diary</h1>
        <p>{today}</p>
      </div>
      <Link className="btn btn-primary" to="/lookup">Add food</Link>
    </div>

    {summary && (
      <DashboardCard title="Today summary" eyebrow="Logged nutrition">
        <div className="macro-pill-grid">
          {macros.map((macro) => <MetricPill key={macro.code} label={macro.label} value={macro.amountLabel} tone="green" />)}
        </div>
      </DashboardCard>
    )}

    <DashboardCard title="Meals" eyebrow="Timeline">
      {mealGroups.map((group) => (
        <section className="meal-card" key={group.mealType}>
          <h3>{mealLabel(group.mealType)}</h3>
          {group.entries.length === 0 ? <p className="empty-copy">No entries yet.</p> : <ul className="entry-list">{group.entries.map((entry) => <DiaryEntryRow entry={entry} key={entry.id} onDelete={(id) => removeEntry.mutate(id)} />)}</ul>}
        </section>
      ))}
    </DashboardCard>

    <DashboardCard title="Vitamins" eyebrow="Checklist">
      {vitamins.map((row) => <ProgressRow key={row.code} label={row.label} percent={row.percent} amountLabel={row.amountLabel} />)}
    </DashboardCard>

    <DashboardCard title="Minerals" eyebrow="Checklist">
      <GroupedBars label="Minerals" groups={minerals} />
    </DashboardCard>
  </main>
)
```

- [ ] **Step 4: Add diary CSS**

Append to `frontend/src/index.css`:

```css
.diary-mobile-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.meal-card {
  padding: 1rem 0;
  border-top: 1px solid rgba(31, 74, 55, 0.1);
}

.meal-card:first-of-type {
  border-top: 0;
}

.meal-card h3 {
  margin: 0 0 0.45rem;
  color: var(--leaf-deep);
}
```

- [ ] **Step 5: Verify and commit**

Run: `npm test -- --run src/pages/DiaryPage.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

Commit:

```bash
git add frontend/src/pages/DiaryPage.tsx frontend/src/pages/DiaryPage.test.tsx frontend/src/index.css
git commit -m "feat: redesign diary as mobile timeline"
```

---

### Task 6: Mobile analytics screen

**Files:**
- Create: `frontend/src/pages/AnalyticsPage.tsx`
- Create: `frontend/src/pages/AnalyticsPage.test.tsx`
- Modify: `frontend/src/index.css`

**Interfaces:**
- Consumes `fetchDiarySummaryRange`, `fetchWeightHistory`, `dateDaysAgo`, `buildWeightTrend`, `buildMacroSummaries`, `buildMicronutrientRows`, `Sparkline`, `StackedBar`, and `GroupedBars`.
- Produces `/analytics` from the analytics mockup.

- [ ] **Step 1: Write failing analytics test**

Add `frontend/src/pages/AnalyticsPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import AnalyticsPage from './AnalyticsPage'
import * as client from '../api/client'

describe('AnalyticsPage', () => {
  it('renders weight, macro, vitamin, mineral, and insight cards', async () => {
    vi.spyOn(client, 'fetchDiarySummaryRange').mockResolvedValue([
      {
        date: '2026-07-22',
        totals: [
          { code: 'protein', amount: 92, unit: 'g', target: 100 },
          { code: 'carbohydrates', amount: 210, unit: 'g', target: 250 },
          { code: 'fat', amount: 60, unit: 'g', target: 70 },
          { code: 'vitamin_d', amount: 5, unit: 'ug', target: 15 },
          { code: 'calcium', amount: 700, unit: 'mg', target: 1000 },
        ],
        water: { amountMl: 1800, targetMl: 2500 },
      },
    ])
    vi.spyOn(client, 'fetchWeightHistory').mockResolvedValue([
      { id: 'w1', weightKg: 72.3, measuredAt: '2026-07-16T08:00:00Z' },
      { id: 'w2', weightKg: 71.9, measuredAt: '2026-07-22T08:00:00Z' },
    ])

    renderWithClient(<AnalyticsPage />)

    expect(await screen.findByRole('heading', { name: 'Analytics' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Weight trend' })).toBeInTheDocument()
    expect(screen.getByText('Macro balance')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Vitamins' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Minerals' })).toBeInTheDocument()
    expect(screen.getByText(/Vitamin D/i)).toBeInTheDocument()
  })
})

function renderWithClient(children: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}>{children}</QueryClientProvider>)
}
```

- [ ] **Step 2: Run test and confirm failure**

Run: `npm test -- --run src/pages/AnalyticsPage.test.tsx`

Expected: FAIL because `AnalyticsPage` does not exist.

- [ ] **Step 3: Implement analytics page**

Create `frontend/src/pages/AnalyticsPage.tsx`:

```tsx
import { useQuery } from '@tanstack/react-query'
import { fetchDiarySummaryRange, fetchWeightHistory } from '../api/client'
import { buildMacroSummaries, buildMicronutrientRows, buildWeightTrend, dateDaysAgo } from '../diary/nutritionDashboard'
import { formatLocalDate } from '../diary/formatDay'
import { DashboardCard, MetricPill } from '../ui/Card'
import { GroupedBars, Sparkline, StackedBar } from '../ui/MiniCharts'

export default function AnalyticsPage() {
  const to = formatLocalDate()
  const from = dateDaysAgo(6)
  const rangeQuery = useQuery({ queryKey: ['diary-summary-range', from, to], queryFn: () => fetchDiarySummaryRange(from, to) })
  const weightQuery = useQuery({ queryKey: ['weight-history'], queryFn: () => fetchWeightHistory({ from, to }) })

  const latest = rangeQuery.data?.at(-1)
  const macros = latest ? buildMacroSummaries(latest.totals) : []
  const vitamins = latest ? buildMicronutrientRows(latest.totals, 'vitamin') : []
  const minerals = latest ? buildMicronutrientRows(latest.totals, 'mineral') : []
  const lowVitamin = vitamins.find((item) => item.percent > 0 && item.percent < 60)
  const proteinPercent = macros.find((item) => item.code === 'protein')?.percent ?? 0

  return (
    <main className="mobile-page analytics-page">
      <div className="mobile-hero">
        <p className="sheet-kicker">This week</p>
        <h1>Analytics</h1>
        <p>{from} to {to}</p>
      </div>

      {(rangeQuery.isLoading || weightQuery.isLoading) && <p>Loading analytics...</p>}
      {[rangeQuery.error, weightQuery.error].filter(Boolean).map((error, index) => (
        <p className="error" key={index}>{(error as Error).message}</p>
      ))}

      <DashboardCard title="Weight trend" eyebrow="Goal progress">
        <Sparkline label="Weight trend" points={buildWeightTrend(weightQuery.data ?? [])} />
      </DashboardCard>

      <DashboardCard title="Macros" eyebrow="Weekly balance">
        <StackedBar label="Macro balance" segments={macros.map((macro) => ({ label: macro.label, percent: macro.percent }))} />
      </DashboardCard>

      <DashboardCard title="Vitamins" eyebrow="Latest day">
        <GroupedBars label="Vitamins" groups={vitamins} />
      </DashboardCard>

      <DashboardCard title="Minerals" eyebrow="Latest day">
        <GroupedBars label="Minerals" groups={minerals} />
      </DashboardCard>

      <DashboardCard title="Insights" eyebrow="Signals">
        <div className="insight-grid">
          <MetricPill label="Protein" value={proteinPercent >= 80 ? 'On target' : 'Needs focus'} tone="green" />
          <MetricPill label={lowVitamin?.label ?? 'Vitamins'} value={lowVitamin ? 'Low' : 'Steady'} tone="amber" />
        </div>
      </DashboardCard>
    </main>
  )
}
```

- [ ] **Step 4: Add analytics CSS**

Append to `frontend/src/index.css`:

```css
.insight-grid {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.stacked-track {
  display: flex;
  margin-top: 0.75rem;
}

.stacked-track span:nth-child(1) {
  background: var(--leaf);
}

.stacked-track span:nth-child(2) {
  background: var(--sun);
}

.stacked-track span:nth-child(3) {
  background: #8d79b8;
}

.stacked-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.55rem;
  color: #5d7166;
  font-size: 0.85rem;
}
```

- [ ] **Step 5: Verify and commit**

Run: `npm test -- --run src/pages/AnalyticsPage.test.tsx`

Expected: PASS.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

Commit:

```bash
git add frontend/src/pages/AnalyticsPage.tsx frontend/src/pages/AnalyticsPage.test.tsx frontend/src/index.css
git commit -m "feat: add mobile nutrition analytics"
```

---

### Task 7: Responsive polish, profile alignment, and final verification

**Files:**
- Modify: `frontend/src/pages/ProfilePage.tsx`
- Modify: `frontend/src/index.css`
- Modify: `AI/mobile-ui-mockups.md`

**Interfaces:**
- Keeps profile forms and weight logging behavior intact.
- Produces desktop responsive polish that follows the desktop reference mockups without changing mobile-first priority.

- [ ] **Step 1: Update profile layout**

In `frontend/src/pages/ProfilePage.tsx`:

- Change `<main className="panel profile-panel">` to `<main className="mobile-page profile-panel">`.
- Wrap existing profile, weight, and daily goals sections with `dashboard-card` styling by changing `className="diary-card"` to `className="dashboard-card"`.
- Keep all existing form IDs and submit handlers unchanged.

- [ ] **Step 2: Add responsive CSS**

Append to `frontend/src/index.css`:

```css
@media (min-width: 760px) {
  .mobile-page {
    padding: 0 1.5rem 2rem;
  }

  .dashboard-page,
  .analytics-page {
    width: min(1100px, 100%);
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
  }

  .dashboard-page .mobile-hero,
  .analytics-page .mobile-hero,
  .dashboard-page .dashboard-card:first-of-type,
  .analytics-page .dashboard-card:first-of-type {
    grid-column: 1 / -1;
  }

  .diary-page,
  .profile-panel {
    width: min(860px, 100%);
  }
}
```

- [ ] **Step 3: Update AI notes**

Append to `AI/mobile-ui-mockups.md`:

```md
## Implementation plan

- Plan saved at `docs/superpowers/plans/2026-07-22-mobile-nutrition-ui.md`.
- The implementation should prioritize `/today` as the dashboard, `/diary` as the food timeline, and `/analytics` as the graph/insights view.
- Charts should remain lightweight CSS/SVG components unless the app later needs advanced gestures or chart accessibility features.
```

- [ ] **Step 4: Run full frontend verification**

Run: `npm test`

Expected: all Vitest suites pass.

Run: `npm run build`

Expected: TypeScript and Vite build complete.

- [ ] **Step 5: Manual mobile verification**

Run the app:

```bash
npm run dev -- --host 0.0.0.0
```

Use mobile viewport manual testing through the browser:

- Log in through the existing dev/auth flow.
- Open `/today`; verify dashboard cards match the mobile dashboard mockup direction.
- Open `/diary`; verify meal timeline, add food action, water actions, vitamins, and minerals remain usable at 360px width.
- Open `/analytics`; verify weight, macro, vitamin, mineral, and insight cards are visible and readable at 360px width.
- Open `/me`; verify profile and weight logging are not regressed.
- Save a short walkthrough video showing `/today`, `/diary`, and `/analytics`.

- [ ] **Step 6: Commit final polish**

Commit:

```bash
git add frontend/src/pages/ProfilePage.tsx frontend/src/index.css AI/mobile-ui-mockups.md
git commit -m "style: polish mobile nutrition ui"
```

---

## Final delivery checklist

- [ ] `npm test` passes in `frontend/`.
- [ ] `npm run build` passes in `frontend/`.
- [ ] Manual mobile walkthrough verifies `/today`, `/diary`, `/analytics`, and `/me`.
- [ ] Walkthrough video is saved under `/opt/cursor/artifacts`.
- [ ] `AI/mobile-ui-mockups.md` records implementation notes.
- [ ] Branch is pushed with `git push -u origin <branch-name>`.
- [ ] Pull request is created or updated.

## Self-review

- Spec coverage: dashboard, analytics, diary, weight, macros, vitamins, minerals, mobile navigation, responsive desktop references, tests, and AI notes are covered.
- Placeholder scan: no task contains unresolved placeholder markers; each task has exact file paths and concrete commands.
- Type consistency: exported component/helper names are defined before consumers use them; route names are consistent across navigation and `App.tsx`.
- Risk note: nutrient code naming depends on seeded backend nutrient codes. If real API data uses different vitamin/mineral codes, update `vitaminCodes` and `mineralCodes` in `frontend/src/diary/nutritionDashboard.ts` with a focused helper test in Task 2.

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-22-mobile-nutrition-ui.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - execute tasks in this session using `superpowers:executing-plans`, batch execution with checkpoints.
