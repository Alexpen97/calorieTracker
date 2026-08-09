import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import UpdateMessageHost from './UpdateMessageHost'
import * as client from '../api/client'

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client')
  return {
    ...actual,
    fetchPendingUpdateMessage: vi.fn(),
    acknowledgeUpdateMessage: vi.fn(),
  }
})

function renderHost() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <UpdateMessageHost />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('UpdateMessageHost', () => {
  beforeEach(() => {
    vi.mocked(client.fetchPendingUpdateMessage).mockReset()
    vi.mocked(client.acknowledgeUpdateMessage).mockReset()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders nothing when there is no pending message', async () => {
    vi.mocked(client.fetchPendingUpdateMessage).mockResolvedValue(null)
    renderHost()
    await waitFor(() => {
      expect(client.fetchPendingUpdateMessage).toHaveBeenCalled()
    })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('shows pending update and acknowledges on dismiss', async () => {
    vi.mocked(client.fetchPendingUpdateMessage).mockResolvedValue({
      id: 'msg-1',
      title: "What's new",
      body: 'We added water goals.',
      imageUrl: null,
      actionLabel: null,
      actionUrl: null,
      pushedAt: '2026-08-09T12:00:00Z',
    })
    vi.mocked(client.acknowledgeUpdateMessage).mockResolvedValue()

    renderHost()

    expect(await screen.findByRole('dialog', { name: "What's new" })).toBeInTheDocument()
    expect(screen.getByText('We added water goals.')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /got it/i }))

    await waitFor(() => {
      expect(client.acknowledgeUpdateMessage).toHaveBeenCalledWith('msg-1')
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('shows optional action when provided', async () => {
    vi.mocked(client.fetchPendingUpdateMessage).mockResolvedValue({
      id: 'msg-2',
      title: 'Try analytics',
      body: 'See your weekly trends.',
      imageUrl: 'https://cdn.example/update.png',
      actionLabel: 'Open analytics',
      actionUrl: '/analytics',
      pushedAt: '2026-08-09T12:00:00Z',
    })

    renderHost()

    expect(await screen.findByRole('heading', { name: 'Try analytics' })).toBeInTheDocument()
    expect(document.querySelector('.update-message-image')).toHaveAttribute(
      'src',
      'https://cdn.example/update.png',
    )
    expect(screen.getByRole('link', { name: 'Open analytics' })).toHaveAttribute(
      'href',
      '/analytics',
    )
  })
})
