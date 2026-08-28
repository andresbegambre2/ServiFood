import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAdminAuth } from '../features/admin/admin-auth-context'

const operational = [
  ['⌂', 'Dashboard', '/admin'], ['▤', 'Pedidos', '/admin/orders'], ['₱', 'Pagos', '/admin/payments'], ['▦', 'Productos', '/admin/products'], ['◩', 'Inventario', '/admin/inventory'], ['♙', 'Clientes', '/admin/customers'],
] as const
const management = [['◫', 'Categorías', '/admin/categories'], ['%', 'Promociones', '/admin/promotions'], ['◇', 'Puntos y cupones', '/admin/coupons'], ['⚙', 'Configuración', '/admin/settings']] as const

export function RequireAdmin() {
  const { user, loading } = useAdminAuth()
  const location = useLocation()
  if (loading) return <main className="admin-centered"><div className="admin-loader" /><p>Verificando sesión…</p></main>
  if (!user) return <Navigate to="/admin/login" state={{ from: location.pathname }} replace />
  if (user.role === 'KITCHEN') return <main className="admin-centered"><p className="eyebrow">Acceso restringido</p><h1>Este panel aún no está disponible para Cocina</h1><p>La pantalla operativa de cocina se desarrollará en una fase posterior.</p></main>
  return <Outlet />
}

export function AdminLayout() {
  const { user, logout } = useAdminAuth()
  const links = user?.role === 'ADMIN' ? [...operational, ...management] : operational
  return <div className="admin-app">
    <aside className="admin-sidebar">
      <NavLink to="/admin" className="admin-brand"><span>SF</span><strong>ServiFood</strong><small>OPERACIONES</small></NavLink>
      <nav aria-label="Administración">{links.map(([icon, label, to]) => <NavLink key={to} to={to} end={to === '/admin'}><b aria-hidden="true">{icon}</b><span>{label}</span></NavLink>)}</nav>
      <div className="admin-profile"><span className="avatar">{user?.name.slice(0, 2).toUpperCase()}</span><div><strong>{user?.name}</strong><small>{user?.role}</small></div><button aria-label="Cerrar sesión" title="Cerrar sesión" onClick={() => void logout()}>↪</button></div>
    </aside>
    <div className="admin-workspace">
      <header className="admin-topbar"><div><span className="live-dot" /> Operación en línea</div><NavLink to="/admin/orders?status=NEW" className="new-order-link">Ver pedidos nuevos</NavLink></header>
      <main className="admin-content"><Outlet /></main>
    </div>
  </div>
}
