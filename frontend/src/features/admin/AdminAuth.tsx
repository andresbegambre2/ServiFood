import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { adminRequest, login as loginRequest, logout as logoutRequest } from '../../api/admin'
import { ApiError } from '../../api/client'
import type { AdminUser } from '../../types/admin'
import { AdminAuthContext, type AdminAuthState } from './admin-auth-context'

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AdminUser>()
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState<string>()
  async function refresh() {
    setLoading(true)
    try { setUser(await adminRequest<AdminUser>('/auth/session')); setMessage(undefined) }
    catch (error) { setUser(undefined); setMessage(error instanceof ApiError && error.status !== 401 ? error.message : undefined) }
    finally { setLoading(false) }
  }
  useEffect(() => {
    adminRequest<AdminUser>('/auth/session').then(value => { setUser(value); setMessage(undefined) }).catch(error => { setUser(undefined); setMessage(error instanceof ApiError && error.status !== 401 ? error.message : undefined) }).finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    const expired = () => { setUser(undefined); setMessage('Tu sesión expiró. Inicia sesión nuevamente.') }
    window.addEventListener('admin-session-expired', expired)
    return () => window.removeEventListener('admin-session-expired', expired)
  }, [])
  const value = useMemo<AdminAuthState>(() => ({ user, loading, message,
    async login(email, password) { await loginRequest(email, password); await refresh() },
    async logout() { await logoutRequest(); setUser(undefined) }, refresh,
  }), [user, loading, message])
  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>
}
