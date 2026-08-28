import { renderToString } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { CartContext, type CartContextValue } from '../features/cart/cart-context'
import { StorefrontContext, type StorefrontContextValue } from '../features/storefront/storefront-context'
import type { CheckoutQuote, StorefrontData, TransferPayment } from '../types/public'
import { CheckoutPage, CheckoutSummary } from './CheckoutPage'

const cart: CartContextValue = {
  lines: [{ id: 'line-1', productId: 1, slug: 'doble-bacon', name: 'Doble Bacon', imagePath: null, unitPriceMinor: 32_000_00, quantity: 1, notes: '', extras: [] }],
  units: 1, subtotal: 32_000_00, isOpen: false,
  add: () => undefined, remove: () => undefined, setQuantity: () => undefined,
  clear: () => undefined, open: () => undefined, close: () => undefined,
}

function storefront(transfer?: TransferPayment): StorefrontData {
  return {
    business: {
      tradeName: 'Distrito Smash', description: null, logoPath: null, phone: '3005551212', whatsapp: '3005551212',
      address: 'Calle 1', instagram: null, facebook: null, baseDeliveryFee: 5_000,
      estimatedPreparationMinutes: 25, currency: 'COP', timeZone: 'America/Bogota', hours: [], ...(transfer ? { transfer } : {}),
    },
    categories: [], products: [], promotions: [],
  }
}

function renderCheckout(value: StorefrontContextValue) {
  return renderToString(<MemoryRouter><StorefrontContext.Provider value={value}><CartContext.Provider value={cart}><CheckoutPage /></CartContext.Provider></StorefrontContext.Provider></MemoryRouter>)
}

describe('CheckoutPage storefront configuration', () => {
  it('renders a loading state before business data arrives', () => {
    const html = renderCheckout({ data: null, loading: true, error: null, retry: () => undefined })
    expect(html).toContain('Preparando tu pedido')
  })

  it('renders safely without transfer configuration', () => {
    const render = () => renderCheckout({ data: storefront(), loading: false, error: null, retry: () => undefined })
    expect(render).not.toThrow()
    expect(render()).toContain('Efectivo')
    expect(render()).not.toContain('Transferencia')
  })

  it('shows transfer only when the backend configuration enables it', () => {
    const data = storefront({ provider: 'Nequi', accountHolder: 'Distrito Smash', accountReference: '3005551212', qrPath: '/qr.svg', configured: true })
    expect(renderCheckout({ data, loading: false, error: null, retry: () => undefined })).toContain('Transferencia')
  })

  it('renders a recoverable API error instead of the checkout form', () => {
    const html = renderCheckout({ data: null, loading: false, error: 'Servicio no disponible', retry: () => undefined })
    expect(html).toContain('No pudimos cargar la configuración')
    expect(html).toContain('Servicio no disponible')
  })

  it('replaces quote loading with an error and retry action when pricing fails', () => {
    const html = renderToString(<CartContext.Provider value={cart}><CheckoutSummary quote={null} error="" quoteError="No autorizado" retryQuote={() => undefined} submitting={false} currency="COP" /></CartContext.Provider>)
    expect(html).toContain('No pudimos actualizar los precios')
    expect(html).toContain('Reintentar precios')
    expect(html).not.toContain('Actualizando precios')
  })

  it('shows coupon, redeemed points and points to earn from the backend quote', () => {
    const quote: CheckoutQuote = { totals: { subtotal: 50000, discount: 15000, deliveryFee: 0, total: 35000, currency: 'COP', estimatedMinutes: 25 }, items: [], loyalty: { active: true, availablePoints: 50, pointsRedeemed: 10, pointsDiscount: 10000, couponCode: 'CLIENTE10', couponDiscount: 5000, pointsToEarn: 35, minimumPointsToRedeem: 10, maximumRedemptionPercentage: 30, amountPerPoint: 1000 } }
    const html = renderToString(<CartContext.Provider value={cart}><CheckoutSummary quote={quote} error="" quoteError="" retryQuote={() => undefined} submitting={false} currency="COP" /></CartContext.Provider>)
    expect(html).toContain('Cupón')
    expect(html).toContain('CLIENTE10')
    expect(html).toContain('10<!-- --> puntos')
    expect(html).toContain('Ganarás')
    expect(html).toContain('35<!-- --> puntos')
  })
})
