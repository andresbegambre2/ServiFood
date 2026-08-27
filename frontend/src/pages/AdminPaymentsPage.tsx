import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminRequest } from '../api/admin'
import type { PaymentQueueItem, PaymentStatus } from '../types/admin'
import { money } from '../utils/money'
import { PageTitle } from './AdminDashboardPage'
import { AdminError, AdminLoading, EmptyState, StatusBadge } from './adminUi'
import { dateTime, label } from './adminFormat'

export function AdminPaymentsPage() {
  const [status, setStatus] = useState<PaymentStatus | ''>('UNDER_REVIEW'); const [items, setItems] = useState<PaymentQueueItem[]>(); const [error, setError] = useState('')
  const load = () => { setError(''); adminRequest<PaymentQueueItem[]>(`/payments${status ? `?status=${status}` : ''}`).then(setItems).catch(() => setError('No pudimos consultar la cola de pagos.')) }
  useEffect(() => { adminRequest<PaymentQueueItem[]>(`/payments${status ? `?status=${status}` : ''}`).then(setItems).catch(() => setError('No pudimos consultar la cola de pagos.')) }, [status])
  return <><PageTitle eyebrow="CONTROL FINANCIERO" title="Pagos" action={<select aria-label="Filtrar pagos" value={status} onChange={e => setStatus(e.target.value as PaymentStatus | '')}><option value="">Todos</option><option value="UNDER_REVIEW">Por revisar</option><option value="APPROVED">Aprobados</option><option value="REJECTED">Rechazados</option><option value="PENDING">Pendientes</option></select>} />
    {error ? <AdminError message={error} retry={load} /> : !items ? <AdminLoading label="Consultando pagos…" /> : !items.length ? <EmptyState text="No hay pagos en este estado." /> : <div className="payment-grid">{items.map(payment => <article className="admin-panel payment-card" key={payment.publicNumber}><div><p className="eyebrow">{payment.publicNumber}</p><h2>{payment.customerName}</h2><small>{dateTime(payment.createdAt)}</small></div><strong className="payment-amount">{money(payment.amount)}</strong><dl className="info-list"><div><dt>Método</dt><dd>{label(payment.method)}</dd></div><div><dt>Estado</dt><dd><StatusBadge value={payment.status} /></dd></div><div><dt>Comprobante</dt><dd>{payment.receiptAvailable ? 'Disponible' : 'No aplica'}</dd></div></dl><Link className="primary button-link" to={`/admin/orders/${payment.publicNumber}`}>Revisar pedido</Link></article>)}</div>}
  </>
}
