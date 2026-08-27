import type { DeliveryType } from './admin'

export type KitchenStage = 'NEW' | 'PREPARING' | 'READY'
export interface KitchenExtra { name: string; quantity: number }
export interface KitchenItem { name: string; quantity: number; notes?: string; extras: KitchenExtra[] }
export interface KitchenOrder { publicNumber: string; createdAt: string; stage: KitchenStage; deliveryType: DeliveryType; notes?: string; items: KitchenItem[] }
