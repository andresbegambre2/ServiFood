import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => { vi.unstubAllGlobals(); vi.resetModules() })

describe('administrative API client', () => {
  it('obtains CSRF for login and refreshes it after session rotation', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'safe-token' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'rotated-token' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    const { adminJson, login } = await import('./admin')
    await login('admin@servifood.local', 'password')
    await adminJson('/orders/SF-1/status', 'PATCH', { status: 'CONFIRMED' })
    expect(fetchMock).toHaveBeenCalledTimes(4)
    expect(fetchMock.mock.calls[0][0]).toContain('/admin/auth/csrf')
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST', credentials: 'include' })
    expect(fetchMock.mock.calls[1][1].headers).toMatchObject({ 'X-XSRF-TOKEN': 'safe-token' })
    expect(fetchMock.mock.calls[2][0]).toContain('/admin/auth/csrf')
    expect(fetchMock.mock.calls[3][1].headers).toMatchObject({ 'X-XSRF-TOKEN': 'rotated-token' })
  })

  it('surfaces an expired session as a typed 401 error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Sesión expirada' }), { status: 401, headers: { 'Content-Type': 'application/json' } })))
    const { adminRequest } = await import('./admin')
    await expect(adminRequest('/dashboard')).rejects.toMatchObject({ status: 401, message: 'Sesión expirada' })
  })
})
