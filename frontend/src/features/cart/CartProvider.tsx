import { useEffect, useMemo, useReducer, useState, type ReactNode } from 'react'
import { CART_STORAGE_KEY, CART_VERSION, cartReducer, cartSubtotal, cartUnits, parseStoredCart } from './cart'
import { CartContext, type CartContextValue } from './cart-context'
export function CartProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(cartReducer, undefined, () => parseStoredCart(localStorage.getItem(CART_STORAGE_KEY)))
  const [isOpen, setOpen] = useState(false)
  useEffect(() => { localStorage.setItem(CART_STORAGE_KEY, JSON.stringify({ version: CART_VERSION, lines: state.lines })) }, [state.lines])
  const value = useMemo<CartContextValue>(() => ({ lines: state.lines, units: cartUnits(state), subtotal: cartSubtotal(state), isOpen,
    add: (line) => dispatch({ type: 'add', line }), remove: (id) => dispatch({ type: 'remove', id }), setQuantity: (id, quantity) => dispatch({ type: 'quantity', id, quantity }), clear: () => dispatch({ type: 'clear' }), open: () => setOpen(true), close: () => setOpen(false),
  }), [state, isOpen])
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}
