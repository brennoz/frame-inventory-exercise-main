type ProblemDetail = {
  type?: string
  title?: string
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly title?: string
  readonly type?: string
  readonly fieldErrors: Record<string, string>

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.title = problem?.title
    this.type = problem?.type
    this.fieldErrors = problem?.errors ?? {}
  }
}

export async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  return requestJson<T>(url, {
    headers: { Accept: 'application/json' },
    signal,
  })
}

export async function sendJson<T>(url: string, method: 'POST' | 'PUT', body: unknown): Promise<T> {
  return requestJson<T>(url, {
    method,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

async function requestJson<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, init)

  if (!response.ok) {
    let problem: ProblemDetail | undefined
    try {
      problem = (await response.json()) as ProblemDetail
    } catch {
      // Some proxy and infrastructure errors do not return JSON.
    }
    throw new ApiError(problem?.detail ?? problem?.title ?? `Request failed (${response.status})`, response.status, problem)
  }

  return (await response.json()) as T
}
