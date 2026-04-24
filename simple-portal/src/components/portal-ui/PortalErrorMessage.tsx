import { PortalAlert } from './PortalAlert';
import { PortalButton } from './PortalButton';

export interface PortalErrorMessageProps {
  error: string;
  onRetry?: () => void;
}

export function PortalErrorMessage({ error, onRetry }: PortalErrorMessageProps) {
  return (
    <PortalAlert variant="danger" heading="Något gick fel">
      <p>{error}</p>
      {onRetry && (
        <PortalButton variant="secondary" size="small" onClick={onRetry}>
          Försök igen
        </PortalButton>
      )}
    </PortalAlert>
  );
}
