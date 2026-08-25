import { renderToString } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { CartContext, type CartContextValue } from './cart-context'
import { CartDrawer } from './CartDrawer'

function renderDrawer(isOpen: boolean) {
  const cart: CartContextValue = {
    lines: [], units: 0, subtotal: 0, isOpen,
    add: () => undefined, remove: () => undefined, setQuantity: () => undefined,
    clear: () => undefined, open: () => undefined, close: () => undefined,
  }
  return renderToString(<MemoryRouter><CartContext.Provider value={cart}><CartDrawer /></CartContext.Provider></MemoryRouter>)
}

describe('CartDrawer accessibility state', () => {
  it('uses inert instead of aria-hidden while closed', () => {
    const html = renderDrawer(false)
    expect(html).toContain('inert=""')
    expect(html).not.toContain('aria-hidden')
  })

  it('exposes the open drawer as a modal dialog', () => {
    const html = renderDrawer(true)
    expect(html).toContain('role="dialog"')
    expect(html).toContain('aria-modal="true"')
    expect(html).not.toContain('inert=""')
  })
})
