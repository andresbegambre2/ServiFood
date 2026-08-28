import { describe, expect, it } from 'vitest'
import { buildCheckoutLines, completeOrderCreation, paymentMethodsFor, validateDraft, validateReceipt, type CheckoutDraft } from './checkout'
import type { CartLine } from '../cart/cart'
import type { CreatedOrder } from '../../types/public'

const draft: CheckoutDraft = { name: 'Ana', phone: '3005551212', email: '', deliveryType: 'DELIVERY', address: 'Calle 1', neighborhood: 'Centro', reference: '', paymentMethod: 'CASH', cashTendered: '', pointsToRedeem: '' }
const line: CartLine = { id: 'line-1', productId: 8, slug: 'doble', name: 'Doble', imagePath: null, unitPriceMinor: 3200000, quantity: 2, notes: 'Sin cebolla', extras: [{ id: 3, name: 'Cheddar', unitPriceMinor: 300000 }] }
const createdOrder = { publicNumber: 'SF-260824-ABC123', trackingToken: 'private-token' } as CreatedOrder

describe('checkout rules', () => {
  it('switches payment options with delivery type', () => {
    expect(paymentMethodsFor('DELIVERY', true)).toEqual(['CASH', 'TRANSFER'])
    expect(paymentMethodsFor('PICKUP', false)).toEqual(['PAY_ON_PICKUP'])
  })
  it('requires address only for delivery', () => {
    expect(validateDraft({ ...draft, address: '' })).toBe('Completa dirección y barrio.')
    expect(validateDraft({ ...draft, deliveryType: 'PICKUP', address: '', neighborhood: '', paymentMethod: 'PAY_ON_PICKUP' })).toBeNull()
  })
  it('builds identifiers, quantities and expected prices without trusting totals', () => {
    expect(buildCheckoutLines([line])).toEqual([{ productId: 8, quantity: 2, notes: 'Sin cebolla', expectedUnitPrice: 32000, extras: [{ extraId: 3, expectedUnitPrice: 3000 }] }])
    expect(buildCheckoutLines([line], false)[0]).not.toHaveProperty('expectedUnitPrice')
  })
  it('validates receipt format and size', () => {
    expect(validateReceipt(null, true)).toContain('Adjunta')
    expect(validateReceipt({ name: 'pago.pdf', type: 'application/pdf', size: 100 }, true)).toContain('JPG')
    expect(validateReceipt({ name: 'pago.webp', type: 'image/webp', size: 5 * 1024 * 1024 + 1 }, true)).toContain('5 MB')
    expect(validateReceipt({ name: 'pago.png', type: 'image/png', size: 100 }, true)).toBeNull()
  })
  it('stores private access, clears the cart and opens confirmation after success', async () => {
    const calls: string[] = []
    await completeOrderCreation(async () => createdOrder, {
      storeTracking: () => calls.push('tracking'),
      clearCart: () => calls.push('clear'),
      openConfirmation: () => calls.push('navigate'),
    })
    expect(calls).toEqual(['tracking', 'clear', 'navigate'])
  })
  it('preserves the cart and does not navigate when creation fails', async () => {
    const calls: string[] = []
    await expect(completeOrderCreation(async () => { throw new Error('network') }, {
      storeTracking: () => calls.push('tracking'),
      clearCart: () => calls.push('clear'),
      openConfirmation: () => calls.push('navigate'),
    })).rejects.toThrow('network')
    expect(calls).toEqual([])
  })
})
