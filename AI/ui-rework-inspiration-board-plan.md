# UI Rework Plan: Inspiration Board Direction

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework NutriTrack's current mobile-first UI so the app feels closer to the new inspiration board: bright, focused, tactile, health-dashboard-like, and optimized around fast food logging plus immediately readable nutrition status.

**Architecture:** Keep the current React/Vite route structure and data-fetching model. Rework the presentation layer through shared UI primitives, chart primitives, screen view components, and `index.css`, using existing dev preview routes as the visual QA harness before touching authenticated flows.

**Tech Stack:** React, TypeScript, React Router, TanStack Query, Vite, CSS/SVG chart primitives, Vitest, Testing Library, Capacitor Android.

## Sources Reviewed

- New inspiration screenshots in `Insperation/`.
- Existing UI notes:
  - `AI/mobile-ui-mockups.md`
  - `AI/mobile-nav-track-food-fab.md`
  - `AI/settings-profile-menu.md`
- Current frontend structure from Codebase Memory.
- Current source surfaces:
  - `frontend/src/App.tsx`
  - `frontend/src/navigation/AppNavigation.tsx`
  - `frontend/src/screens/DashboardView.tsx`
  - `frontend/src/screens/DiaryView.tsx`
  - `frontend/src/screens/AnalyticsView.tsx`
  - `frontend/src/ui/Card.tsx`
  - `frontend/src/ui/MiniCharts.tsx`
  - `frontend/src/index.css`
  - `frontend/src/pages/preview/*`

## Product Assumptions

- Mobile remains the primary surface; desktop should be functional and tidy, but the design language is mobile-led.
- Food logging is the primary daily action, so `/lookup` keeps the centered bottom-nav action.
- `/today`, `/diary`, `/analytics`, `/lookup`, and `/settings` remain the main authenticated destinations.
- The rework should not change backend contracts, nutrition calculations, route permissions, onboarding rules, or moderation behavior.
- Dev preview routes stay available in development and become the main screenshot/visual-regression workflow.

## Inspiration Board Takeaways

The board mixes two strong directions:

- **Cronometer-style dark density:** dark panels, bottom navigation, report tabs, circular calorie/macro summaries, and many compact data cards.
- **Modern light health-card UI:** white cards on a soft gray or pale green canvas, large numbers, high-radius cards, subtle shadows, rings, pill tabs, friendly icons, and compact meal rows.

Use the light health-card direction as the primary NutriTrack style. Borrow from the dark Cronometer references only where they improve information architecture: bottom tabs, quick-add behavior, compact report summaries, and dense macro/nutrient breakdowns.

## Visual Direction

- **Canvas:** soft off-white or pale warm gray app background, with cards floating above it through subtle elevation rather than heavy borders.
- **Cards:** rounded rectangles with large radii, generous internal spacing, low-contrast shadows, and clear hierarchy between hero cards, metric cards, and list rows.
- **Typography:** bold, large numeric values first; labels stay short and muted. Avoid dense explanatory copy inside daily-use cards.
- **Color:** keep nutrition meaning consistent:
  - Calories/action: orange.
  - Protein/health success: green.
  - Carbs: yellow or blue depending on existing macro mapping.
  - Fat: red or warm coral.
  - Weight/body metrics: mint, blue, or lavender.
- **Charts:** prefer rings, compact bars, small sparklines, and range bands. Avoid complex chart chrome unless it directly helps the user decide what to do.
- **Icons:** use simple food/body/action icons as recognition aids, not decorative clutter.
- **Navigation:** preserve bottom nav with a centered add button, but tune it toward the board's floating capsule style.

## Target Experience

### `/today`: Daily Command Center

`/today` should answer, in order:

1. How much can I still eat today?
2. Are my macros roughly on track?
3. What did I log recently?
4. Is there one health trend worth noticing?

Planned layout:

- Top date strip with the current week and active day, inspired by the Cal AI and light food diary screenshots.
- Hero calorie card with a large "calories left" number and a right-side ring.
- Three compact macro cards below the hero: protein left, carbs left, fat left.
- Recently logged list with larger, image-capable rows and macro chips.
- Optional small insight card for weight, streak, or water if the data is available.

