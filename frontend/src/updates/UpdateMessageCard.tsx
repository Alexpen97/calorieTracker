import { Link } from 'react-router-dom'
import type { UpdateMessage } from '../api/client'

type Props = {
  message: UpdateMessage
  onDismiss: () => void
  dismissing?: boolean
}

function isExternalUrl(url: string): boolean {
  return /^https?:\/\//i.test(url)
}

export default function UpdateMessageCard({ message, onDismiss, dismissing = false }: Props) {
  const hasAction = Boolean(message.actionLabel && message.actionUrl)

  return (
    <div className="sheet-backdrop update-message-backdrop" role="presentation" onClick={onDismiss}>
      <aside
        className="sheet update-message-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="update-message-title"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="sheet-kicker">Update</p>
        <h3 id="update-message-title">{message.title}</h3>
        {message.imageUrl && (
          <img className="update-message-image" src={message.imageUrl} alt="" />
        )}
        <p className="update-message-body">{message.body}</p>
        <div className="update-message-actions">
          {hasAction && message.actionUrl && message.actionLabel && (
            isExternalUrl(message.actionUrl) ? (
              <a
                className="btn btn-secondary"
                href={message.actionUrl}
                target="_blank"
                rel="noreferrer"
              >
                {message.actionLabel}
              </a>
            ) : (
              <Link className="btn btn-secondary" to={message.actionUrl} onClick={onDismiss}>
                {message.actionLabel}
              </Link>
            )
          )}
          <button
            type="button"
            className="btn"
            onClick={onDismiss}
            disabled={dismissing}
          >
            Got it
          </button>
        </div>
      </aside>
    </div>
  )
}
