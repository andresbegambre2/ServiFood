import { renderToString } from 'react-dom/server'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { AdminAuthContext, type AdminAuthState } from '../features/admin/admin-auth-context'
import type { AdminRole } from '../types/admin'
import { RequireKitchen } from './KitchenAccess'

const actions = { login: async () => ({ id: 1, name: 'Cocina', email: 'kitchen@servifood.local', role: 'KITCHEN' as const }), logout: async () => undefined, refresh: async () => undefined }
function renderAccess(role?: AdminRole) {
  const value: AdminAuthState = { loading: false, ...actions, user: role ? { id: 1, name: role, email: `${role.toLowerCase()}@servifood.local`, role } : undefined }
  return renderToString(<MemoryRouter initialEntries={['/kitchen']}><AdminAuthContext.Provider value={value}><Routes><Route path="/admin/login" element={<p>Ingreso interno</p>} /><Route element={<RequireKitchen />}><Route path="/kitchen" element={<p>Tablero autorizado</p>} /></Route></Routes></AdminAuthContext.Provider></MemoryRouter>)
}

describe('kitchen route access', () => {
  it('allows kitchen and administrator roles', () => {
    expect(renderAccess('KITCHEN')).toContain('Tablero autorizado')
    expect(renderAccess('ADMIN')).toContain('Tablero autorizado')
  })

  it('blocks cashiers and does not render kitchen content for anonymous users', () => {
    expect(renderAccess('CASHIER')).toContain('Esta pantalla es exclusiva de cocina')
    expect(renderAccess('CASHIER')).not.toContain('Tablero autorizado')
    expect(renderAccess()).not.toContain('Tablero autorizado')
  })
})
