# Lookup (Add Food) design iteration: method navbar + default barcode

## Change

Iterated on the Add Food / Lookup screen to make the add-method choice feel
like a small navbar and to default the flow to barcode entry.

## UX

- Method navbar: **Barcode** | **Search** | **Add your own**
- Default selection: **Barcode**
- Switching tabs swaps the visible form fields without navigation.
- Leaving Barcode mode stops any active scan session.

## Files

- `frontend/src/pages/LookupPage.tsx`
- `frontend/src/index.css`
- `frontend/src/pages/LookupPage.test.tsx`

