import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { KitchenOrder } from '../../types/kitchen'
import { createKitchenPolling, elapsedLabel, groupKitchenOrders, isDelayed, updateKitchenStage } from './kitchen'

const order = (publicNumber: string, stage: KitchenOrder['stage'], createdAt: string): KitchenOrder => ({
  publicNumber,
  stage,
  createdAt,
  deliveryType: 'PICKUP',
  items: [],
})

describe('kitchen board domain helpers', () => {
  it('groups orders by visual stage and keeps the oldest ticket first', () => {
    const grouped = groupKitchenOrders([
      order('SF-NEWER', 'NEW', '2026-08-26T15:20:00Z'),
      order('SF-READY', 'READY', '2026-08-26T15:10:00Z'),
      order('SF-OLDER', 'NEW', '2026-08-26T15:00:00Z'),
    ])

    expect(grouped.NEW.map(value => value.publicNumber)).toEqual(['SF-OLDER', 'SF-NEWER'])
    expect(grouped.PREPARING).toEqual([])
    expect(grouped.READY[0].publicNumber).toBe('SF-READY')
  })

  it('formats elapsed time, identifies delayed orders and applies an optimistic stage', () => {
    const createdAt = '2026-08-26T15:00:00Z'
    const now = new Date('2026-08-26T15:21:00Z').getTime()
    const orders = [order('SF-1', 'NEW', createdAt)]

    expect(elapsedLabel(createdAt, now)).toBe('Hace 21 min')
    expect(isDelayed(createdAt, now)).toBe(true)
    expect(updateKitchenStage(orders, 'SF-1', 'PREPARING')[0].stage).toBe('PREPARING')
    expect(orders[0].stage).toBe('NEW')
  })
})

describe('kitchen polling', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('refreshes every 12 seconds only while the page is visible and stops cleanly', () => {
    const refresh = vi.fn()
    let hidden = false
    const stop = createKitchenPolling(refresh, () => hidden)

    vi.advanceTimersByTime(12_000)
    expect(refresh).toHaveBeenCalledTimes(1)
    hidden = true
    vi.advanceTimersByTime(24_000)
    expect(refresh).toHaveBeenCalledTimes(1)
    hidden = false
    vi.advanceTimersByTime(12_000)
    expect(refresh).toHaveBeenCalledTimes(2)
    stop()
    vi.advanceTimersByTime(24_000)
    expect(refresh).toHaveBeenCalledTimes(2)
  })
})
