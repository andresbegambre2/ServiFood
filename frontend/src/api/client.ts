const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1'

export interface ApiProblem { title?: string; detail?: string; code?: string; currentQuote?: unknown }
export class ApiError extends Error {
  readonly status: number
  readonly problem: ApiProblem
  constructor(status: number, problem: ApiProblem) { super(problem.detail ?? 'No pudimos completar la solicitud.'); this.status = status; this.problem = problem }
}

export async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, { signal, headers: { Accept: 'application/json' } })
  if (!response.ok) throw new Error(response.status === 404 ? 'No encontramos lo que buscas.' : 'No pudimos cargar la información.')
  return response.json() as Promise<T>
}

async function parse<T>(response: Response): Promise<T> {
  if (response.ok) return response.json() as Promise<T>
  let problem: ApiProblem = {}
  try { problem = await response.json() as ApiProblem } catch { problem = {} }
  throw new ApiError(response.status, problem)
}

export async function postJson<TRequest, TResponse>(path: string, body: TRequest, signal?: AbortSignal): Promise<TResponse> {
  const response = await fetch(`${API_URL}${path}`, { method: 'POST', signal, headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  return parse<TResponse>(response)
}

export async function postForm<TResponse>(path: string, form: FormData): Promise<TResponse> {
  const response = await fetch(`${API_URL}${path}`, { method: 'POST', headers: { Accept: 'application/json' }, body: form })
  return parse<TResponse>(response)
}
