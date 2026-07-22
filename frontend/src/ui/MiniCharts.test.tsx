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

  it('renders calorie ring with horizontal macro bars and amount labels', () => {
    render(
      <NestedCalorieMacroRing
        calorieLabel="Calories"
        caloriePercent={128}
        calorieAmountLabel="1,450 / 2,100"
        macros={[
          { label: 'Protein', percent: 82, amountLabel: '82 / 100 g' },
          { label: 'Carbs', percent: 72, amountLabel: '180 / 250 g' },
          { label: 'Fat', percent: 69, amountLabel: '48 / 70 g' },
        ]}
      />,
    )

    expect(screen.getByLabelText('Calories: 1,450 / 2,100')).toBeInTheDocument()
    expect(screen.getByText('1,450 / 2,100')).toBeInTheDocument()
    expect(screen.getByText('Protein')).toBeInTheDocument()
    expect(screen.getByText('82 / 100 g')).toBeInTheDocument()
    expect(screen.getByText('Carbs')).toBeInTheDocument()
    expect(screen.getByText('180 / 250 g')).toBeInTheDocument()
    expect(screen.getByText('Fat')).toBeInTheDocument()
    expect(screen.getByText('48 / 70 g')).toBeInTheDocument()
    expect(screen.queryByText('82%')).not.toBeInTheDocument()
    expect(screen.getByTestId('nested-calorie-track')).toBeInTheDocument()
    expect(screen.queryByTestId('nested-macro-track')).not.toBeInTheDocument()
    expect(screen.getAllByTestId('nested-macro-bar-track')).toHaveLength(3)
  })

  it('keeps full goal backgrounds when macros are at zero', () => {
    render(
      <NestedCalorieMacroRing
        calorieLabel="Calories"
        caloriePercent={0}
        calorieAmountLabel="0 / 2,100"
        macros={[
          { label: 'Protein', percent: 0, amountLabel: '0 / 100 g' },
          { label: 'Carbs', percent: 0, amountLabel: '0 / 250 g' },
          { label: 'Fat', percent: 0, amountLabel: '0 / 70 g' },
        ]}
      />,
    )

    expect(screen.getByTestId('nested-calorie-track')).toBeInTheDocument()
    expect(screen.queryByTestId('nested-macro-track')).not.toBeInTheDocument()
    expect(screen.getAllByTestId('nested-macro-bar-track')).toHaveLength(3)
    expect(screen.getByLabelText('Calories: 0 / 2,100')).toBeInTheDocument()
    expect(screen.getByLabelText('Protein: 0 / 100 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Carbs: 0 / 250 g')).toBeInTheDocument()
    expect(screen.getByLabelText('Fat: 0 / 70 g')).toBeInTheDocument()
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
