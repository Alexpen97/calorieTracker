import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchProductByBarcode } from '../api/client'
import { isValidBarcode, sanitizeBarcodeInput } from '../food/barcode'

type BarcodeDetectorLike = {
  detect: (source: ImageBitmapSource) => Promise<Array<{ rawValue: string }>>
}

declare global {
  interface Window {
    BarcodeDetector?: new (options?: { formats?: string[] }) => BarcodeDetectorLike
  }
}

export default function LookupPage() {
  const navigate = useNavigate()
  const [barcode, setBarcode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [scanSupported, setScanSupported] = useState(false)
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    setScanSupported(typeof window !== 'undefined' && typeof window.BarcodeDetector === 'function')
    return () => stopScan()
  }, [])

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
      navigate(`/products/${product.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lookup failed')
    } finally {
      setBusy(false)
    }
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault()
    await lookup(barcode)
  }

  async function startScan() {
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
          const value = codes[0]?.rawValue
          if (value && isValidBarcode(value)) {
            stopScan()
            setBarcode(sanitizeBarcodeInput(value))
            await lookup(value)
            return
          }
        } catch {
          // Keep scanning on transient detect errors.
        }
        rafRef.current = requestAnimationFrame(() => {
          void tick()
        })
      }
      rafRef.current = requestAnimationFrame(() => {
        void tick()
      })
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

  return (
    <main className="panel lookup-panel">
      <h2>Look up a food</h2>
      <p>Scan a barcode with your camera or type the EAN/UPC digits.</p>
      <form className="lookup-form" onSubmit={onSubmit}>
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
          {scanSupported && !scanning && (
            <button className="btn btn-secondary" type="button" onClick={() => void startScan()}>
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
      <div className={`scanner-frame ${scanning ? 'is-active' : ''}`}>
        <video ref={videoRef} muted playsInline className="scanner-video" />
        {!scanning && (
          <p className="scanner-hint">
            {scanSupported
              ? 'Camera preview appears here when scanning.'
              : 'This browser has no BarcodeDetector — use manual entry.'}
          </p>
        )}
      </div>
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
