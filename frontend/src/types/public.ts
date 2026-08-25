export interface Category { id: number; name: string; slug: string; description: string | null }
export interface Extra { id: number; name: string; description: string | null; price: number }
export interface Product { id: number; name: string; slug: string; description: string; price: number; imagePath: string | null; available: boolean; featured: boolean; category: Category }
export interface ProductDetail extends Product { allowedExtras: Extra[] }
export interface Promotion { id: number; name: string; description: string | null; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; startsAt: string; endsAt: string; minimumPurchase: number }
export interface BusinessHours { dayOfWeek: string; slotNumber: number; opensAt: string | null; closesAt: string | null; closed: boolean }
export interface TransferPayment { provider: string | null; accountHolder: string | null; accountReference: string | null; qrPath: string | null; configured: boolean }
export interface Business { tradeName: string; description: string | null; logoPath: string | null; phone: string; whatsapp: string; address: string; instagram: string | null; facebook: string | null; baseDeliveryFee: number; estimatedPreparationMinutes: number; currency: string; timeZone: string; transfer?: TransferPayment | null; hours: BusinessHours[] }
export interface StorefrontData { business: Business; categories: Category[]; products: Product[]; promotions: Promotion[] }

export type DeliveryType = 'DELIVERY' | 'PICKUP'
export type PaymentMethod = 'CASH' | 'TRANSFER' | 'PAY_ON_PICKUP'
export type PaymentStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type OrderStatus = 'NEW' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'ON_THE_WAY' | 'DELIVERED' | 'CANCELLED'
export interface CheckoutExtraRequest { extraId: number; expectedUnitPrice?: number }
export interface CheckoutLineRequest { productId: number; quantity: number; notes: string; expectedUnitPrice?: number; extras: CheckoutExtraRequest[] }
export interface CheckoutQuoteRequest { deliveryType: DeliveryType; lines: CheckoutLineRequest[] }
export interface CheckoutCustomerRequest { name: string; phone: string; email: string | null }
export interface CheckoutDeliveryRequest { type: DeliveryType; address: string | null; neighborhood: string | null; reference: string | null }
export interface CheckoutPaymentRequest { method: PaymentMethod; cashTendered: number | null }
export interface CreateOrderRequest { clientRequestId: string; customer: CheckoutCustomerRequest; delivery: CheckoutDeliveryRequest; payment: CheckoutPaymentRequest; lines: CheckoutLineRequest[] }
export interface OrderExtraSnapshot { name: string; unitPrice: number; quantity: number; subtotal: number }
export interface OrderItemSnapshot { name: string; unitPrice: number; quantity: number; notes: string | null; subtotal: number; extras: OrderExtraSnapshot[] }
export interface OrderTotals { subtotal: number; discount: number; deliveryFee: number; total: number; currency: string; estimatedMinutes: number }
export interface CheckoutQuote { totals: OrderTotals; items: OrderItemSnapshot[] }
export interface CreatedOrder { publicNumber: string; trackingToken: string; status: OrderStatus; paymentMethod: PaymentMethod; paymentStatus: PaymentStatus; deliveryType: DeliveryType; deliveryAddress: string | null; customerName: string; createdAt: string; totals: OrderTotals; items: OrderItemSnapshot[]; businessWhatsapp: string; idempotent: boolean }
export type TrackedOrder = Omit<CreatedOrder, 'trackingToken' | 'idempotent'>
