const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { signal, headers: { Accept: 'application/json' } })
  if (!response.ok) throw new Error(response.status === 404 ? 'No encontramos lo que buscas.' : 'No pudimos cargar la información.')
  return response.json() as Promise<T>
}
