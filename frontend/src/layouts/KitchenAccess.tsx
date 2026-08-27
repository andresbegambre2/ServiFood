import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAdminAuth } from '../features/admin/admin-auth-context'

export function RequireKitchen() {
  const { user, loading, logout } = useAdminAuth(); const location = useLocation()
  if (loading) return <main className="kitchen-gate"><div className="kitchen-spinner" /><p>Abriendo cocina…</p></main>
  if (!user) return <Navigate to="/admin/login" state={{ from: location.pathname }} replace />
  if (!['ADMIN', 'KITCHEN'].includes(user.role)) return <main className="kitchen-gate"><p className="kitchen-kicker">ACCESO RESTRINGIDO</p><h1>Esta pantalla es exclusiva de cocina</h1><p>Tu rol no tiene permisos para operar pedidos desde aquí.</p><button onClick={() => void logout()}>Cerrar sesión</button></main>
  return <Outlet />
}
