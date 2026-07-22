import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  GroupedBars,
  NestedCalorieMacroRing,
  ProgressRing,
  ProgressRow,
  Sparkline,
  StackedBar,
} from './MiniCharts'

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

  it('renders nested calorie ring with macro name and percent labels', () => {
    render(
      <NestedCalorieMacroRing
        calorieLabel="Calories"
        caloriePercent={128}
        calorieValue="650"
        macros={[
          { label: 'Protein', percent: 82 },
          { label: 'Carbs', percent: 72 },
          { label: 'Fat', percent: 69 },
        ]}
      />,
    )

    expect(screen.getByLabelText('Calories: 100%')).toBeInTheDocument()
    expect(screen.getByText('650')).toBeInTheDocument()
    expect(screen.getByText('Protein')).toBeInTheDocument()
    expect(screen.getByText('82%')).toBeInTheDocument()
    expect(screen.getByText('Carbs')).toBeInTheDocument()
    expect(screen.getByText('72%')).toBeInTheDocument()
    expect(screen.getByText('Fat')).toBeInTheDocument()
    expect(screen.getByText('69%')).toBeInTheDocument()
    expect(screen.getByTestId('nested-calorie-track')).toBeInTheDocument()
    expect(screen.getAllByTestId('nested-macro-track')).toHaveLength(3)
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
