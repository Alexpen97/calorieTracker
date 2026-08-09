import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { fetchProductByBarcode, searchProducts, type Product } from '../api/client'
import { isValidBarcode, sanitizeBarcodeInput } from '../food/barcode'
import { parseMealTypeParam, productPathWithMeal } from '../diary/formatDay'
import {
  isNativeBarcodeScanAvailable,
  scanBarcodeNative,
} from '../platform/barcodeScan'

type Mode = 'barcode' | 'search'

type BarcodeDetectorLike = {
  detect: (source: ImageBitmapSource) => Promise<Array<{ rawValue: string }>>
}

function nevoPathWithMeal(nevoCode: string, mealType: ReturnType<typeof parseMealTypeParam>): string {
  const path = `/nevo/${encodeURIComponent(nevoCode)}`
  return mealType ? `${path}?meal=${mealType}` : path
}

function resultPath(item: Product, mealType: ReturnType<typeof parseMealTypeParam>): string {
  if (item.source === 'NEVO' && item.nevoCode) {
    return nevoPathWithMeal(item.nevoCode, mealType)
  }
  return productPathWithMeal(item.id, mealType)
}

function resultMeta(item: Product): string {
  if (item.source === 'NEVO') {
    return ['NEVO', item.foodGroup].filter(Boolean).join(' · ')
  }
  return [item.brand, item.source === 'PENDING_SUBMISSION' ? 'awaiting review' : null]
    .filter(Boolean)
    .join(' · ')
}

declare global {
  interface Window {
    BarcodeDetector?: new (options?: { formats?: string[] }) => BarcodeDetectorLike
  }
}

