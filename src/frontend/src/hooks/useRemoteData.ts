import { useCallback, useEffect, useState } from 'react'

type RemoteData<T> =
  | { status: 'loading'; data?: undefined; error?: undefined }
  | { status: 'success'; data: T; error?: undefined }
  | { status: 'error'; data?: undefined; error: string }

export function useRemoteData<T>(loader: (signal: AbortSignal) => Promise<T>, dependencies: readonly unknown[]) {
  const [state, setState] = useState<RemoteData<T>>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => setAttempt((current) => current + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    // A new request intentionally replaces stale data with an explicit loading state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setState({ status: 'loading' })
    loader(controller.signal)
      .then((data) => setState({ status: 'success', data }))
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return
        setState({ status: 'error', error: error instanceof Error ? error.message : 'An unexpected error occurred' })
      })
    return () => controller.abort()
    // The caller owns dependency stability, matching useEffect semantics.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencies, attempt])

  return { ...state, retry }
}
