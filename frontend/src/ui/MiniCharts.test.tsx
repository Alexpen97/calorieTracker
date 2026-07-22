import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { GroupedBars, ProgressRing, ProgressRow, Sparkline, StackedBar } from './MiniCharts'

describe('mini chart primitives', () => {
  it('bounds progress values and exposes readable labels', () => {
    render(
      <>
        <ProgressRing label="Calories" percent={128} value="1,920" />
        <ProgressRow label="Vitamin D" percent={43} amountLabel="43%" />
      </>,
    )

    expect(screen.getByLabelText('Calories: 100%')).toBeInTheDocument()
    expect(screen.getByText('1,920')).toBeInTheDocument()
    expect(screen.getByText('Vitamin D')).toBeInTheDocument()
    expect(screen.getByText('43%')).toBeInTheDocument()
  })

  it('renders sparkline, stacked, and grouped chart labels', () => {
    render(
      <>
        <Sparkline label="Weight trend" points={[72.4, 72.1, 71.8]} />
        <StackedBar
          label="Macro balance"
          segments={[
            { label: 'Protein', percent: 30 },
            { label: 'Carbs', percent: 45 },
            { label: 'Fat', percent: 25 },
          ]}
        />
        <GroupedBars
          label="Minerals"
          groups={[
            { label: 'Iron', percent: 70 },
            { label: 'Calcium', percent: 55 },
          ]}
        />
      </>,
    )

    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
    expect(screen.getByText('Macro balance')).toBeInTheDocument()
    expect(screen.getByText('Iron')).toBeInTheDocument()
  })
})
