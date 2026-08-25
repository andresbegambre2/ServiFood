import { getJson, postForm, postJson } from './client'
import type { CheckoutQuote, CheckoutQuoteRequest, CreateOrderRequest, CreatedOrder, TrackedOrder } from '../types/public'

export const quoteOrder = (request: CheckoutQuoteRequest, signal?: AbortSignal) => postJson<CheckoutQuoteRequest, CheckoutQuote>('/public/orders/quote', request, signal)
export async function createOrder(request: CreateOrderRequest, receipt: File | null) {
  const form = new FormData()
  form.append('order', new Blob([JSON.stringify(request)], { type: 'application/json' }))
  if (receipt) form.append('receipt', receipt)
  return postForm<CreatedOrder>('/public/orders', form)
}
export const trackOrder = (publicNumber: string, token: string, signal?: AbortSignal) => getJson<TrackedOrder>(`/public/orders/${encodeURIComponent(publicNumber)}?token=${encodeURIComponent(token)}`, signal)
