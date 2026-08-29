import { useEffect, useState } from 'react'
import { adminJson, adminRequest } from '../api/admin'
import type { KitchenOrder, OrderStatus } from '../types/admin'
import { dateTime, label } from './adminFormat'
import { AdminError, AdminLoading, EmptyState, StatusBadge } from './adminUi'
import { PageTitle } from './AdminDashboardPage'

const columns: { status: OrderStatus[]; title: string; empty: string }[] = [
  { status: ['NEW', 'CONFIRMED'], title: 'Nuevos', empty: 'No hay pedidos por iniciar.' },
  { status: ['PREPARING'], title: 'En preparación', empty: 'No hay pedidos en preparación.' },
  { status: ['READY'], title: 'Listos', empty: 'No hay pedidos listos para entregar.' },
]

export function KitchenPage() {
  const [orders, setOrders] = useState<KitchenOrder[]>(); const [error, setError] = useState(''); const [busy, setBusy] = useState('')
  const load = () => { setError(''); adminRequest<KitchenOrder[]>('/kitchen/orders').then(setOrders).catch(() => setError('No pudimos cargar los pedidos de cocina.')) }
  useEffect(() => { adminRequest<KitchenOrder[]>('/kitchen/orders').then(setOrders).catch(() => setError('No pudimos cargar los pedidos de cocina.')) }, [])
  async function advance(order: KitchenOrder) {
    const target: OrderStatus = ['NEW', 'CONFIRMED'].includes(order.status) ? 'PREPARING' : 'READY'
    setBusy(order.publicNumber); setError('')
    try { await adminJson<KitchenOrder>(`/kitchen/orders/${encodeURIComponent(order.publicNumber)}/status`, 'PATCH', { status: target }); load() }
    catch { setError('No fue posible actualizar el pedido. Revisa su estado e inténtalo de nuevo.') }
    finally { setBusy('') }
  }
  if (error && !orders) return <AdminError message={error} retry={load} />
  if (!orders) return <AdminLoading label="Preparando la vista de cocina…" />
  return <><PageTitle eyebrow="PRODUCCIÓN" title="Cocina" action={<button className="secondary" onClick={load}>Actualizar pedidos</button>} />
    {error && <div className="admin-alert" role="alert">{error}</div>}
    <p className="kitchen-note">Los pedidos se actualizan al usar el botón. Así evitamos consultas automáticas innecesarias durante la operación.</p>
    <section className="kitchen-board">{columns.map(column => { const values = orders.filter(order => column.status.includes(order.status)); return <section className="kitchen-column" key={column.title}><header><h2>{column.title}</h2><span>{values.length}</span></header>{values.length ? values.map(order => <article className="kitchen-ticket" key={order.publicNumber}><div className="panel-title"><div><strong>{order.publicNumber}</strong><small>{dateTime(order.createdAt)} · {label(order.deliveryType)}</small></div><StatusBadge value={order.status} /></div><ul>{order.items.map((item, index) => <li key={`${item.name}-${index}`}><strong>{item.quantity} × {item.name}</strong>{item.extras.map(extra => <small key={extra}>+ {extra}</small>)}{item.notes && <em>“{item.notes}”</em>}</li>)}</ul>{order.status !== 'READY' && <button className="primary block" disabled={busy === order.publicNumber} onClick={() => void advance(order)}>{busy === order.publicNumber ? 'Actualizando…' : ['NEW', 'CONFIRMED'].includes(order.status) ? 'Iniciar preparación' : 'Marcar como listo'}</button>}</article>) : <EmptyState text={column.empty} />}</section> })}</section>
  </>
}
