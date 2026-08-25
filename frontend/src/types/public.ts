export interface Category { id: number; name: string; slug: string; description: string | null }
export interface Extra { id: number; name: string; description: string | null; price: number }
export interface Product { id: number; name: string; slug: string; description: string; price: number; imagePath: string | null; available: boolean; featured: boolean; category: Category }
export interface ProductDetail extends Product { allowedExtras: Extra[] }
export interface Promotion { id: number; name: string; description: string | null; discountType: 'PERCENTAGE' | 'FIXED_AMOUNT'; discountValue: number; startsAt: string; endsAt: string; minimumPurchase: number }
export interface BusinessHours { dayOfWeek: string; slotNumber: number; opensAt: string | null; closesAt: string | null; closed: boolean }
export interface Business { tradeName: string; description: string | null; logoPath: string | null; phone: string; whatsapp: string; address: string; instagram: string | null; facebook: string | null; baseDeliveryFee: number; estimatedPreparationMinutes: number; currency: string; hours: BusinessHours[] }
export interface StorefrontData { business: Business; categories: Category[]; products: Product[]; promotions: Promotion[] }
