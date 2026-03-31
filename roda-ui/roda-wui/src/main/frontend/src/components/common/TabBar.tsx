interface Tab {
  id: string
  label: string
}

interface TabBarProps {
  tabs: Tab[]
  activeTab: string
  onTabChange: (id: string) => void
}

export function TabBar({ tabs, activeTab, onTabChange }: TabBarProps) {
  return (
    <div
      style={{
        display: 'flex',
        borderBottom: '2px solid #e0e0e0',
        marginBottom: '1.5rem',
        gap: '0',
      }}
      role="tablist"
    >
      {tabs.map((tab) => {
        const isActive = tab.id === activeTab
        return (
          <button
            key={tab.id}
            role="tab"
            aria-selected={isActive}
            onClick={() => onTabChange(tab.id)}
            style={{
              padding: '0.65rem 1.25rem',
              border: 'none',
              background: 'transparent',
              cursor: 'pointer',
              fontWeight: isActive ? 600 : 400,
              fontSize: '0.9375rem',
              color: isActive ? 'var(--digi-color-primary, #006991)' : '#555',
              borderBottom: isActive ? '2px solid var(--digi-color-primary, #006991)' : '2px solid transparent',
              marginBottom: '-2px',
            }}
          >
            {tab.label}
          </button>
        )
      })}
    </div>
  )
}
