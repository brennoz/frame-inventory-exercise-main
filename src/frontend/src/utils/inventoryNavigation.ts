export type InventoryNavigationState = {
  returnTo: string
}

export function inventoryReturnPath(state: unknown): string {
  if (!state || typeof state !== 'object' || !('returnTo' in state)) return '/frames'
  const returnTo = (state as { returnTo?: unknown }).returnTo
  return typeof returnTo === 'string' && (returnTo === '/frames' || returnTo.startsWith('/frames?'))
    ? returnTo
    : '/frames'
}
