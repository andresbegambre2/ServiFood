import { createContext, useContext } from 'react'
import type { CartLine } from './cart'
export interface CartContextValue { lines: CartLine[]; units: number; subtotal: number; isOpen: boolean; add: (line: CartLine) => void; remove: (id: string) => void; setQuantity: (id: string, quantity: number) => void; clear: () => void; open: () => void; close: () => void }
export const CartContext = createContext<CartContextValue | null>(null)
export function useCart() { const value = useContext(CartContext); if (!value) throw new Error('useCart must be used within CartProvider'); return value }
