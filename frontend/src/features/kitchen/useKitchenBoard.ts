import { useCallback, useEffect, useRef, useState } from 'react'
import { getKitchenOrders, transitionKitchenOrder } from '../../api/kitchen'
import type { KitchenOrder, KitchenStage } from '../../types/kitchen'
import { createKitchenPolling, updateKitchenStage } from './kitchen'

export function useKitchenBoard() {
  const [orders, setOrders] = useState<KitchenOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [feedback, setFeedback] = useState('')
  const [lastUpdated, setLastUpdated] = useState<Date>()
  const [pending, setPending] = useState<string>()
  const [clock, setClock] = useState(() => Date.now())
  const loadingRef = useRef(false)
  const mutationRef = useRef(false)

  const refresh = useCallback(async () => {
    if (loadingRef.current || mutationRef.current) return
    loadingRef.current = true
    try { setOrders(await getKitchenOrders()); setError(''); setLastUpdated(new Date()) }
    catch { setError('No pudimos actualizar los pedidos. La pantalla reintentará automáticamente.') }
    finally { loadingRef.current = false; setLoading(false) }
  }, [])

  useEffect(() => {
    let active = true
    loadingRef.current = true
    getKitchenOrders().then(values => { if (active) { setOrders(values); setError(''); setLastUpdated(new Date()) } })
      .catch(() => { if (active) setError('No pudimos actualizar los pedidos. La pantalla reintentará automáticamente.') })
      .finally(() => { loadingRef.current = false; if (active) setLoading(false) })
    const stop = createKitchenPolling(() => void refresh(), () => document.hidden)
    const visible = () => { if (!document.hidden) void refresh() }
    document.addEventListener('visibilitychange', visible)
    return () => { active = false; stop(); document.removeEventListener('visibilitychange', visible) }
  }, [refresh])

  useEffect(() => { const timer = window.setInterval(() => setClock(Date.now()), 60_000); return () => window.clearInterval(timer) }, [])

  async function transition(order: KitchenOrder, target: KitchenStage) {
    if (mutationRef.current) return
    const previous = orders
    mutationRef.current = true; setPending(order.publicNumber); setError(''); setFeedback('')
    setOrders(values => updateKitchenStage(values, order.publicNumber, target))
    try {
      const updated = await transitionKitchenOrder(order.publicNumber, target)
      setOrders(values => values.map(value => value.publicNumber === order.publicNumber ? updated : value))
      setFeedback(`${order.publicNumber} pasó a ${target === 'PREPARING' ? 'En preparación' : 'Listo'}.`)
      setLastUpdated(new Date())
    } catch {
      setOrders(previous)
      setError(`No se pudo actualizar ${order.publicNumber}. El pedido permanece en su estado anterior.`)
    } finally { mutationRef.current = false; setPending(undefined) }
  }

  return { orders, loading, error, feedback, lastUpdated, pending, clock, refresh, transition }
}
