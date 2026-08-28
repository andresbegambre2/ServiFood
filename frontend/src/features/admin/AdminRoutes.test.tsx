import { renderToString } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AdminAuthContext, type AdminAuthState } from './admin-auth-context'
import { AdminLayout, RequireAdmin } from '../../layouts/AdminLayout'
import { AdminLoginPage } from '../../pages/AdminLoginPage'
import { AdminDashboardPage } from '../../pages/AdminDashboardPage'
import { AdminOrdersPage } from '../../pages/AdminOrdersPage'
import { label } from '../../pages/adminFormat'

const actions = { login: async () => undefined, logout: async () => undefined, refresh: async () => undefined }
const state = (overrides: Partial<AdminAuthState> = {}): AdminAuthState => ({ loading: false, ...actions, ...overrides })
function renderRoute(value: AdminAuthState, path: string, page: React.ReactNode) {
  return renderToString(<MemoryRouter initialEntries={[path]}><AdminAuthContext.Provider value={value}><Routes><Route path="/admin/login" element={<AdminLoginPage />} /><Route element={<RequireAdmin />}><Route element={<AdminLayout />}><Route path="/admin" element={page} /><Route path="/admin/orders" element={page} /></Route></Route></Routes></AdminAuthContext.Provider></MemoryRouter>)
}

describe('administrative routes', () => {
  it('renders the internal login form for an anonymous user', () => {
    const html = renderRoute(state(), '/admin/login', <AdminDashboardPage />)
    expect(html).toContain('Bienvenido de vuelta')
    expect(html).toContain('Acceso exclusivo para personal autorizado')
    expect(html).not.toContain('sesión expiró')
  })

  it('blocks the kitchen role from the general administration panel', () => {
    const html = renderRoute(state({ user: { id: 3, name: 'Cocina', email: 'kitchen@servifood.local', role: 'KITCHEN' } }), '/admin', <AdminDashboardPage />)
    expect(html).toContain('Este panel aún no está disponible para Cocina')
    expect(html).not.toContain('Configuración')
  })

  it('shows operational navigation but hides critical settings from cashier', () => {
    const html = renderRoute(state({ user: { id: 2, name: 'Caja', email: 'cashier@servifood.local', role: 'CASHIER' } }), '/admin', <AdminDashboardPage />)
    expect(html).toContain('Pedidos')
    expect(html).toContain('Pagos')
    expect(html).toContain('Productos')
    expect(html).toContain('Inventario')
    expect(html).toContain('Clientes')
    expect(html).not.toContain('Puntos y cupones')
    expect(html).not.toContain('Analítica')
    expect(html).not.toContain('Reportes')
    expect(html).not.toContain('Categorías')
    expect(html).not.toContain('Configuración')
  })

  it('shows loyalty management only to administrators', () => {
    const html = renderRoute(state({ user: { id: 1, name: 'Admin', email: 'admin@servifood.local', role: 'ADMIN' } }), '/admin', <AdminDashboardPage />)
    expect(html).toContain('Clientes')
    expect(html).toContain('Puntos y cupones')
    expect(html).toContain('Analítica')
    expect(html).toContain('Reportes')
  })

  it('renders dashboard and order filters with recoverable loading states', () => {
    const admin = state({ user: { id: 1, name: 'Admin', email: 'admin@servifood.local', role: 'ADMIN' } })
    expect(renderRoute(admin, '/admin', <AdminDashboardPage />)).toContain('Preparando el dashboard')
    const orders = renderRoute(admin, '/admin/orders', <AdminOrdersPage />)
    expect(orders).toContain('Buscar pedido o cliente')
    expect(orders).toContain('Cargando pedidos')
  })

  it('uses the backend payment method contract for pickup orders', () => {
    expect(label('PAY_ON_PICKUP')).toBe('Pago al recoger')
  })
})
