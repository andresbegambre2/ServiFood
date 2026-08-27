import { renderToString } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { KitchenBoard } from './KitchenPage'
import type { KitchenOrder } from '../types/kitchen'

const orders: KitchenOrder[] = [
  {
    publicNumber: 'SF-NUEVO',
    createdAt: '2026-08-26T14:30:00Z',
    stage: 'NEW',
    deliveryType: 'DELIVERY',
    notes: 'Tocar el timbre',
    items: [{ name: 'Hamburguesa clásica', quantity: 2, notes: 'Sin cebolla', extras: [{ name: 'Queso', quantity: 2 }] }],
  },
  { publicNumber: 'SF-PREPARANDO', createdAt: '2026-08-26T14:55:00Z', stage: 'PREPARING', deliveryType: 'PICKUP', items: [{ name: 'Papas', quantity: 1, extras: [] }] },
  { publicNumber: 'SF-LISTO', createdAt: '2026-08-26T14:58:00Z', stage: 'READY', deliveryType: 'PICKUP', items: [{ name: 'Bebida', quantity: 1, extras: [] }] },
]

describe('kitchen board', () => {
  it('renders all operational information and friendly labels without exposing internal enums', () => {
    const html = renderToString(<KitchenBoard orders={orders} now={new Date('2026-08-26T15:00:00Z').getTime()} onTransition={async () => undefined} />)

    expect(html).toContain('Nuevos')
    expect(html).toContain('En preparación')
    expect(html).toContain('Listos')
    expect(html).toContain('SF-NUEVO')
    expect(html).toContain('Domicilio')
    expect(html).toContain('Recoger en local')
    expect(html).toContain('Hamburguesa clásica')
    expect(html).toContain('Queso')
    expect(html).toContain('Sin cebolla')
    expect(html).toContain('Tocar el timbre')
    expect(html).toContain('DEMORADO')
    expect(html).not.toContain('>PREPARING<')
    expect(html).not.toContain('>DELIVERY<')
  })
})