Implementation consequence:

- Refactor `DashboardView` away from the current four-card dashboard stack and toward a single daily command flow.
- Keep current nutrition-dashboard helpers, but expose values as "left" and "target progress" where the data supports it.

### `/diary`: Food Timeline

`/diary` should feel like a detailed log, not a second dashboard.

Planned layout:

- Header with date navigation using compact day pills.
- Calorie/macro summary card at the top, matching `/today` but smaller.
- Meal sections as clean white rows with optional emoji/icon thumbnail, name, time, calories, and macro chips.
- Keep per-meal add actions, but make them lighter than the global bottom-nav add button.
- Empty meal sections should feel useful: short copy plus a single action.

Implementation consequence:

- Keep `DiaryView` as the main renderer.
- Replace heavy nested `DashboardCard` treatment around meals with direct list sections and reusable meal row styling.
- Keep delete behavior and accessibility names intact.

### `/analytics`: Insights And Trends

`/analytics` should become a health-insights grid similar to the board's "Insights" and "Biology" references.

Planned layout:

- Header with "Insights" or "Analytics" and an edit/filter affordance.
- Date-range selector as a compact pill control.
- Mixed card grid:
  - Streak/status card.
  - Weight card with sparkline.
  - Macro averages card with stacked bars.
  - Calories goal card.
  - Micronutrient cards for vitamins and minerals.
- Cards should contain one dominant fact and one lightweight chart.

Implementation consequence:

- Keep `AnalyticsView` data inputs.
- Recompose existing macro, weight, micronutrient, and insight data into smaller reusable metric cards.
- Consider renaming visible copy to "Insights" while leaving route `/analytics` unchanged.

### `/lookup`: Primary Add Flow

The inspiration board treats adding food as the most important action. NutriTrack should do the same.

Planned layout:

- Keep `/lookup` as the centered bottom-nav destination.
- Rework the lookup screen into a quick-action surface:
  - Search food.
  - Scan barcode.
  - Recently used or recent results.
  - Create/submit product as a secondary path.
- Mobile should prioritize one-handed use and large touch targets.

Implementation consequence:

- Review `LookupPage` after the main dashboard primitives are updated.
- Reuse the new card, button, and list row primitives rather than custom lookup-only styling.

### `/settings`: Quiet Utility

Settings should adopt the same card language but remain less expressive than food and analytics surfaces.

Planned layout:

- Keep the existing settings hub and subsection routes.
- Style section rows as calm white cards with icons and concise secondary labels.
- Keep account/sign-out visually separated.

Implementation consequence:

- Update settings styles after the shared primitives land.
- Avoid changing form behavior or route structure.

## Implementation status

- Branch: `cursor/ui-rework-inspiration-board`
- Worktree: `.worktrees/ui-rework-inspiration-board`
- Phase 1 complete: tokens + shared card/chart primitives
- Phase 2 complete: floating bottom nav with orange Track food FAB
- Phase 3 started/complete for first pass: `/today` daily command center
- Remaining: diary, analytics, lookup/settings polish, visual QA

## Implementation Phases

### Phase 1: Design Tokens And Shared Primitives

**Files:**

- Modify: `frontend/src/index.css`
- Modify: `frontend/src/ui/Card.tsx`
- Modify: `frontend/src/ui/MiniCharts.tsx`
- Modify: `frontend/src/ui/Card.test.tsx`
- Modify: `frontend/src/ui/MiniCharts.test.tsx`

**Tasks:**

- [x] Define the new palette, spacing, card radius, shadow, and macro tone variables in `index.css`.
- [x] Extend `DashboardCard` into a more general card primitive that supports hero, metric, list, and insight densities.
- [x] Add or adapt chart primitives for calorie rings, macro mini-rings, thin progress bars, compact sparklines, and range-band charts.
- [x] Keep chart components accessible with `role="progressbar"` or `role="img"` and useful labels.
- [x] Update component tests for accessible labels, bounded percentages, and tone/class behavior.

**Acceptance:**

- Existing card/chart tests pass.
- No screen-specific rework is required to use the new primitive defaults.
- The shared primitives visually support both hero cards and compact metric cards.

