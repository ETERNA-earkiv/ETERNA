import type { ReactNode } from 'react';

export interface PortalEmptyStateProps {
  title: string;
  message?: string;
  icon?: ReactNode;
  action?: ReactNode;
}

export function PortalEmptyState({
  title,
  message,
  icon,
  action,
}: PortalEmptyStateProps) {
  return (
    <div className="portal-empty-state" role="status">
      {icon && <div className="portal-empty-state__icon">{icon}</div>}
      <h2 className="portal-empty-state__title">{title}</h2>
      {message && <p className="portal-empty-state__message">{message}</p>}
      {action && <div className="portal-empty-state__action">{action}</div>}
    </div>
  );
}
