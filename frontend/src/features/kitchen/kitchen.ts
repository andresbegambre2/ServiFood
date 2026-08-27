import type { KitchenOrder, KitchenStage } from '../../types/kitchen'

export const kitchenStages: KitchenStage[] = ['NEW', 'PREPARING', 'READY']
export const kitchenLabels: Record<KitchenStage, string> = { NEW: 'Nuevos', PREPARING: 'En preparación', READY: 'Listos' }
export const deliveryLabels = { DELIVERY: 'Domicilio', PICKUP: 'Recoger en local' } as const
export const nextKitchenStage: Partial<Record<KitchenStage, KitchenStage>> = { NEW: 'PREPARING', PREPARING: 'READY' }

export function groupKitchenOrders(orders: KitchenOrder[]) {
  return Object.fromEntries(kitchenStages.map(stage => [stage, orders.filter(order => order.stage === stage)
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())])) as Record<KitchenStage, KitchenOrder[]>
}

export function elapsedMinutes(createdAt: string, now = Date.now()) { return Math.max(0, Math.floor((now - new Date(createdAt).getTime()) / 60_000)) }
export function elapsedLabel(createdAt: string, now = Date.now()) { const minutes = elapsedMinutes(createdAt, now); return minutes < 1 ? 'Hace menos de 1 min' : `Hace ${minutes} min` }
export function isDelayed(createdAt: string, now = Date.now()) { return elapsedMinutes(createdAt, now) >= 20 }
export function updateKitchenStage(orders: KitchenOrder[], publicNumber: string, stage: KitchenStage) { return orders.map(order => order.publicNumber === publicNumber ? { ...order, stage } : order) }

export function createKitchenPolling(refresh: () => void, isHidden: () => boolean, intervalMs = 12_000) {
  const timer = setInterval(() => { if (!isHidden()) refresh() }, intervalMs)
  return () => clearInterval(timer)
}
