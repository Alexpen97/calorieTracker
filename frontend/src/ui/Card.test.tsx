import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DashboardCard, EmptyCard, MetricPill } from './Card'

describe('card primitives', () => {
  it('renders a titled dashboard card with optional action content', () => {
    render(
      <DashboardCard eyebrow="Today" title="Macros" action={<a href="/diary">Open</a>}>
        <p>Protein is on target.</p>
      </DashboardCard>,
    )

    expect(screen.getByText('Today')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Macros' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open' })).toHaveAttribute('href', '/diary')
  })

  it('renders metric pills and empty states accessibly', () => {
    render(
      <>
        <MetricPill label="Protein" value="82g" tone="green" />
        <EmptyCard title="No meals yet" copy="Add food to start tracking today." />
      </>,
    )

    expect(screen.getByText('Protein')).toBeInTheDocument()
    expect(screen.getByText('82g')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'No meals yet' })).toBeInTheDocument()
  })
})
