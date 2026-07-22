import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  approveSubmission,
  fetchMe,
  fetchSubmissionQueue,
  rejectSubmission,
} from '../api/client'
import { useState } from 'react'

export default function ModerationPage() {
  const queryClient = useQueryClient()
  const [note, setNote] = useState<Record<string, string>>({})
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe })
  const role = me.data?.role ?? 'USER'
  const canModerate = role === 'MODERATOR' || role === 'ADMIN'
  const queue = useQuery({
    queryKey: ['submission-queue'],
    queryFn: () => fetchSubmissionQueue('PENDING'),
    enabled: canModerate,
  })

  const approve = useMutation({
    mutationFn: (id: string) => approveSubmission(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['submission-queue'] }),
  })
  const reject = useMutation({
    mutationFn: (input: { id: string; note?: string }) => rejectSubmission(input.id, input.note),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['submission-queue'] }),
  })

  if (me.isLoading) {
    return (
      <main className="panel">
        <p>Loading…</p>
      </main>
    )
  }

  if (!canModerate) {
    return (
      <main className="panel">
        <h2>Moderation</h2>
        <p className="error">Moderator role required.</p>
        <Link to="/lookup">Back to lookup</Link>
      </main>
    )
  }

  return (
    <main className="panel">
      <h2>Moderation queue</h2>
      <p>Review pending user product submissions.</p>
      {queue.isLoading && <p>Loading queue…</p>}
      {queue.error && <p className="error">{(queue.error as Error).message}</p>}
      <ul className="search-results">
        {(queue.data ?? []).map((item) => (
          <li key={item.id}>
            <div>
              <strong>{item.name}</strong>
              <span>
                {[item.brand, item.barcode && `EAN ${item.barcode}`, item.submittedAt]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
              {item.duplicateWarnings?.length > 0 && (
                <p className="error">{item.duplicateWarnings.join(' · ')}</p>
              )}
              <label htmlFor={`note-${item.id}`}>Reject note</label>
              <input
                id={`note-${item.id}`}
                value={note[item.id] ?? ''}
                onChange={(e) => setNote((prev) => ({ ...prev, [item.id]: e.target.value }))}
              />
              <div className="cta-row" style={{ justifyContent: 'flex-start' }}>
                <button
                  className="btn btn-primary"
                  type="button"
                  disabled={approve.isPending}
                  onClick={() => approve.mutate(item.id)}
                >
                  Approve
                </button>
                <button
                  className="btn btn-secondary"
                  type="button"
                  disabled={reject.isPending}
                  onClick={() => reject.mutate({ id: item.id, note: note[item.id] })}
                >
                  Reject
                </button>
                <Link to={`/products/${item.id}`}>Open</Link>
              </div>
            </div>
          </li>
        ))}
      </ul>
      {queue.data?.length === 0 && <p>No pending submissions.</p>}
    </main>
  )
}
