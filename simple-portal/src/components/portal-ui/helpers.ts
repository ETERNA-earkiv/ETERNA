/**
 * Strip undefined values from props before passing to Digi components.
 * Digi's Stencil-generated React wrappers have strict types that reject undefined.
 */
export function definedProps<T extends Record<string, unknown>>(obj: T): T {
  return Object.fromEntries(
    Object.entries(obj).filter(([, v]) => v !== undefined),
  ) as T;
}
