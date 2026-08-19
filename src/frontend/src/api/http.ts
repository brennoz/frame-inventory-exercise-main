type ProblemDetail = {
  title?: string
  detail?: string
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    let problem: ProblemDetail | undefined
    try {
      problem = (await response.json()) as ProblemDetail
    } catch {
      // Some proxy and infrastructure errors do not return JSON.
    }
    throw new ApiError(problem?.detail ?? problem?.title ?? `Request failed (${response.status})`, response.status)
  }

  return (await response.json()) as T
}
