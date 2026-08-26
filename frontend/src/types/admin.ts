export type AdminRole = 'ADMIN' | 'CASHIER' | 'KITCHEN'
export type OrderStatus = 'NEW' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'ON_THE_WAY' | 'DELIVERED' | 'CANCELLED'
export type PaymentStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type PaymentMethod = 'CASH' | 'TRANSFER'
export type DeliveryType = 'DELIVERY' | 'PICKUP'

export interface AdminUser { id: number; name: string; email: string; role: AdminRole }
export interface OrderSummary { publicNumber: string; customerName: string; createdAt: string; deliveryType: DeliveryType; total: number; paymentMethod?: PaymentMethod; paymentStatus?: PaymentStatus; orderStatus: OrderStatus }
export interface Dashboard { salesToday: number; ordersToday: number; newOrders: number; preparingOrders: number; paymentsUnderReview: number; averageTicket: number; latestOrders: OrderSummary[]; topProducts: { name: string; quantity: number }[] }
export interface PaymentView { id: number; method: PaymentMethod; status: PaymentStatus; amount: number; cashTendered?: number; receiptAvailable: boolean; reviewerName?: string; reviewedAt?: string; rejectionReason?: string }
export interface OrderDetail extends Omit<OrderSummary, 'orderStatus' | 'paymentMethod' | 'paymentStatus'> { customerPhone: string; customerEmail?: string; deliveryAddress?: string; subtotal: number; discount: number; deliveryFee: number; status: OrderStatus; items: { name: string; unitPrice: number; quantity: number; notes?: string; subtotal: number; extras: { name: string; quantity: number; subtotal: number }[] }[]; payment?: PaymentView; timeline: Record<string, string | undefined> }
export interface PaymentQueueItem { publicNumber: string; customerName: string; createdAt: string; amount: number; method: PaymentMethod; status: PaymentStatus; receiptAvailable: boolean }
export interface ProductView { id: number; name: string; slug: string; description: string; price: number; imagePath?: string; available: boolean; featured: boolean; categoryId: number; categoryName: string; extraIds: number[] }
export interface CategoryView { id: number; name: string; slug: string; description?: string; displayOrder: number; active: boolean }
export interface ExtraView { id: number; name: string; price: number; available: boolean }
export interface PromotionView { id: number; name: string; description?: string; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; startsAt: string; endsAt: string; minimumPurchase: number; usageLimit?: number; active: boolean }
export interface HoursView { id?: number; dayOfWeek: string; slotNumber: number; opensAt?: string; closesAt?: string; closed: boolean }
export interface SettingsView { tradeName: string; description?: string; phone: string; whatsapp: string; address: string; instagram?: string; facebook?: string; baseDeliveryFee: number; estimatedPreparationMinutes: number; timeZone: string; transferProvider?: string; transferAccountHolder?: string; transferAccountReference?: string; paymentQrPath?: string; hours: HoursView[] }
