import { renderToString } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AdminAuthContext, type AdminAuthState } from './admin-auth-context'
import { AdminLayout, RequireAdmin } from '../../layouts/AdminLayout'
import { AdminLoginPage } from '../../pages/AdminLoginPage'
import { AdminDashboardPage } from '../../pages/AdminDashboardPage'
import { AdminOrdersPage } from '../../pages/AdminOrdersPage'
import { formatReportCell, label, quantityLabel } from '../../pages/adminFormat'
import { KitchenPage } from '../../pages/KitchenPage'

const actions = { login: async () => undefined, logout: async () => undefined, refresh: async () => undefined }
const state = (overrides: Partial<AdminAuthState> = {}): AdminAuthState => ({ loading: false, ...actions, ...overrides })
function renderRoute(value: AdminAuthState, path: string, page: React.ReactNode) {
  return renderToString(<MemoryRouter initialEntries={[path]}><AdminAuthContext.Provider value={value}><Routes><Route path="/admin/login" element={<AdminLoginPage />} /><Route element={<RequireAdmin />}><Route element={<AdminLayout />}><Route path="/admin" element={page} /><Route path="/admin/orders" element={page} /><Route path="/admin/kitchen" element={page} /></Route></Route></Routes></AdminAuthContext.Provider></MemoryRouter>)
}

describe('administrative routes', () => {
  it('renders the internal login form for an anonymous user', () => {
    const html = renderRoute(state(), '/admin/login', <AdminDashboardPage />)
    expect(html).toContain('Bienvenido de vuelta')
    expect(html).toContain('Acceso exclusivo para personal autorizado')
    expect(html).not.toContain('sesión expiró')
  })

  it('shows only the scoped kitchen workspace to the kitchen role', () => {
    const html = renderRoute(state({ user: { id: 3, name: 'Cocina', email: 'kitchen@servifood.local', role: 'KITCHEN' } }), '/admin/kitchen', <KitchenPage />)
    expect(html).toContain('Preparando la vista de cocina')
    expect(html).toContain('Cocina')
    expect(html).not.toContain('Configuración')
    expect(html).not.toContain('KITCHEN')
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
    expect(renderRoute(admin, '/admin', <AdminDashboardPage />)).toContain('Preparando el panel')
    const orders = renderRoute(admin, '/admin/orders', <AdminOrdersPage />)
    expect(orders).toContain('Buscar pedido o cliente')
    expect(orders).toContain('Cargando pedidos')
  })

  it('uses the backend payment method contract for pickup orders', () => {
    expect(label('PAY_ON_PICKUP')).toBe('Pago al recoger')
    expect(label('ADMIN')).toBe('Administración')
    expect(label('CASHIER')).toBe('Caja')
    expect(label('KITCHEN')).toBe('Cocina')
  })

  it('uses correct singular and plural labels in customer summaries', () => {
    expect(quantityLabel(1, 'pedido', 'pedidos')).toBe('1 pedido')
    expect(quantityLabel(2, 'pedido', 'pedidos')).toBe('2 pedidos')
    expect(quantityLabel(1, 'unidad', 'unidades')).toBe('1 unidad')
  })

  it('formats report dates and monetary values for the Spanish interface', () => {
    expect(formatReportCell(32450, 'Ventas', 'SALES')).toContain('32.450')
    expect(formatReportCell('2026-08-25T05:00:00.000Z', 'Fecha', 'SALES')).not.toContain('T05:00')
  })
})
