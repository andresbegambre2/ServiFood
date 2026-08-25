import { getJson } from './client'
import type { Business, Category, Product, ProductDetail, Promotion, StorefrontData } from '../types/public'

export async function getStorefront(signal?: AbortSignal): Promise<StorefrontData> {
  const [business, categories, products, promotions] = await Promise.all([
    getJson<Business>('/public/business', signal), getJson<Category[]>('/public/categories', signal),
    getJson<Product[]>('/public/products', signal), getJson<Promotion[]>('/public/promotions', signal),
  ])
  return { business, categories, products, promotions }
}

export const getProduct = (slug: string, signal?: AbortSignal) => getJson<ProductDetail>(`/public/products/${encodeURIComponent(slug)}`, signal)
