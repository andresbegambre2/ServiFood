import { renderToString } from 'react-dom/server'
import { describe, expect, it, vi } from 'vitest'
import type { InventoryOverview } from '../types/admin'
import { InventoryView } from './AdminInventoryPage'

const data: InventoryOverview = {
  trackedIngredients: 2, lowStockCount: 1, outOfStockCount: 0,
  ingredients: [
    { id: 1, name: 'Carne de res', unit: 'GRAM', stockCurrent: 800, stockMinimum: 1000, unitCost: 32, active: true, stockStatus: 'LOW' },
    { id: 2, name: 'Pan brioche', unit: 'UNIT', stockCurrent: 20, stockMinimum: 10, active: true, stockStatus: 'OK' },
  ],
  productRecipes: [{ targetId: 10, targetName: 'Clásica Urbana', effectiveAvailable: true, ingredients: [{ ingredientId: 1, ingredientName: 'Carne de res', unit: 'GRAM', quantity: 150 }] }],
  extraRecipes: [{ targetId: 20, targetName: 'Carne adicional', effectiveAvailable: false, ingredients: [{ ingredientId: 1, ingredientName: 'Carne de res', unit: 'GRAM', quantity: 150 }] }],
  recentMovements: [{ id: 1, ingredientId: 1, ingredientName: 'Carne de res', type: 'CONSUMPTION', quantityDelta: -300, balanceAfter: 800, reason: 'Consumo del pedido', orderNumber: 'SF-1', createdAt: '2026-08-27T12:00:00Z' }],
}
const callbacks = { onTab: vi.fn(), onEdit: vi.fn(), onAdjust: vi.fn(), onRecipe: vi.fn() }

describe('inventory administration view', () => {
  it('shows stock alerts and keeps cashier access read-only', () => {
    const html = renderToString(<InventoryView data={data} editable={false} tab="ingredients" {...callbacks} />)
    expect(html).toContain('Stock bajo')
    expect(html).toContain('Carne de res')
    expect(html).toContain('Solo consulta')
    expect(html).not.toContain('>Ajustar<')
  })

  it('renders product and extra recipes with effective availability', () => {
    const html = renderToString(<InventoryView data={data} editable tab="recipes" {...callbacks} />)
    expect(html).toContain('Clásica Urbana')
    expect(html).toContain('Carne adicional')
    expect(html).toContain('Sin insumos')
    expect(html).toContain('Editar receta')
  })

  it('renders consumption history in Spanish', () => {
    const html = renderToString(<InventoryView data={data} editable tab="movements" {...callbacks} />)
    expect(html).toContain('Consumo')
    expect(html).toContain('SF-1')
    expect(html).toContain('-300')
  })
})
