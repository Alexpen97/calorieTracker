# Fix: SVG circle `title` TypeScript build error

## Problem

Frontend Docker/Railway build failed:

```
src/ui/MiniCharts.tsx(269,13): error TS2322:
Property 'title' does not exist on type 'SVGProps<SVGCircleElement>'.
```

React 19 `@types/react` no longer allows the HTML `title` attribute on SVG
elements.

## Fix

- Use a nested SVG `<title>` child on the weight-trend hit circle (browser
  tooltip + SVG a11y annotation).
- Add `aria-label` for keyboard/screen-reader focus.
- Update `MiniCharts.test.tsx` to assert via `getByLabelText` + `<title>` text.
