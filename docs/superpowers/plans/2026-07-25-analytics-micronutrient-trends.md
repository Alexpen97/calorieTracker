# Analytics Micronutrient 30-Day Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Analytics vitamin/mineral average bars with 30-day micronutrient line graphs.

**Architecture:** Extend the existing diary summary range fetch to 30 days, build per-nutrient daily percent series in `nutritionDashboard.ts`, and render small-multiple SVG line charts via `MiniCharts` on `AnalyticsView`.

**Tech Stack:** React 19, Vitest, existing custom SVG charts (no new chart library).

## Global Constraints

- Keep the existing vitamin/mineral checklist order and codes.
- Prefer project-local SVG patterns over new dependencies.
- Dashboard remains today-only progress bars.
- Insights may continue using range averages.

---

### Task 1: Trend series helper

**Files:**
- Modify: `frontend/src/diary/nutritionDashboard.ts`
- Test: `frontend/src/diary/nutritionDashboard.test.ts`

- [x] Add `buildMicronutrientTrendSeries`
- [x] Cover gaps, capping, and 30-day length in tests

### Task 2: Analytics range + charts

**Files:**
- Modify: `frontend/src/pages/AnalyticsPage.tsx`
- Modify: `frontend/src/screens/AnalyticsView.tsx`
- Modify: `frontend/src/ui/MiniCharts.tsx`
- Modify: `frontend/src/index.css`
- Test: `frontend/src/pages/AnalyticsPage.test.tsx`

- [x] Fetch last 30 days
- [x] Render `MicroTrendGrid` for vitamins and minerals
- [x] Update page tests and preview data
