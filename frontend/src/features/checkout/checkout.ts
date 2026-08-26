import type { CartLine } from '../cart/cart'
import type { CheckoutLineRequest, CreatedOrder, DeliveryType, PaymentMethod } from '../../types/public'
import { fromMinorUnits } from '../../utils/money'

export const RECEIPT_MAX_BYTES = 5 * 1024 * 1024
export const RECEIPT_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const
export const trackingStorageKey = (publicNumber: string) => `servifood:tracking:${publicNumber}`

export interface CheckoutDraft {
  name: string; phone: string; email: string; deliveryType: DeliveryType
  address: string; neighborhood: string; reference: string
  paymentMethod: PaymentMethod; cashTendered: string
}

export function paymentMethodsFor(deliveryType: DeliveryType, transferConfigured: boolean): PaymentMethod[] {
  const methods: PaymentMethod[] = deliveryType === 'DELIVERY' ? ['CASH'] : ['PAY_ON_PICKUP']
  if (transferConfigured) methods.push('TRANSFER')
  return methods
}

export function buildCheckoutLines(lines: CartLine[], includeExpectedPrices = true): CheckoutLineRequest[] {
  return lines.map((line) => ({ productId: line.productId, quantity: line.quantity, notes: line.notes,
    ...(includeExpectedPrices ? { expectedUnitPrice: fromMinorUnits(line.unitPriceMinor) } : {}),
    extras: line.extras.map((extra) => ({ extraId: extra.id, ...(includeExpectedPrices ? { expectedUnitPrice: fromMinorUnits(extra.unitPriceMinor) } : {}) })),
  }))
}

export function validateDraft(draft: CheckoutDraft): string | null {
  if (!draft.name.trim() || draft.name.trim().length > 120) return 'Escribe tu nombre.'
  if (!/^[0-9+() .-]{7,30}$/.test(draft.phone.trim())) return 'Revisa el número de teléfono.'
  if (draft.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(draft.email.trim())) return 'Revisa el correo electrónico.'
  if (draft.deliveryType === 'DELIVERY' && (!draft.address.trim() || !draft.neighborhood.trim())) return 'Completa dirección y barrio.'
  if (draft.paymentMethod === 'CASH' && draft.cashTendered && (!Number.isFinite(Number(draft.cashTendered)) || Number(draft.cashTendered) < 0)) return 'Revisa el valor en efectivo.'
  return null
}

export function validateReceipt(file: Pick<File, 'name' | 'type' | 'size'> | null, required: boolean): string | null {
  if (!file) return required ? 'Adjunta el comprobante de transferencia.' : null
  if (!RECEIPT_TYPES.includes(file.type as typeof RECEIPT_TYPES[number])) return 'Usa una imagen JPG, PNG o WEBP.'
  if (!/\.(jpe?g|png|webp)$/i.test(file.name)) return 'La extensión del archivo no es válida.'
  if (file.size > RECEIPT_MAX_BYTES) return 'La imagen no puede superar 5 MB.'
  return null
}

export async function completeOrderCreation(
  create: () => Promise<CreatedOrder>,
  effects: { storeTracking: (order: CreatedOrder) => void; clearCart: () => void; openConfirmation: (order: CreatedOrder) => void },
): Promise<CreatedOrder> {
  const order = await create()
  effects.storeTracking(order)
  effects.clearCart()
  effects.openConfirmation(order)
  return order
}
