import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminAuth } from '../features/admin/admin-auth-context'
import { useKitchenBoard } from '../features/kitchen/useKitchenBoard'
import { deliveryLabels, elapsedLabel, groupKitchenOrders, isDelayed, kitchenLabels, kitchenStages, nextKitchenStage } from '../features/kitchen/kitchen'
import type { KitchenOrder, KitchenStage } from '../types/kitchen'

export function KitchenPage() {
  const { user, logout } = useAdminAuth(); const board = useKitchenBoard()
  async function fullscreen() { try { if (document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen() } catch { /* fullscreen is optional */ } }
  return <main className="kitchen-screen">
    <header className="kitchen-header"><div className="kitchen-brand"><span>SF</span><div><strong>ServiFood</strong><small>COCINA</small></div></div><div className="kitchen-header__status"><i /> En línea <small>{board.lastUpdated ? `Actualizado ${board.lastUpdated.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}` : 'Conectando…'}</small></div><div className="kitchen-actions">{user?.role === 'ADMIN' && <Link to="/admin">Ir al panel</Link>}<button onClick={() => void board.refresh()}>↻ Actualizar</button><button onClick={() => void fullscreen()}>⛶ Pantalla completa</button><button onClick={() => void logout()}>Salir</button></div></header>
    {board.error && <div className="kitchen-message error" role="alert"><strong>Atención</strong>{board.error}<button onClick={() => void board.refresh()}>Reintentar</button></div>}
    {board.feedback && <div className="kitchen-message success" role="status">✓ {board.feedback}</div>}
    {board.loading ? <section className="kitchen-loading"><div className="kitchen-spinner" /><h1>Preparando el tablero…</h1></section> : <KitchenBoard orders={board.orders} now={board.clock} pending={board.pending} onTransition={board.transition} />}
  </main>
}

export function KitchenBoard({ orders, now, pending, onTransition }: { orders: KitchenOrder[]; now: number; pending?: string; onTransition(order: KitchenOrder, target: KitchenStage): Promise<void> }) {
  const [mobileStage, setMobileStage] = useState<KitchenStage>('NEW'); const grouped = useMemo(() => groupKitchenOrders(orders), [orders])
  return <><nav className="kitchen-mobile-tabs" aria-label="Estados de cocina">{kitchenStages.map(stage => <button className={mobileStage === stage ? 'active' : ''} onClick={() => setMobileStage(stage)} key={stage}>{kitchenLabels[stage]} <b>{grouped[stage].length}</b></button>)}</nav><section className="kitchen-board" data-mobile-active={mobileStage}>{kitchenStages.map(stage => <KitchenColumn key={stage} stage={stage} orders={grouped[stage]} now={now} pending={pending} onTransition={onTransition} />)}</section></>
}

function KitchenColumn({ stage, orders, now, pending, onTransition }: { stage: KitchenStage; orders: KitchenOrder[]; now: number; pending?: string; onTransition(order: KitchenOrder, target: KitchenStage): Promise<void> }) {
  return <section className="kitchen-column" data-stage={stage} aria-labelledby={`kitchen-${stage}`}><header><div><h2 id={`kitchen-${stage}`}>{kitchenLabels[stage]}</h2><p>{stage === 'NEW' ? 'Confirmados para preparar' : stage === 'PREPARING' ? 'Actualmente en cocina' : 'Esperando despacho'}</p></div><strong>{orders.length}</strong></header><div className="kitchen-column__orders">{orders.length ? orders.map(order => <KitchenCard order={order} now={now} pending={pending === order.publicNumber} onTransition={onTransition} key={order.publicNumber} />) : <div className="kitchen-empty"><span>✓</span><strong>Todo al día</strong><p>No hay pedidos en esta etapa.</p></div>}</div></section>
}

function KitchenCard({ order, now, pending, onTransition }: { order: KitchenOrder; now: number; pending: boolean; onTransition(order: KitchenOrder, target: KitchenStage): Promise<void> }) {
  const delayed = isDelayed(order.createdAt, now); const next = nextKitchenStage[order.stage]
  return <article className={`kitchen-ticket ${delayed ? 'delayed' : ''}`}><header><div><p>PEDIDO</p><h3>{order.publicNumber}</h3></div><div className="ticket-time"><strong>{elapsedLabel(order.createdAt, now)}</strong><small>{new Date(order.createdAt).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}</small></div></header>{delayed && <div className="delay-badge">⚠ DEMORADO · Prioridad alta</div>}<div className="delivery-badge">{order.deliveryType === 'DELIVERY' ? '↗' : '⌂'} {deliveryLabels[order.deliveryType]}</div><div className="ticket-items">{order.items.map((item, index) => <section key={`${item.name}-${index}`}><div className="ticket-item-title"><b>{item.quantity}×</b><strong>{item.name}</strong></div>{item.extras.length > 0 && <ul>{item.extras.map(extra => <li key={extra.name}>+ {extra.quantity > 1 ? `${extra.quantity} × ` : ''}{extra.name}</li>)}</ul>}{item.notes && <p className="ticket-note"><span>OBSERVACIÓN</span>{item.notes}</p>}</section>)}</div>{order.notes && <div className="ticket-general-note"><strong>NOTA GENERAL</strong>{order.notes}</div>}{next && <button className="kitchen-transition" disabled={pending} onClick={() => void onTransition(order, next)}>{pending ? 'Actualizando…' : next === 'PREPARING' ? 'Empezar preparación →' : 'Marcar como listo ✓'}</button>}</article>
}
