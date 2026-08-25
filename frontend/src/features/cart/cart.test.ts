import { describe, expect, it } from 'vitest'
import { CART_VERSION, cartReducer, cartSubtotal, cartUnits, lineTotal, parseStoredCart, type CartLine } from './cart'

const line = (id: string, notes = '', extras: CartLine['extras'] = []): CartLine => ({ id, productId: 10, slug: 'clasica', name: 'Clásica', imagePath: null, unitPriceMinor: 2_500_000, quantity: 1, notes, extras })

describe('customer cart', () => {
  it('keeps different configurations as independent lines', () => {
    const plain = line('plain', 'Sin cebolla')
    const bacon = line('bacon', '', [{ id: 2, name: 'Tocineta', unitPriceMinor: 400_000 }])
    const state = cartReducer(cartReducer({ lines: [] }, { type: 'add', line: plain }), { type: 'add', line: bacon })
    expect(state.lines).toHaveLength(2)
    expect(state.lines.map((item) => item.id)).toEqual(['plain', 'bacon'])
  })

  it('calculates extras and quantities using integer minor units', () => {
    const configured = { ...line('configured', '', [{ id: 2, name: 'Cheddar', unitPriceMinor: 300_000 }]), quantity: 2 }
    expect(lineTotal(configured)).toBe(5_600_000)
    expect(cartSubtotal({ lines: [configured] })).toBe(5_600_000)
    expect(cartUnits({ lines: [configured] })).toBe(2)
  })

  it('prevents invalid quantities and limits observations', () => {
    const state = cartReducer({ lines: [line('one')] }, { type: 'quantity', id: 'one', quantity: -4 })
    const added = cartReducer(state, { type: 'add', line: line('two', 'x'.repeat(300)) })
    expect(state.lines[0].quantity).toBe(1)
    expect(added.lines[1].notes).toHaveLength(240)
  })

  it('validates and versions persisted carts', () => {
    expect(parseStoredCart('{broken')).toEqual({ lines: [] })
    expect(parseStoredCart(JSON.stringify({ version: 99, lines: [line('old')] }))).toEqual({ lines: [] })
    expect(parseStoredCart(JSON.stringify({ version: CART_VERSION, lines: [line('valid')] })).lines[0].id).toBe('valid')
  })
})