export default function LookupPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const mealType = parseMealTypeParam(searchParams.get('meal'))
  const [mode, setMode] = useState<Mode>('barcode')
  const [barcode, setBarcode] = useState('')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<Product[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [webScanSupported, setWebScanSupported] = useState(false)
  const [nativeScanSupported, setNativeScanSupported] = useState(false)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    setWebScanSupported(typeof window !== 'undefined' && typeof window.BarcodeDetector === 'function')
    void isNativeBarcodeScanAvailable().then(setNativeScanSupported)
    return () => stopScan()
  }, [])

  useEffect(() => {
    if (mode !== 'barcode') {
      stopScan()
    }
  }, [mode])

  async function lookup(raw: string) {
    const cleaned = sanitizeBarcodeInput(raw)
    if (!isValidBarcode(cleaned)) {
      setError('Enter an 8–14 digit barcode.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const product = await fetchProductByBarcode(cleaned)
      navigate(productPathWithMeal(product.id, mealType))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lookup failed')
    } finally {
      setBusy(false)
    }
  }

  async function onBarcodeSubmit(event: React.FormEvent) {
    event.preventDefault()
    await lookup(barcode)
  }

  async function onSearchSubmit(event: React.FormEvent) {
    event.preventDefault()
    const q = query.trim()
    if (q.length < 2) {
      setError('Enter at least 2 characters to search.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const response = await searchProducts(q)
      setResults(response.items)
      if (response.items.length === 0) {
        setError('No products found. Try another name or add your own.')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
    } finally {
      setBusy(false)
    }
  }

  async function startNativeScan() {
    setError(null)
    setBusy(true)
    try {
      const raw = await scanBarcodeNative()
      if (!raw) {
        return
      }
      setBarcode(sanitizeBarcodeInput(raw))
      await lookup(raw)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Native scan failed')
    } finally {
      setBusy(false)
    }
  }

  async function startWebScan() {
    setError(null)
    if (!window.BarcodeDetector) {
      setError('Live scanning is not supported in this browser. Enter the barcode manually.')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      })
      streamRef.current = stream
      setScanning(true)
      const video = videoRef.current
      if (video) {
        video.srcObject = stream
        await video.play()
      }
      const detector = new window.BarcodeDetector({
        formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128'],
      })
      const tick = async () => {
        if (!videoRef.current || videoRef.current.readyState < 2) {
          rafRef.current = requestAnimationFrame(() => {
            void tick()
          })
          return
        }
        try {
          const codes = await detector.detect(videoRef.current)
          const raw = codes[0]?.rawValue
          if (raw) {
            stopScan()
            setBarcode(sanitizeBarcodeInput(raw))
            await lookup(raw)
            return
          }
        } catch {
          // keep scanning
        }
        rafRef.current = requestAnimationFrame(() => {
          void tick()
        })
      }
      void tick()
    } catch {
      setError('Camera permission denied or unavailable.')
      stopScan()
    }
  }

  function stopScan() {
    if (rafRef.current != null) {
      cancelAnimationFrame(rafRef.current)
      rafRef.current = null
    }
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (videoRef.current) {
      videoRef.current.srcObject = null
    }
    setScanning(false)
  }

  const showScanButton = nativeScanSupported || webScanSupported

  return (
    <main className="panel lookup-panel">
      <h2>Look up a food</h2>
      <p>Search by name, scan a barcode, or submit a product that is missing.</p>
      <nav className="lookup-method-nav" aria-label="Add method">
        <button
          className={`lookup-method-tab ${mode === 'barcode' ? 'is-active' : ''}`}
          type="button"
          aria-current={mode === 'barcode' ? 'page' : undefined}
          onClick={() => setMode('barcode')}
        >
          Barcode
        </button>
        <button
          className={`lookup-method-tab ${mode === 'search' ? 'is-active' : ''}`}
          type="button"
          aria-current={mode === 'search' ? 'page' : undefined}
          onClick={() => setMode('search')}
        >
          Search
        </button>
        <Link className="lookup-method-tab" to="/submit-product">
          Add your own
        </Link>
      </nav>

      {mode === 'search' ? (
        <form className="lookup-form" onSubmit={(e) => void onSearchSubmit(e)}>
          <label htmlFor="search-q">Product name</label>
          <input
            id="search-q"
            name="q"
            autoComplete="off"
            placeholder="e.g. oat milk"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
            <button className="btn btn-primary" type="submit" disabled={busy}>
              {busy ? 'Searching…' : 'Search'}
            </button>
          </div>
        </form>
      ) : (
        <form className="lookup-form" onSubmit={(e) => void onBarcodeSubmit(e)}>
          <label htmlFor="barcode">Barcode</label>
          <input
            id="barcode"
            name="barcode"
            inputMode="numeric"
            autoComplete="off"
            placeholder="e.g. 3017620422003"
            value={barcode}
            onChange={(e) => setBarcode(sanitizeBarcodeInput(e.target.value))}
          />
          <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
            <button className="btn btn-primary" type="submit" disabled={busy}>
              {busy ? 'Looking up…' : 'Find product'}
            </button>
            {showScanButton && !scanning && (
              <button
                className="btn btn-secondary"
                type="button"
                disabled={busy}
                onClick={() => void (nativeScanSupported ? startNativeScan() : startWebScan())}
              >
                Scan with camera
              </button>
            )}
            {scanning && (
              <button className="btn btn-secondary" type="button" onClick={stopScan}>
                Stop camera
              </button>
            )}
          </div>
        </form>
      )}

      {mode === 'barcode' && !nativeScanSupported && (
        <div className={`scanner-frame ${scanning ? 'is-active' : ''}`}>
          <video ref={videoRef} muted playsInline className="scanner-video" />
          {!scanning && (
            <p className="scanner-hint">
              {webScanSupported
                ? 'Camera preview appears here when scanning.'
                : 'This browser has no BarcodeDetector — use manual entry.'}
            </p>
          )}
        </div>
      )}

      {results.length > 0 && (
        <ul className="search-results">
          {results.map((item) => (
            <li key={`${item.source}-${item.id}`}>
              <Link to={resultPath(item, mealType)}>
                <strong>{item.name}</strong>
                <span>{resultMeta(item)}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {error && <p className="error">{error}</p>}
      <p className="attribution">
        Product data from{' '}
        <a href="https://world.openfoodfacts.org" target="_blank" rel="noreferrer">
          Open Food Facts
        </a>
        .
      </p>
    </main>
  )
}
