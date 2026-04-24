import { DigiNotificationAlert } from '@designsystem-se/af-react';
import { NotificationAlertVariation, NotificationAlertSize } from '@designsystem-se/af';
import type { ComponentProps, ReactNode } from 'react';
import { definedProps } from './helpers';

export interface PortalAlertProps {
  variant?: 'info' | 'success' | 'warning' | 'danger';
  size?: 'small' | 'medium' | 'large';
  heading?: string;
  closeable?: boolean;
  onClose?: () => void;
  children: ReactNode;
}

const variationMap: Record<string, NotificationAlertVariation> = {
  info: NotificationAlertVariation.INFO,
  success: NotificationAlertVariation.SUCCESS,
  warning: NotificationAlertVariation.WARNING,
  danger: NotificationAlertVariation.DANGER,
};

const sizeMap: Record<string, NotificationAlertSize> = {
  small: NotificationAlertSize.SMALL,
  medium: NotificationAlertSize.MEDIUM,
  large: NotificationAlertSize.LARGE,
};

export function PortalAlert({
  variant = 'info',
  size = 'large',
  heading,
  closeable = false,
  onClose,
  children,
}: PortalAlertProps) {
  const digiProps = definedProps({
    afVariation: variationMap[variant],
    afSize: sizeMap[size],
    afHeading: heading,
    afCloseable: closeable,
    onAfOnClose: () => onClose?.(),
  }) as ComponentProps<typeof DigiNotificationAlert>;

  return (
    <DigiNotificationAlert {...digiProps}>
      {children}
    </DigiNotificationAlert>
  );
}
