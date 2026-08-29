export type AdminRole = 'ADMIN' | 'CASHIER' | 'KITCHEN'
export type OrderStatus = 'NEW' | 'CONFIRMED' | 'PREPARING' | 'READY' | 'ON_THE_WAY' | 'DELIVERED' | 'CANCELLED'
export type PaymentStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED'
export type PaymentMethod = 'CASH' | 'TRANSFER' | 'PAY_ON_PICKUP'
export type DeliveryType = 'DELIVERY' | 'PICKUP'

export interface AdminUser { id: number; name: string; email: string; role: AdminRole }
export interface OrderSummary { publicNumber: string; customerName: string; createdAt: string; deliveryType: DeliveryType; total: number; paymentMethod?: PaymentMethod; paymentStatus?: PaymentStatus; orderStatus: OrderStatus }
export interface Dashboard { salesToday: number; ordersToday: number; newOrders: number; preparingOrders: number; paymentsUnderReview: number; lowStockIngredients: number; outOfStockIngredients: number; averageTicket: number; latestOrders: OrderSummary[]; topProducts: { name: string; quantity: number }[] }
export interface PaymentView { id: number; method: PaymentMethod; status: PaymentStatus; amount: number; cashTendered?: number; receiptAvailable: boolean; reviewerName?: string; reviewedAt?: string; rejectionReason?: string }
export interface OrderDetail extends Omit<OrderSummary, 'orderStatus' | 'paymentMethod' | 'paymentStatus'> { customerPhone: string; customerEmail?: string; deliveryAddress?: string; subtotal: number; discount: number; deliveryFee: number; status: OrderStatus; items: { name: string; unitPrice: number; quantity: number; notes?: string; subtotal: number; extras: { name: string; quantity: number; subtotal: number }[] }[]; payment?: PaymentView; timeline: Record<string, string | undefined> }
export interface PaymentQueueItem { publicNumber: string; customerName: string; createdAt: string; amount: number; method: PaymentMethod; status: PaymentStatus; receiptAvailable: boolean }
export interface ProductView { id: number; name: string; slug: string; description: string; price: number; imagePath?: string; available: boolean; featured: boolean; categoryId: number; categoryName: string; extraIds: number[] }
export interface CategoryView { id: number; name: string; slug: string; description?: string; displayOrder: number; active: boolean }
export interface ExtraView { id: number; name: string; price: number; available: boolean }
export interface PromotionView { id: number; name: string; description?: string; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; startsAt: string; endsAt: string; minimumPurchase: number; usageLimit?: number; active: boolean }
export interface HoursView { id?: number; dayOfWeek: string; slotNumber: number; opensAt?: string; closesAt?: string; closed: boolean }
export interface SettingsView { tradeName: string; description?: string; phone: string; whatsapp: string; address: string; instagram?: string; facebook?: string; baseDeliveryFee: number; estimatedPreparationMinutes: number; timeZone: string; transferProvider?: string; transferAccountHolder?: string; transferAccountReference?: string; paymentQrPath?: string; hours: HoursView[] }
export type IngredientUnit = 'GRAM' | 'MILLILITER' | 'UNIT'
export type StockStatus = 'OK' | 'LOW' | 'OUT' | 'INACTIVE'
export type InventoryMovementType = 'ENTRY' | 'CONSUMPTION' | 'ADJUSTMENT' | 'REVERSAL'
export interface IngredientView { id: number; name: string; unit: IngredientUnit; stockCurrent: number; stockMinimum: number; unitCost?: number; active: boolean; stockStatus: StockStatus }
export interface RecipeLine { ingredientId: number; ingredientName: string; unit: IngredientUnit; quantity: number }
export interface RecipeView { targetId: number; targetName: string; effectiveAvailable: boolean; ingredients: RecipeLine[] }
export interface InventoryMovementView { id: number; ingredientId: number; ingredientName: string; type: InventoryMovementType; quantityDelta: number; balanceAfter: number; reason: string; orderNumber?: string; createdBy?: string; createdAt: string }
export interface InventoryOverview { trackedIngredients: number; lowStockCount: number; outOfStockCount: number; ingredients: IngredientView[]; productRecipes: RecipeView[]; extraRecipes: RecipeView[]; recentMovements: InventoryMovementView[] }
export interface CustomerSummary { id: number; name: string; phone: string; points: number; orderCount: number; totalSpent: number; lastOrderAt?: string }
export interface CustomerAddressView { id: number; label?: string; address: string; neighborhood: string; reference?: string; primary: boolean }
export interface CustomerOrderView { publicNumber: string; createdAt: string; status: OrderStatus; total: number; discount: number; couponCode?: string; pointsRedeemed: number; pointsEarned: number }
export interface PointMovementView { id: number; type: 'EARN' | 'REDEEM' | 'ADJUSTMENT' | 'REVERSAL_EARN' | 'REVERSAL_REDEEM'; pointsDelta: number; balanceAfter: number; reason: string; orderNumber?: string; createdBy?: string; createdAt: string }
export interface CustomerProfile { id: number; name: string; phone: string; email?: string; points: number; orderCount: number; totalSpent: number; lastOrderAt?: string; addresses: CustomerAddressView[]; orders: CustomerOrderView[]; frequentProducts: { productId: number; name: string; quantity: number }[]; pointMovements: PointMovementView[] }
export interface CouponView { id: number; code: string; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; startsAt: string; endsAt: string; minimumPurchase: number; totalUsageLimit?: number; perCustomerUsageLimit?: number; uses: number; active: boolean }
export interface LoyaltySettingsView { id: number; amountPerPoint: number; minimumPointsToRedeem: number; maximumRedemptionPercentage: number; active: boolean }
export interface RepeatOrderResponse { lines: { productId: number; slug: string; name: string; imagePath: string | null; price: number; quantity: number; notes: string; extras: { id: number; name: string; price: number }[] }[] }
export type ReportType = 'SALES' | 'ORDERS' | 'PRODUCTS' | 'CUSTOMERS' | 'PROMOTIONS' | 'COUPONS' | 'PAYMENTS'
export interface PeriodMetric { label: string; from: string; to: string; sales: number; previousSales: number; changePercentage: number; orders: number }
export interface RankedMetric { label: string; quantity: number; amount: number }
export interface AnalyticsOverview { today: PeriodMetric; yesterday: PeriodMetric; week: PeriodMetric; month: PeriodMetric; totalOrders: number; cancelledOrders: number; averageTicket: number; discountsApplied: number; couponUses: number; pointsEarned: number; pointsRedeemed: number; lowStockIngredients: number; outOfStockIngredients: number; topProducts: RankedMetric[]; topCategories: RankedMetric[]; peakHours: RankedMetric[]; paymentMethods: RankedMetric[]; deliveryTypes: RankedMetric[]; topCustomers: { customerId: number; name: string; orders: number; amount: number }[]; salesEvolution: { date: string; sales: number; orders: number }[] }
export interface ReportData { type: ReportType; from: string; to: string; columns: string[]; rows: unknown[][] }
