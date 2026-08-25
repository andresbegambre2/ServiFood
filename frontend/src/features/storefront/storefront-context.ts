import { createContext, useContext } from 'react'
import type { StorefrontData } from '../../types/public'
export interface StorefrontContextValue { data: StorefrontData | null; loading: boolean; error: string | null; retry: () => void }
export const StorefrontContext = createContext<StorefrontContextValue | null>(null)
export function useStorefront() { const value = useContext(StorefrontContext); if (!value) throw new Error('useStorefront must be used within StorefrontProvider'); return value }
