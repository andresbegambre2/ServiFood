import { adminJson, adminRequest } from './admin'
import type { IngredientView, InventoryMovementType, InventoryOverview, RecipeView } from '../types/admin'

export const getInventory = () => adminRequest<InventoryOverview>('/inventory')
export const createIngredient = (body: unknown) => adminJson<IngredientView>('/inventory/ingredients', 'POST', body)
export const updateIngredient = (id: number, body: unknown) => adminJson<IngredientView>(`/inventory/ingredients/${id}`, 'PUT', body)
export const adjustIngredient = (id: number, body: { type: InventoryMovementType; quantity: number; reason: string }) => adminJson<IngredientView>(`/inventory/ingredients/${id}/adjustments`, 'POST', body)
export const saveProductRecipe = (id: number, ingredients: { ingredientId: number; quantity: number }[]) => adminJson<RecipeView>(`/inventory/recipes/products/${id}`, 'PUT', { ingredients })
export const saveExtraRecipe = (id: number, ingredients: { ingredientId: number; quantity: number }[]) => adminJson<RecipeView>(`/inventory/recipes/extras/${id}`, 'PUT', { ingredients })
