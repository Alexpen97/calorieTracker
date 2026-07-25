import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import {
  GroupedBars,
  MicroTrendGrid,
  NestedCalorieMacroRing,
  ProgressRing,
  ProgressRow,
  SharedMicronutrientTrendChart,
  Sparkline,
  StackedBar,
  StackedFoodBars,
  WeightTrendChart,
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

  it('renders stacked food bars with amount labels and no rings', () => {
    render(
      <StackedFoodBars
        rows={[
          { label: 'Calories', percent: 43, amountLabel: '900 / 2,100' },
          { label: 'Protein', percent: 45, amountLabel: '45 / 100 g' },
          { label: 'Carbs', percent: 32, amountLabel: '80 / 250 g' },
          { label: 'Fat', percent: 43, amountLabel: '30 / 70 g' },
        ]}
      />,
    )

    expect(screen.getByTestId('diary-macro-bars')).toBeInTheDocument()
    expect(screen.getByLabelText('Calories: 900 / 2,100')).toBeInTheDocument()
    expect(screen.getByLabelText('Protein: 45 / 100 g')).toBeInTheDocument()
    expect(screen.getByText('900 / 2,100')).toBeInTheDocument()
    expect(document.querySelector('.progress-ring')).not.toBeInTheDocument()
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

  it('renders a timed weight chart with a marker per logged weigh-in', () => {
    render(
      <WeightTrendChart
        label="Weight trend"
        points={[
          { weightKg: 72.3, t: 0.2 },
          { weightKg: 71.8, t: 1 },
        ]}
      />,
    )

    expect(screen.getByLabelText('Weight trend')).toBeInTheDocument()
    expect(screen.getAllByTestId('weight-trend-point')).toHaveLength(2)
    expect(screen.getByTestId('weight-trend-path')).toBeInTheDocument()
  })

  it('renders y/x axes, gridlines, and clearer markers on the weight chart', () => {
    render(
      <WeightTrendChart
        label="Weight trend"
        points={[
          { weightKg: 72.3, t: 0.2 },
          { weightKg: 71.8, t: 1 },
        ]}
        xLabels={['Jun 23', 'Jul 7', 'Jul 22']}
      />,
    )

    expect(screen.getByTestId('weight-trend-grid')).toBeInTheDocument()
    expect(screen.getAllByTestId('weight-trend-y-tick').length).toBeGreaterThanOrEqual(2)
    expect(screen.getAllByTestId('weight-trend-x-tick')).toHaveLength(3)
    expect(screen.getByText('Jun 23')).toBeInTheDocument()
    expect(screen.getByText('Jul 7')).toBeInTheDocument()
    expect(screen.getByText('Jul 22')).toBeInTheDocument()
    expect(screen.getByText(/^72[,.]3$/)).toBeInTheDocument()
    expect(screen.getByText(/^71[,.]8$/)).toBeInTheDocument()
    const markers = screen.getAllByTestId('weight-trend-point')
    expect(markers).toHaveLength(2)
    const radius = Number(markers[0].getAttribute('r'))
    expect(radius).toBeGreaterThan(0)
    expect(radius).toBeLessThanOrEqual(2.5)
  })

  it('shows the weight on each point for hover tooltips', () => {
    render(
      <WeightTrendChart
        label="Weight trend"
        points={[
          { weightKg: 72.3, t: 0.2, measuredAt: '2026-07-20T08:00:00Z' },
          { weightKg: 71.8, t: 1, measuredAt: '2026-07-22T08:00:00Z' },
        ]}
      />,
    )

    const first = screen.getByLabelText(/^72[,.]3 kg/)
    const second = screen.getByLabelText(/^71[,.]8 kg/)
    expect(first).toBeInTheDocument()
    expect(second).toBeInTheDocument()
    expect(first.querySelector('title')).toHaveTextContent(/^72[,.]3 kg/)
    expect(second.querySelector('title')).toHaveTextContent(/^71[,.]8 kg/)

    fireEvent.mouseEnter(first)
    expect(screen.getByTestId('weight-trend-tooltip')).toHaveTextContent(/^72[,.]3 kg/)
  })

  it('renders micronutrient trend lines with latest amount labels', () => {
    render(
      <MicroTrendGrid
        rows={[
          {
            code: 'vitamin_d',
            label: 'Vitamin D',
            latestPercent: 60,
            latestAmountLabel: '9 / 15 ug',
            points: [
              { percent: 20 },
              { percent: 40 },
              { percent: 60 },
            ],
          },
        ]}
      />,
    )

    expect(screen.getByLabelText('Vitamin D trend, last 30 days')).toBeInTheDocument()
    expect(screen.getByText('Vitamin D')).toBeInTheDocument()
    expect(screen.getByText('9 / 15 ug')).toBeInTheDocument()
    expect(document.querySelector('.micro-trend-mid')).toBeInTheDocument()
  })

  it('renders a shared micronutrient chart with RDI-centered lines and legend', () => {
    render(
      <SharedMicronutrientTrendChart
        label="Vitamin trends, last 30 days"
        series={[
          {
            code: 'vitamin_d',
            label: 'Vitamin D',
            target: 15,
            points: [
              { date: '2026-07-01', amount: 7.5 },
              { date: '2026-07-02', amount: 15 },
              { date: '2026-07-03', amount: 22.5 },
            ],
          },
          {
            code: 'vitamin_c',
            label: 'Vitamin C',
            target: 80,
            points: [
              { date: '2026-07-01', amount: 40 },
              { date: '2026-07-02', amount: 80 },
              { date: '2026-07-03', amount: 120 },
            ],
          },
        ]}
      />,
    )

    expect(screen.getByLabelText('Vitamin trends, last 30 days')).toBeInTheDocument()
    expect(screen.getByTestId('shared-micro-line-vitamin_d')).toBeInTheDocument()
    expect(screen.getByTestId('shared-micro-line-vitamin_c')).toBeInTheDocument()
    expect(screen.getByText('RDI')).toBeInTheDocument()
    expect(screen.getByText('150%')).toBeInTheDocument()
    expect(screen.getByText('50%')).toBeInTheDocument()
    expect(screen.getByLabelText('Vitamin trends, last 30 days legend')).toBeInTheDocument()
    expect(screen.getAllByText('Vitamin D').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Vitamin C').length).toBeGreaterThan(0)
  })
})
