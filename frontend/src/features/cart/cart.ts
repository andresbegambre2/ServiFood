export const CART_STORAGE_KEY = 'servifood:cart'
export const CART_VERSION = 1

export interface CartExtra { id: number; name: string; unitPriceMinor: number }
export interface CartLine { id: string; productId: number; slug: string; name: string; imagePath: string | null; unitPriceMinor: number; quantity: number; notes: string; extras: CartExtra[] }
export interface CartState { lines: CartLine[] }
export type CartAction = { type: 'add'; line: CartLine } | { type: 'remove'; id: string } | { type: 'quantity'; id: string; quantity: number } | { type: 'clear' }

export function cartReducer(state: CartState, action: CartAction): CartState {
  if (action.type === 'add') return { lines: [...state.lines, { ...action.line, quantity: Math.max(1, Math.floor(action.line.quantity)), notes: action.line.notes.slice(0, 240) }] }
  if (action.type === 'remove') return { lines: state.lines.filter((line) => line.id !== action.id) }
  if (action.type === 'quantity') return { lines: state.lines.map((line) => line.id === action.id ? { ...line, quantity: Math.max(1, Math.floor(action.quantity)) } : line) }
  return { lines: [] }
}

export const lineTotal = (line: CartLine) => (line.unitPriceMinor + line.extras.reduce((sum, extra) => sum + extra.unitPriceMinor, 0)) * line.quantity
export const cartSubtotal = (state: CartState) => state.lines.reduce((sum, line) => sum + lineTotal(line), 0)
export const cartUnits = (state: CartState) => state.lines.reduce((sum, line) => sum + line.quantity, 0)

export function parseStoredCart(value: string | null): CartState {
  if (!value) return { lines: [] }
  try {
    const parsed: unknown = JSON.parse(value)
    if (!isStoredCart(parsed)) return { lines: [] }
    return { lines: parsed.lines.map((line) => ({ ...line, quantity: Math.max(1, Math.floor(line.quantity)), notes: line.notes.slice(0, 240) })) }
  } catch { return { lines: [] } }
}

function isStoredCart(value: unknown): value is { version: number; lines: CartLine[] } {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Record<string, unknown>
  return candidate.version === CART_VERSION && Array.isArray(candidate.lines) && candidate.lines.every(isCartLine)
}
function isCartLine(value: unknown): value is CartLine {
  if (!value || typeof value !== 'object') return false
  const line = value as Record<string, unknown>
  return typeof line.id === 'string' && typeof line.productId === 'number' && typeof line.slug === 'string' && typeof line.name === 'string' && typeof line.unitPriceMinor === 'number' && Number.isSafeInteger(line.unitPriceMinor) && typeof line.quantity === 'number' && line.quantity > 0 && line.quantity <= 99 && typeof line.notes === 'string' && Array.isArray(line.extras) && line.extras.every((extra) => !!extra && typeof extra === 'object' && typeof (extra as Record<string, unknown>).id === 'number' && typeof (extra as Record<string, unknown>).name === 'string' && typeof (extra as Record<string, unknown>).unitPriceMinor === 'number')
}
