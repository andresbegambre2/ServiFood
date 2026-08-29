import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAdminAuth } from '../features/admin/admin-auth-context'
import { label } from '../pages/adminFormat'

const operational = [
  ['⌂', 'Panel general', '/admin'], ['▤', 'Pedidos', '/admin/orders'], ['₱', 'Pagos', '/admin/payments'], ['▦', 'Productos', '/admin/products'], ['◩', 'Inventario', '/admin/inventory'], ['♙', 'Clientes', '/admin/customers'],
] as const
const management = [['◫', 'Categorías', '/admin/categories'], ['%', 'Promociones', '/admin/promotions'], ['◇', 'Puntos y cupones', '/admin/coupons'], ['↗', 'Analítica', '/admin/analytics'], ['▥', 'Reportes', '/admin/reports'], ['⚙', 'Configuración', '/admin/settings']] as const
const kitchen = [['☷', 'Cocina', '/admin/kitchen']] as const
const managementPaths = management.map(([, , path]) => path)

export function RequireAdmin() {
  const { user, loading } = useAdminAuth()
  const location = useLocation()
  if (loading) return <main className="admin-centered"><div className="admin-loader" /><p>Verificando sesión…</p></main>
  if (!user) return <Navigate to="/admin/login" state={{ from: location.pathname }} replace />
  return <Outlet />
}

export function AdminLayout() {
  const { user, logout } = useAdminAuth()
  const location = useLocation()
  if (user?.role === 'KITCHEN' && location.pathname !== '/admin/kitchen') return <Navigate to="/admin/kitchen" replace />
  if (user?.role === 'CASHIER' && (location.pathname.startsWith('/admin/kitchen') || managementPaths.some(path => location.pathname.startsWith(path)))) return <Navigate to="/admin" replace />
  const links = user?.role === 'KITCHEN' ? kitchen : user?.role === 'ADMIN' ? [...operational, ...kitchen, ...management] : operational
  const home = user?.role === 'KITCHEN' ? '/admin/kitchen' : '/admin'
  return <div className="admin-app">
    <aside className="admin-sidebar">
      <NavLink to={home} className="admin-brand"><span>SF</span><strong>ServiFood</strong><small>OPERACIONES</small></NavLink>
      <nav aria-label="Administración">{links.map(([icon, label, to]) => <NavLink key={to} to={to} end={to === '/admin'} aria-label={label} title={label}><b aria-hidden="true">{icon}</b><span>{label}</span></NavLink>)}</nav>
      <div className="admin-profile"><span className="avatar">{user?.name.slice(0, 2).toUpperCase()}</span><div><strong>{user?.name}</strong><small>{label(user?.role)}</small></div><button aria-label="Cerrar sesión" title="Cerrar sesión" onClick={() => void logout()}>↪</button></div>
    </aside>
    <div className="admin-workspace">
      <header className="admin-topbar"><div><span className="live-dot" /> Operación en línea</div><NavLink to={user?.role === 'KITCHEN' ? '/admin/kitchen' : '/admin/orders?status=NEW'} className="new-order-link">{user?.role === 'KITCHEN' ? 'Ver cocina' : 'Ver pedidos nuevos'}</NavLink></header>
      <main className="admin-content"><Outlet /></main>
    </div>
  </div>
}
