interface ConfirmDialogProps {
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  onConfirm: () => void
  onCancel: () => void
  dangerous?: boolean
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Bekräfta',
  cancelLabel = 'Avbryt',
  onConfirm,
  onCancel,
  dangerous = false,
}: ConfirmDialogProps) {
  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={onCancel}
    >
      <div
        style={{
          background: 'white',
          borderRadius: '4px',
          padding: '2rem',
          maxWidth: '420px',
          width: '90%',
          boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 style={{ marginTop: 0, fontSize: '1.25rem' }}>{title}</h2>
        <p style={{ color: '#444', marginBottom: '1.75rem' }}>{message}</p>
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
          <button
            onClick={onCancel}
            style={{
              padding: '0.5rem 1.25rem',
              border: '1px solid #ccc',
              borderRadius: '2px',
              background: 'white',
              cursor: 'pointer',
            }}
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            style={{
              padding: '0.5rem 1.25rem',
              border: 'none',
              borderRadius: '2px',
              background: dangerous ? '#c5221f' : 'var(--digi-color-primary, #006991)',
              color: 'white',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
