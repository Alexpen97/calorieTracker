import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  fetchMyFeedback,
  submitFeedback,
  type FeedbackStatus,
  type UserFeedback,
} from '../../api/client'
import { SettingsSectionShell } from './SettingsSectionShell'

const MIN_MESSAGE_LENGTH = 10
const MAX_MESSAGE_LENGTH = 2000

export default function SettingsFeedbackSection() {
  const queryClient = useQueryClient()
  const [message, setMessage] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [sentNotice, setSentNotice] = useState(false)

  const feedbackQuery = useQuery({
    queryKey: ['my-feedback'],
    queryFn: fetchMyFeedback,
  })

  const sendFeedback = useMutation({
    mutationFn: (input: { message: string; appVersion?: string }) => submitFeedback(input),
    onSuccess: async () => {
      setMessage('')
      setFormError(null)
      setSentNotice(true)
      await queryClient.invalidateQueries({ queryKey: ['my-feedback'] })
    },
  })

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setSentNotice(false)
    const trimmed = message.trim()
    if (trimmed.length < MIN_MESSAGE_LENGTH) {
      setFormError(`Please write at least ${MIN_MESSAGE_LENGTH} characters.`)
      return
    }
    if (trimmed.length > MAX_MESSAGE_LENGTH) {
      setFormError(`Keep feedback under ${MAX_MESSAGE_LENGTH} characters.`)
      return
    }
    sendFeedback.mutate({
      message: trimmed,
      appVersion: import.meta.env.VITE_APP_VERSION || '0.1.0',
    })
  }

  const items = feedbackQuery.data ?? []

  return (
    <SettingsSectionShell
      title="Feedback"
      description="Share ideas or issues and track whether we accepted or completed them."
    >
      {feedbackQuery.isLoading && <p>Loading…</p>}
      {feedbackQuery.error && <p className="error">{(feedbackQuery.error as Error).message}</p>}
      {sendFeedback.error && <p className="error">{(sendFeedback.error as Error).message}</p>}

      <section className="dashboard-card">
        <form className="feedback-form" onSubmit={onSubmit}>
          <label htmlFor="settings-feedback-message">Your feedback</label>
          <textarea
            id="settings-feedback-message"
            maxLength={MAX_MESSAGE_LENGTH}
            onChange={(event) => setMessage(event.target.value)}
            placeholder="What should we improve?"
            rows={5}
            value={message}
          />
          <div className="feedback-form-meta">
            <span>
              {message.trim().length}/{MAX_MESSAGE_LENGTH}
            </span>
            <button className="btn btn-primary" disabled={sendFeedback.isPending} type="submit">
              {sendFeedback.isPending ? 'Sending…' : 'Send feedback'}
            </button>
          </div>
        </form>
        {formError && <p className="error">{formError}</p>}
        {sentNotice && <p className="success-copy">Feedback sent. Status starts as Pending.</p>}
      </section>

      <section className="dashboard-card">
        <h3 className="settings-subsection-title">Your submissions</h3>
        {items.length === 0 ? (
          <p className="empty-copy">No feedback yet.</p>
        ) : (
          <ul className="feedback-list">
            {items.map((item) => (
              <li className="feedback-list-item" key={item.id}>
                <div className="feedback-list-header">
                  <span className={`feedback-status feedback-status-${item.status.toLowerCase()}`}>
                    {statusLabel(item.status)}
                  </span>
                  <time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time>
                </div>
                <p className="feedback-list-message">{item.message}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </SettingsSectionShell>
  )
}

export function feedbackSummary(items: UserFeedback[] | undefined): string {
  if (!items || items.length === 0) {
    return 'Share ideas and track status'
  }
  const openCount = items.filter((item) => item.status !== 'COMPLETED').length
  if (openCount === 0) {
    return `${items.length} submitted · all completed`
  }
  return `${items.length} submitted · ${openCount} open`
}

function statusLabel(status: FeedbackStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Pending'
    case 'ACCEPTED':
      return 'Accepted'
    case 'COMPLETED':
      return 'Completed'
  }
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