### Phase 2: Mobile Shell And Navigation

**Files:**

- Modify: `frontend/src/navigation/AppNavigation.tsx`
- Modify: `frontend/src/navigation/AppNavigation.test.tsx`
- Modify: `frontend/src/index.css`

**Tasks:**

- [x] Tune the bottom nav into a floating capsule matching the inspiration board.
- [x] Keep the centered `/lookup` action with accessible name `Track food`.
- [ ] Consider shortening visible mobile labels from `Dashboard` to `Home` and `Analytics` to `Insights` only if tests and UX copy are updated together.
- [x] Preserve desktop navigation behavior.
- [x] Test the active state and `/lookup` FAB target.

**Acceptance:**

- Bottom-nav links remain keyboard/focus accessible.
- Desktop top nav remains visible at desktop breakpoints.
- `/lookup` remains one tap from every authenticated mobile surface.

### Phase 3: Rework `/today`

**Files:**

- Modify: `frontend/src/screens/DashboardView.tsx`
- Modify: `frontend/src/pages/DashboardPage.test.tsx`
- Modify: `frontend/src/pages/preview/PreviewDashboardPage.tsx`
- Modify: `frontend/src/index.css`

**Tasks:**

- [x] Convert the top section into a date-aware daily command center.
- [x] Show calories left as the primary value when a target exists; fall back to calories consumed when it does not.
- [x] Add three macro-left cards for protein, carbs, and fat.
- [ ] Add a recently logged section if current-day entries are made available to this screen; otherwise keep this as a future integration note in the plan and do not fake data in production.
- [x] Keep weight or micronutrient insight as a secondary card.
- [x] Update preview data to cover normal, low-progress, and over-target states.

**Acceptance:**

- `/preview/dashboard` communicates the day status within the first viewport.
- Tests verify headline values, macro labels, and accessible chart labels.
- The screen still works when targets or weight history are missing.

### Phase 4: Rework `/diary`

**Files:**

- Modify: `frontend/src/screens/DiaryView.tsx`
- Modify: `frontend/src/pages/DiaryPage.test.tsx`
- Modify: `frontend/src/pages/preview/PreviewDiaryPage.tsx`
- Modify: `frontend/src/index.css`

**Tasks:**

- [ ] Replace the current stacked-card diary with a lighter food timeline.
- [ ] Add a compact date strip or day-pill navigation.
- [ ] Convert meal entries into image-ready rows with calories and macro chips.
- [ ] Keep delete buttons available and accessible.
- [ ] Keep per-meal add links, but visually subordinate them to the global add action.
- [ ] Add preview examples for full, sparse, and empty meals.

**Acceptance:**

- `/preview/diary` reads as a food log, not an analytics dashboard.
- Tests cover previous/next day controls, meal add links, delete behavior, and empty meal copy.
- Long product names wrap without breaking row actions.

### Phase 5: Rework `/analytics`

**Files:**

- Modify: `frontend/src/screens/AnalyticsView.tsx`
- Modify: `frontend/src/pages/AnalyticsPage.test.tsx`
- Modify: `frontend/src/pages/preview/PreviewAnalyticsPage.tsx`
- Modify: `frontend/src/index.css`

**Tasks:**

- [ ] Recompose analytics into an insight-card grid.
- [ ] Keep weight, macro balance, vitamins, minerals, and generated insight states.
- [ ] Use one dominant metric per card.
- [ ] Add compact charts directly inside cards.
- [ ] Keep date-range navigation and disabled-next behavior.
- [ ] Update preview data to show good, improving, and needs-focus states.

**Acceptance:**

- `/preview/analytics` matches the board's health-summary feel.
- Tests verify date-range controls, major insight cards, and disabled next-range behavior.
- Charts remain readable at mobile width.

### Phase 6: Rework `/lookup` And Settings Polish

**Files:**

- Modify: `frontend/src/pages/LookupPage.tsx`
- Modify: `frontend/src/pages/LookupPage.test.tsx`
- Modify: `frontend/src/pages/SettingsHomePage.tsx`
- Modify: `frontend/src/pages/SettingsHomePage.test.tsx`
- Modify: `frontend/src/pages/settings/*`
- Modify: `frontend/src/index.css`

