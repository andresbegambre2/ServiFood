import type { IngredientUnit, InventoryMovementType, RecipeLine, StockStatus } from '../../types/admin'

export const ingredientUnitLabels: Record<IngredientUnit, string> = { GRAM: 'g', MILLILITER: 'ml', UNIT: 'unidad' }
export const inventoryMovementLabels: Record<InventoryMovementType, string> = { ENTRY: 'Entrada', CONSUMPTION: 'Consumo', ADJUSTMENT: 'Ajuste', REVERSAL: 'Reversión' }
export const stockStatusLabels: Record<StockStatus, string> = { OK: 'Disponible', LOW: 'Stock bajo', OUT: 'Agotado', INACTIVE: 'Inactivo' }
export function formatStock(value: number, unit: IngredientUnit) { return `${new Intl.NumberFormat('es-CO', { maximumFractionDigits: 3 }).format(value)} ${ingredientUnitLabels[unit]}` }
export function upsertRecipeLine(lines: RecipeLine[], line: RecipeLine) { return [...lines.filter(value => value.ingredientId !== line.ingredientId), line].sort((a, b) => a.ingredientName.localeCompare(b.ingredientName, 'es')) }
