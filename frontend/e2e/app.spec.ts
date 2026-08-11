import { expect, test, type Page, type Route } from '@playwright/test'

const emptyLinkPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

const emptyAuditPage = {
  content: [],
  page: 0,
  size: 8,
  totalElements: 0,
  totalPages: 0,
}

const emptyAnalytics = {
  totalUrls: 0,
  activeUrls: 0,
  lifetimeClicks: 0,
  periodClicks: 0,
  periodUniqueVisitors: 0,
  from: '2026-07-13',
  to: '2026-08-11',
  dailyClicks: [],
  topUrls: [],
}

test('opens and closes the sign-in dialog from the landing page', async ({ page }) => {
  await mockHealth(page)
  await mockSignedOutSession(page)

  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Shorten links. Understand every click.' })).toBeVisible()
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()

  const dialog = page.getByRole('dialog')
  await expect(dialog.getByRole('heading', { name: 'Welcome back.' })).toBeVisible()
  await dialog.getByRole('button', { name: 'Close authentication form' }).click()
  await expect(dialog).toBeHidden()
})

test('submits a password-reset request without revealing account existence', async ({ page }) => {
  await mockHealth(page)
  let submittedEmail = ''

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const pathname = new URL(request.url()).pathname

    if (pathname === '/api/v1/auth/refresh') {
      await json(route, 401, { message: 'No active session' })
      return
    }
    if (pathname === '/api/v1/auth/password-reset/request') {
      submittedEmail = (request.postDataJSON() as { email: string }).email
      await route.fulfill({ status: 202 })
      return
    }
    await json(route, 404, { message: 'Unexpected E2E request' })
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('button', { name: 'Forgot password?' }).click()
  await dialog.getByLabel('Email address').fill('person@example.com')
  await dialog.getByRole('button', { name: 'Send reset link' }).click()

  await expect(dialog.getByRole('status')).toContainText('If an account exists for that email')
  expect(submittedEmail).toBe('person@example.com')
})

test('signs in and renders the authenticated dashboard', async ({ page }) => {
  await mockHealth(page)

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const pathname = new URL(request.url()).pathname

    if (pathname === '/api/v1/auth/refresh') {
      await json(route, 401, { message: 'No active session' })
    } else if (pathname === '/api/v1/auth/login') {
      expect(request.postDataJSON()).toEqual({
        email: 'person@example.com',
        password: 'Password123!',
      })
      await json(route, 200, {
        accessToken: 'e2e-access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
      })
    } else if (pathname === '/api/v1/users/me') {
      expect(request.headers().authorization).toBe('Bearer e2e-access-token')
      await json(route, 200, {
        id: 42,
        email: 'person@example.com',
        displayName: 'Test User',
        role: 'USER',
        createdAt: '2026-08-11T00:00:00Z',
      })
    } else if (pathname === '/api/v1/urls/tags') {
      await json(route, 200, [])
    } else if (pathname === '/api/v1/urls') {
      await json(route, 200, emptyLinkPage)
    } else if (pathname === '/api/v1/analytics/overview') {
      await json(route, 200, emptyAnalytics)
    } else if (pathname === '/api/v1/audit/short-urls') {
      await json(route, 200, emptyAuditPage)
    } else {
      await json(route, 404, { message: `Unexpected E2E request: ${pathname}` })
    }
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Email address').fill('person@example.com')
  await dialog.getByLabel('Password').fill('Password123!')
  await dialog.getByRole('button', { name: 'Sign in securely' }).click()

  await expect(page.getByRole('heading', { name: 'Welcome, Test User' })).toBeVisible()
  await expect(page.getByText('person@example.com')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'No links yet.' })).toBeVisible()
})

async function mockHealth(page: Page) {
  await page.route('**/actuator/health', (route) => json(route, 200, { status: 'UP' }))
}

async function mockSignedOutSession(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) => json(route, 401, { message: 'No active session' }))
}

async function json(route: Route, status: number, body: unknown) {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}
