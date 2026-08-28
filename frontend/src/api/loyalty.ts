import { adminJson, adminRequest } from './admin'
import type { CouponView, CustomerProfile, CustomerSummary, LoyaltySettingsView, RepeatOrderResponse } from '../types/admin'

export const listCustomers = () => adminRequest<CustomerSummary[]>('/customers')
export const getCustomer = (id: number) => adminRequest<CustomerProfile>(`/customers/${id}`)
export const adjustCustomerPoints = (id: number, points: number, reason: string) => adminJson<CustomerProfile>(`/customers/${id}/points`, 'POST', { points, reason })
export const repeatCustomerOrder = (id: number, order: string) => adminRequest<RepeatOrderResponse>(`/customers/${id}/orders/${encodeURIComponent(order)}/repeat`)
export const listCoupons = () => adminRequest<CouponView[]>('/coupons')
export const saveCoupon = (coupon: Partial<CouponView> & { code: string }, id?: number) => adminJson<CouponView>(id ? `/coupons/${id}` : '/coupons', id ? 'PUT' : 'POST', coupon)
export const getLoyaltySettings = () => adminRequest<LoyaltySettingsView>('/loyalty/settings')
export const saveLoyaltySettings = (settings: Omit<LoyaltySettingsView, 'id'>) => adminJson<LoyaltySettingsView>('/loyalty/settings', 'PUT', settings)
