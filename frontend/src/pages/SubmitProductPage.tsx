import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createProductSubmission, type ProductNutrient } from '../api/client'

const DEFAULT_NUTRIENTS: Array<{ code: string; label: string; unit: string }> = [
  { code: 'energy_kcal', label: 'Energy', unit: 'kcal' },
  { code: 'protein', label: 'Protein', unit: 'g' },
  { code: 'fat', label: 'Fat', unit: 'g' },
  { code: 'carbohydrates', label: 'Carbohydrates', unit: 'g' },
  { code: 'sugars', label: 'Sugars', unit: 'g' },
  { code: 'fiber', label: 'Fiber', unit: 'g' },
  { code: 'salt', label: 'Salt', unit: 'g' },
]

export default function SubmitProductPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [brand, setBrand] = useState('')
  const [barcode, setBarcode] = useState('')
  const [servingSizeG, setServingSizeG] = useState('')
  const [amounts, setAmounts] = useState<Record<string, string>>({})
  const [force, setForce] = useState(false)
  const [warnings, setWarnings] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setWarnings([])
    if (!name.trim()) {
      setError('Name is required.')
      return
    }
    const nutrients: ProductNutrient[] = DEFAULT_NUTRIENTS.flatMap((n) => {
      const raw = amounts[n.code]
      if (raw == null || raw.trim() === '') {
        return []
      }
      const amount = Number(raw)
      if (!Number.isFinite(amount) || amount <= 0) {
        return []
      }
      return [{ code: n.code, amountPer100g: amount, unit: n.unit }]
    })
    if (nutrients.length === 0) {
      setError('Enter at least one nutrient amount per 100 g.')
      return
    }
    setBusy(true)
    try {
      const submission = await createProductSubmission({
        name: name.trim(),
        brand: brand.trim() || undefined,
        barcode: barcode.trim() || undefined,
        servingSizeG: servingSizeG ? Number(servingSizeG) : undefined,
        nutrients,
        force,
      })
      navigate(`/products/${submission.id}`)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Submit failed'
      const warningMatch = message.match(/warnings[^\[]*(\[[\s\S]*\])/i)
      if (message.includes('409') || message.toLowerCase().includes('duplicate')) {
        setWarnings([message])
        setForce(true)
      } else if (warningMatch) {
        setWarnings([message])
        setForce(true)
      }
      setError(message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="panel">
      <p className="crumb">
        <Link to="/lookup">← Look up</Link>
      </p>
      <h2>Submit a product</h2>
      <p>
        Your submission is usable by you immediately and stays private until a moderator approves it
        for the shared catalog.
      </p>
      <form className="lookup-form" onSubmit={(e) => void onSubmit(e)}>
        <label htmlFor="sub-name">Name</label>
        <input id="sub-name" value={name} onChange={(e) => setName(e.target.value)} required />
        <label htmlFor="sub-brand">Brand</label>
        <input id="sub-brand" value={brand} onChange={(e) => setBrand(e.target.value)} />
        <label htmlFor="sub-barcode">Barcode (optional)</label>
        <input id="sub-barcode" value={barcode} onChange={(e) => setBarcode(e.target.value)} />
        <label htmlFor="sub-serving">Serving size (g, optional)</label>
        <input
          id="sub-serving"
          type="number"
          min="1"
          value={servingSizeG}
          onChange={(e) => setServingSizeG(e.target.value)}
        />
        <h3>Nutrition per 100 g</h3>
        {DEFAULT_NUTRIENTS.map((n) => (
          <div key={n.code}>
            <label htmlFor={`n-${n.code}`}>
              {n.label} ({n.unit})
            </label>
            <input
              id={`n-${n.code}`}
              type="number"
              min="0"
              step="any"
              value={amounts[n.code] ?? ''}
              onChange={(e) => setAmounts((prev) => ({ ...prev, [n.code]: e.target.value }))}
            />
          </div>
        ))}
        {force && (
          <label>
            <input type="checkbox" checked={force} onChange={(e) => setForce(e.target.checked)} />{' '}
            Submit anyway despite possible duplicates
          </label>
        )}
        <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? 'Submitting…' : 'Submit product'}
          </button>
        </div>
      </form>
      {warnings.length > 0 && (
        <ul className="error">
          {warnings.map((w) => (
            <li key={w}>{w}</li>
          ))}
        </ul>
      )}
      {error && <p className="error">{error}</p>}
    </main>
  )
}
