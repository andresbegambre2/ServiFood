import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminRequest } from '../api/admin'
import type { Dashboard } from '../types/admin'
import { money } from '../utils/money'
import { AdminError, AdminLoading, EmptyState, StatusBadge } from './adminUi'
import { time } from './adminFormat'

export function AdminDashboardPage() {
  const [data, setData] = useState<Dashboard>(); const [error, setError] = useState('')
  const load = () => { setError(''); adminRequest<Dashboard>('/dashboard').then(setData).catch(() => setError('No pudimos cargar el resumen operativo.')) }
  useEffect(() => { adminRequest<Dashboard>('/dashboard').then(setData).catch(() => setError('No pudimos cargar el resumen operativo.')) }, [])
  if (error) return <AdminError message={error} retry={load} />
  if (!data) return <AdminLoading label="Preparando el dashboard…" />
  const cards = [['Pedidos hoy', data.ordersToday, 'Actividad del día'], ['Ventas hoy', money(data.salesToday), 'Pedidos no cancelados'], ['Nuevos', data.newOrders, 'Requieren atención'], ['En preparación', data.preparingOrders, 'En cocina'], ['Pagos por revisar', data.paymentsUnderReview, 'Transferencias'], ['Ticket promedio', money(data.averageTicket), 'Promedio del día']]
  return <><PageTitle eyebrow="RESUMEN OPERATIVO" title="Dashboard" action={<button className="secondary" onClick={load}>Actualizar</button>} />
    <section className="metric-grid">{cards.map(([label, value, note]) => <article className="metric-card" key={label}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</section>
    <section className="admin-columns"><article className="admin-panel"><div className="panel-title"><div><p className="eyebrow">EN TIEMPO REAL</p><h2>Últimos pedidos</h2></div><Link to="/admin/orders">Ver todos</Link></div>{data.latestOrders.length ? <div className="order-list">{data.latestOrders.map(order => <Link to={`/admin/orders/${order.publicNumber}`} key={order.publicNumber}><strong>{order.publicNumber}</strong><span>{order.customerName}</span><small>{time(order.createdAt)}</small><b>{money(order.total)}</b><StatusBadge value={order.orderStatus} /></Link>)}</div> : <EmptyState text="Todavía no hay pedidos hoy." />}</article>
      <article className="admin-panel"><div className="panel-title"><div><p className="eyebrow">DEMANDA</p><h2>Más vendidos</h2></div></div>{data.topProducts.length ? <ol className="top-products">{data.topProducts.map((product, index) => <li key={product.name}><span>{index + 1}</span><strong>{product.name}</strong><b>{product.quantity} uds.</b></li>)}</ol> : <EmptyState text="Aún no hay ventas para comparar." />}</article></section></>
}

export function PageTitle({ eyebrow, title, action }: { eyebrow: string; title: string; action?: React.ReactNode }) { return <header className="page-title"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1></div>{action}</header> }
