import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => { vi.unstubAllGlobals(); vi.resetModules() })

describe('kitchen API client', () => {
  it('loads orders with the session and sends protected stage transitions', async () => {
    const orders = [{ publicNumber: 'SF-1', stage: 'NEW', createdAt: '2026-08-26T15:00:00Z', deliveryType: 'PICKUP', items: [] }]
    const updated = { ...orders[0], stage: 'PREPARING' }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify(orders), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: 'kitchen-csrf' }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify(updated), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    const { getKitchenOrders, transitionKitchenOrder } = await import('./kitchen')

    await expect(getKitchenOrders()).resolves.toEqual(orders)
    await expect(transitionKitchenOrder('SF-1', 'PREPARING')).resolves.toEqual(updated)
    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/kitchen/orders')
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: 'include' })
    expect(fetchMock.mock.calls[2][0]).toContain('/api/v1/kitchen/orders/SF-1/stage')
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ method: 'PATCH', credentials: 'include', body: JSON.stringify({ target: 'PREPARING' }) })
    expect(fetchMock.mock.calls[2][1].headers).toMatchObject({ 'X-XSRF-TOKEN': 'kitchen-csrf', 'Content-Type': 'application/json' })
  })

  it('surfaces a failed refresh instead of leaving the board waiting indefinitely', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Servicio no disponible' }), { status: 503, headers: { 'Content-Type': 'application/json' } })))
    const { getKitchenOrders } = await import('./kitchen')
    await expect(getKitchenOrders()).rejects.toMatchObject({ status: 503, message: 'Servicio no disponible' })
  })
})
