import { ApiError, type ApiProblem } from './client'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'
let csrf: { token: string } | undefined

async function problem(response: Response): Promise<never> {
  let body: ApiProblem = {}
  try { body = await response.json() as ApiProblem } catch { /* response without JSON */ }
  throw new ApiError(response.status, body)
}

async function csrfHeaders() {
  if (!csrf) {
    const response = await fetch(`${API_URL}/admin/auth/csrf`, { credentials: 'include', headers: { Accept: 'application/json' } })
    if (!response.ok) return problem(response)
    csrf = await response.json() as { token: string }
  }
  return { 'X-XSRF-TOKEN': csrf.token }
}

export async function adminRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method?.toUpperCase() ?? 'GET'
  const changing = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const response = await fetch(`${API_URL}/admin${path}`, {
    ...init,
    credentials: 'include',
    headers: { Accept: 'application/json', ...(changing ? await csrfHeaders() : {}), ...init.headers },
  })
  if (!response.ok) {
    if (response.status === 403 && changing) csrf = undefined
    if (response.status === 401 && typeof window !== 'undefined') window.dispatchEvent(new Event('admin-session-expired'))
    return problem(response)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function adminJson<T>(path: string, method: 'POST' | 'PUT' | 'PATCH', body: unknown) {
  return adminRequest<T>(path, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
}

export async function login(email: string, password: string) {
  const body = new URLSearchParams({ username: email, password })
  await adminRequest<void>('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body })
  csrf = undefined
}

export async function logout() { await adminRequest<void>('/auth/logout', { method: 'POST' }); csrf = undefined }

export async function uploadAdminFile<T>(path: string, field: string, file: File) {
  const form = new FormData(); form.append(field, file)
  return adminRequest<T>(path, { method: 'POST', body: form })
}

export async function receiptUrl(publicNumber: string) {
  const response = await fetch(`${API_URL}/admin/orders/${encodeURIComponent(publicNumber)}/payment/receipt`, { credentials: 'include' })
  if (!response.ok) return problem(response)
  return URL.createObjectURL(await response.blob())
}
