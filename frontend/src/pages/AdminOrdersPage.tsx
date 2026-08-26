import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { adminRequest } from '../api/admin'
import type { OrderSummary } from '../types/admin'
import { money } from '../utils/money'
import { AdminError, AdminLoading, EmptyState, StatusBadge } from './adminUi'
import { label, time } from './adminFormat'
import { PageTitle } from './AdminDashboardPage'

export function AdminOrdersPage() {
  const [search, setSearch] = useSearchParams(); const [orders, setOrders] = useState<OrderSummary[]>(); const [error, setError] = useState('')
  const [query, setQuery] = useState(search.get('query') ?? '')
  const load = () => { setError(''); const suffix = search.toString() ? `?${search}` : ''; adminRequest<OrderSummary[]>(`/orders${suffix}`).then(setOrders).catch(() => setError('Revisa tu conexión e inténtalo nuevamente.')) }
  useEffect(() => { const suffix = search.toString() ? `?${search}` : ''; adminRequest<OrderSummary[]>(`/orders${suffix}`).then(setOrders).catch(() => setError('Revisa tu conexión e inténtalo nuevamente.')) }, [search])
  function apply(event: FormEvent) { event.preventDefault(); const next = new URLSearchParams(search); if (query) next.set('query', query); else next.delete('query'); setSearch(next) }
  function filter(key: string, value: string) { const next = new URLSearchParams(search); if (value) next.set(key, value); else next.delete(key); setSearch(next) }
  return <><PageTitle eyebrow="OPERACIÓN" title="Pedidos" action={<button className="secondary" onClick={load}>Actualizar lista</button>} />
    <form className="filter-bar" onSubmit={apply}><label className="search-field">Buscar pedido o cliente<input value={query} onChange={e => setQuery(e.target.value)} placeholder="Ej. SF-123 o Andrea" /></label>
      <label>Estado<select value={search.get('status') ?? ''} onChange={e => filter('status', e.target.value)}><option value="">Todos</option>{['NEW','CONFIRMED','PREPARING','READY','ON_THE_WAY','DELIVERED','CANCELLED'].map(v => <option value={v} key={v}>{label(v)}</option>)}</select></label>
      <label>Pago<select value={search.get('paymentMethod') ?? ''} onChange={e => filter('paymentMethod', e.target.value)}><option value="">Todos</option><option value="CASH">Efectivo</option><option value="TRANSFER">Transferencia</option></select></label>
      <label>Entrega<select value={search.get('deliveryType') ?? ''} onChange={e => filter('deliveryType', e.target.value)}><option value="">Todos</option><option value="DELIVERY">Domicilio</option><option value="PICKUP">Recoger</option></select></label>
      <label>Fecha<input type="date" value={search.get('date') ?? ''} onChange={e => filter('date', e.target.value)} /></label><button className="primary">Buscar</button></form>
    {error ? <AdminError message={error} retry={load} /> : !orders ? <AdminLoading label="Cargando pedidos…" /> : !orders.length ? <EmptyState text="No hay pedidos para estos filtros." /> : <section className="admin-table-card"><table><thead><tr><th>Pedido</th><th>Cliente</th><th>Hora</th><th>Entrega</th><th>Pago</th><th>Total</th><th>Estado</th><th /></tr></thead><tbody>{orders.map(order => <tr key={order.publicNumber} className={order.orderStatus === 'NEW' ? 'priority-row' : ''}><td data-label="Pedido"><strong>{order.publicNumber}</strong>{order.orderStatus === 'NEW' && <small className="new-label">NUEVO</small>}</td><td data-label="Cliente">{order.customerName}</td><td data-label="Hora">{time(order.createdAt)}</td><td data-label="Entrega">{label(order.deliveryType)}</td><td data-label="Pago"><span>{label(order.paymentMethod)}</span><StatusBadge value={order.paymentStatus} /></td><td data-label="Total"><strong>{money(order.total)}</strong></td><td data-label="Estado"><StatusBadge value={order.orderStatus} /></td><td><Link className="row-link" to={`/admin/orders/${order.publicNumber}`}>Ver detalle →</Link></td></tr>)}</tbody></table></section>}
  </>
}
