import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => { vi.unstubAllGlobals(); vi.resetModules() })

describe('administrative API client', () => {
  it('obtains CSRF and sends a credentialed login request', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'safe-token' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    const { login } = await import('./admin')
    await login('admin@servifood.local', 'password')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0][0]).toContain('/admin/auth/csrf')
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST', credentials: 'include' })
    expect(fetchMock.mock.calls[1][1].headers).toMatchObject({ 'X-XSRF-TOKEN': 'safe-token' })
  })

  it('surfaces an expired session as a typed 401 error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Sesión expirada' }), { status: 401, headers: { 'Content-Type': 'application/json' } })))
    const { adminRequest } = await import('./admin')
    await expect(adminRequest('/dashboard')).rejects.toMatchObject({ status: 401, message: 'Sesión expirada' })
  })
})
