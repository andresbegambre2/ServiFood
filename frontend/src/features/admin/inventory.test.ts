import { describe, expect, it } from 'vitest'
import { formatStock, inventoryMovementLabels, stockStatusLabels, upsertRecipeLine } from './inventory'

describe('inventory presentation helpers', () => {
  it('formats units and exposes every movement in Spanish', () => {
    expect(formatStock(1250.5, 'GRAM')).toContain('g')
    expect(inventoryMovementLabels).toEqual({ ENTRY: 'Entrada', CONSUMPTION: 'Consumo', ADJUSTMENT: 'Ajuste', REVERSAL: 'Reversión' })
    expect(stockStatusLabels.OUT).toBe('Agotado')
  })

  it('updates a recipe ingredient without duplicating it', () => {
    const first = { ingredientId: 1, ingredientName: 'Carne', unit: 'GRAM' as const, quantity: 100 }
    const updated = upsertRecipeLine([first], { ...first, quantity: 150 })
    expect(updated).toHaveLength(1)
    expect(updated[0].quantity).toBe(150)
  })
})
