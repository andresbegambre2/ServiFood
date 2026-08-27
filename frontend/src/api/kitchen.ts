import { sessionRequest } from './admin'
import type { KitchenOrder, KitchenStage } from '../types/kitchen'

export const getKitchenOrders = () => sessionRequest<KitchenOrder[]>('/kitchen/orders')

export const transitionKitchenOrder = (publicNumber: string, target: KitchenStage) =>
  sessionRequest<KitchenOrder>(`/kitchen/orders/${encodeURIComponent(publicNumber)}/stage`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ target }),
  })
