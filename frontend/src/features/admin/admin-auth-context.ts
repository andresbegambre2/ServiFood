import { createContext, useContext } from 'react'
import type { AdminUser } from '../../types/admin'

export interface AdminAuthState { user?: AdminUser; loading: boolean; message?: string; login(email: string, password: string): Promise<AdminUser>; logout(): Promise<void>; refresh(): Promise<void> }
export const AdminAuthContext = createContext<AdminAuthState | undefined>(undefined)
export function useAdminAuth() { const value = useContext(AdminAuthContext); if (!value) throw new Error('AdminAuthProvider missing'); return value }
