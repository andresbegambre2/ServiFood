import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { getStorefront } from '../../api/publicCatalog'
import type { StorefrontData } from '../../types/public'
import { StorefrontContext } from './storefront-context'
export function StorefrontProvider({ children }: { children: ReactNode }) {
  const [data, setData] = useState<StorefrontData | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState<string | null>(null); const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => { setLoading(true); setError(null); setAttempt((value) => value + 1) }, [])
  useEffect(() => { const controller = new AbortController(); getStorefront(controller.signal).then(setData).catch((reason: unknown) => { if (!(reason instanceof DOMException && reason.name === 'AbortError')) setError(reason instanceof Error ? reason.message : 'No pudimos cargar el restaurante.') }).finally(() => setLoading(false)); return () => controller.abort() }, [attempt])
  return <StorefrontContext.Provider value={{ data, loading, error, retry }}>{children}</StorefrontContext.Provider>
}
