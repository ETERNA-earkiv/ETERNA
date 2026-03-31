const STATE_STYLES: Record<string, { background: string; color: string }> = {
  ACTIVE: { background: '#e6f4ea', color: '#2d7a3a' },
  STORED: { background: '#e6f4ea', color: '#2d7a3a' },
  CREATED: { background: '#e8f0fe', color: '#1a56db' },
  RUNNING: { background: '#fff3e0', color: '#b45309' },
  COMPLETED: { background: '#e6f4ea', color: '#2d7a3a' },
  FAILED: { background: '#fce8e6', color: '#c5221f' },
  STOPPED: { background: '#f3e8ff', color: '#7c3aed' },
  FAILURE: { background: '#fce8e6', color: '#c5221f' },
}

const DEFAULT_STYLE = { background: '#f1f3f4', color: '#444' }

interface StatusBadgeProps {
  state: string
}

export function StatusBadge({ state }: StatusBadgeProps) {
  const style = STATE_STYLES[state] ?? DEFAULT_STYLE

  return (
    <span
      style={{
        padding: '0.2rem 0.6rem',
        borderRadius: '2px',
        fontSize: '0.8rem',
        ...style,
      }}
    >
      {state}
    </span>
  )
}
