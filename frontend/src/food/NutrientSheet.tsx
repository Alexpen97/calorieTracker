import { useQuery } from '@tanstack/react-query'
import { fetchNutrient } from '../api/client'

type Props = {
  code: string
  onClose: () => void
}

export default function NutrientSheet({ code, onClose }: Props) {
  const { data, error, isLoading } = useQuery({
    queryKey: ['nutrient', code],
    queryFn: () => fetchNutrient(code),
  })

  return (
    <div className="sheet-backdrop" role="presentation" onClick={onClose}>
      <aside
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="nutrient-sheet-title"
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" className="sheet-close" onClick={onClose} aria-label="Close">
          Close
        </button>
        {isLoading && <p>Loading…</p>}
        {error && <p className="error">{(error as Error).message}</p>}
        {data && (
          <>
            <p className="sheet-kicker">{data.category}</p>
            <h3 id="nutrient-sheet-title">{data.displayName}</h3>
            {data.description && <p>{data.description}</p>}
            <dl className="meta">
              {data.bodyEffects && (
                <>
                  <dt>In the body</dt>
                  <dd>{data.bodyEffects}</dd>
                </>
              )}
              {data.deficiencyEffects && (
                <>
                  <dt>If too low</dt>
                  <dd>{data.deficiencyEffects}</dd>
                </>
              )}
              {data.excessEffects && (
                <>
                  <dt>If too high</dt>
                  <dd>{data.excessEffects}</dd>
                </>
              )}
              {data.commonSources && (
                <>
                  <dt>Common sources</dt>
                  <dd>{data.commonSources}</dd>
                </>
              )}
            </dl>
            <p className="disclaimer">
              Educational content only — not medical advice.
              {data.contentSource && (
                <>
                  {' '}
                  Source:{' '}
                  <a href={data.contentSource} target="_blank" rel="noreferrer">
                    citation
                  </a>
                  .
                </>
              )}
            </p>
          </>
        )}
      </aside>
    </div>
  )
}
