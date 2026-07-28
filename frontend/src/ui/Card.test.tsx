import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { DashboardCard, EmptyCard, MetricCard, MetricPill } from './Card'

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

  it('applies density classes for hero, metric, list, and insight cards', () => {
    const { container } = render(
      <>
        <DashboardCard density="hero" title="Calories left">
          <p>1505</p>
        </DashboardCard>
        <DashboardCard density="metric" title="Protein left">
          <p>129g</p>
        </DashboardCard>
        <DashboardCard density="list" title="Recently logged">
          <p>Greek yogurt</p>
        </DashboardCard>
        <DashboardCard density="insight" title="Weight">
          <p>71.8 kg</p>
        </DashboardCard>
      </>,
    )

    expect(container.querySelector('.dashboard-card-hero')).toBeTruthy()
    expect(container.querySelector('.dashboard-card-metric')).toBeTruthy()
    expect(container.querySelector('.dashboard-card-list')).toBeTruthy()
    expect(container.querySelector('.dashboard-card-insight')).toBeTruthy()
  })

  it('renders compact metric cards with tone and value hierarchy', () => {
    const { container } = render(
      <MetricCard label="Protein left" value="129g" tone="protein" progress={72} />,
    )

    expect(screen.getByText('129g')).toBeInTheDocument()
    expect(screen.getByText('Protein left')).toBeInTheDocument()
    expect(container.querySelector('.metric-card-protein')).toBeTruthy()
    expect(screen.getByRole('progressbar', { name: /Protein left/i })).toHaveAttribute(
      'aria-valuenow',
      '72',
    )
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
