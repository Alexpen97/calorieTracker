import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import AppNavigation from './AppNavigation'

describe('AppNavigation', () => {
  it('renders mobile-first primary tabs for signed-in users', () => {
    render(
      <MemoryRouter>
        <AppNavigation loggedIn canModerate={false} />
      </MemoryRouter>,
    )

    expect(screen.getAllByRole('link', { name: 'Dashboard' })[0]).toHaveAttribute('href', '/today')
    expect(screen.getAllByRole('link', { name: 'Diary' })[0]).toHaveAttribute('href', '/diary')
    expect(screen.getAllByRole('link', { name: 'Analytics' })[0]).toHaveAttribute(
      'href',
      '/analytics',
    )
    expect(screen.getAllByRole('link', { name: 'Profile' })[0]).toHaveAttribute('href', '/me')
  })

  it('keeps moderation available only for moderators and admins', () => {
    render(
      <MemoryRouter>
        <AppNavigation loggedIn canModerate />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Moderation' })).toHaveAttribute('href', '/moderation')
  })
})