**Tasks:**

- [ ] Rework lookup into a quick-add hub with search, scan, recent/reused affordances, and secondary submit-product path.
- [ ] Keep native scan behavior and barcode sanitization behavior unchanged.
- [ ] Apply the new list-row/card styling to settings hub rows.
- [ ] Keep profile, goals, weight, account, sign-out, and redirect behavior unchanged.
- [ ] Update tests around labels and actions that change visible copy.

**Acceptance:**

- `/preview/lookup` or dev-local lookup can be visually checked without changing API behavior.
- Settings remains calm, scannable, and route-compatible.
- Existing tests for lookup and settings behavior pass.

### Phase 7: Visual QA, Accessibility, And Android Check

**Files:**

- Modify only files needed for defects found during QA.

**Tasks:**

- [ ] Run frontend tests.
- [ ] Run lint/typecheck if available in the frontend package scripts.
- [ ] Inspect `/preview/dashboard`, `/preview/diary`, `/preview/analytics`, and `/preview/lookup` in a mobile viewport.
- [ ] Run the Impeccable detector after UI edits: `node C:\Users\Administrator\.cursor\skills\impeccable\scripts/detect.mjs --json frontend/src/App.tsx frontend/src/index.css frontend/src/screens frontend/src/pages frontend/src/ui frontend/src/navigation`.
- [ ] Check Android safe areas and bottom-nav spacing in the Capacitor shell before release.
- [ ] Update this file or a follow-up `AI/` note with final decisions and screenshots.

**Acceptance:**

- Tests pass.
- No obvious tap target, contrast, or safe-area regressions remain.
- Authenticated routes still redirect through onboarding exactly as before.
- Food logging remains reachable from the bottom-nav action.

## Testing Strategy

- Unit/component tests:
  - `frontend/src/ui/Card.test.tsx`
  - `frontend/src/ui/MiniCharts.test.tsx`
  - `frontend/src/navigation/AppNavigation.test.tsx`
  - Page tests for dashboard, diary, analytics, lookup, and settings.
- Visual QA:
  - Use dev preview routes for stable mock data.
  - Verify mobile viewport first, then desktop.
  - Capture before/after screenshots for `/preview/dashboard`, `/preview/diary`, `/preview/analytics`, and `/preview/lookup`.
- Accessibility:
  - Verify focus states for bottom nav, FAB, date controls, delete buttons, and settings rows.
  - Keep chart values available through accessible labels.
  - Do not rely on color alone for macro or status meaning.

## Risks And Guardrails

- **Risk:** The current app has a prior mockup implementation that may resist a clean visual pivot if only CSS is changed.
  - **Guardrail:** Recompose the three main screen views instead of only restyling classes.
- **Risk:** `/today` does not currently receive diary entries, so "recently logged" may need data flow changes.
  - **Guardrail:** Do not fake recent foods in production. Either pass current-day entries into `DashboardView` from `DashboardPage` or defer that section until the data is available.
- **Risk:** The board includes both dark and light directions.
  - **Guardrail:** Use light as the primary visual system; use dark references only for density and navigation ideas.
- **Risk:** Large CSS edits can cause regressions across settings, login, moderation, and product pages.
  - **Guardrail:** Introduce reusable class families and verify non-target routes after each phase.
- **Risk:** Mobile bottom nav may collide with Android safe areas.
  - **Guardrail:** Test Capacitor Android spacing and preserve enough bottom padding on scrollable pages.

## Out Of Scope

- Backend API changes.
- New nutrition calculations.
- Paid charting libraries.
- Replacing React Router, TanStack Query, or the current auth/onboarding flow.
- Dark mode as a first-pass requirement.
- Full desktop redesign beyond responsive cleanup.

## Recommended Execution Order

1. Shared primitives and tokens.
2. Bottom navigation shell.
3. `/today`.
4. `/diary`.
5. `/analytics`.
6. `/lookup` and settings polish.
7. Visual QA, accessibility, Android safe-area pass, and docs update.

This order keeps the highest-risk visual language decisions early, then applies them screen by screen with preview routes and tests available at each step.
