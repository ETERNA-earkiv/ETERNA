import { DigiLoaderSpinner } from '@designsystem-se/af-react';
import { LoaderSpinnerSize } from '@designsystem-se/af';
import type { ComponentProps } from 'react';
import { definedProps } from './helpers';

export interface PortalSpinnerProps {
  text?: string;
  size?: 'small' | 'medium' | 'large';
}

const sizeMap: Record<string, LoaderSpinnerSize> = {
  small: LoaderSpinnerSize.SMALL,
  medium: LoaderSpinnerSize.MEDIUM,
  large: LoaderSpinnerSize.LARGE,
};

export function PortalSpinner({
  text = 'Laddar...',
  size = 'medium',
}: PortalSpinnerProps) {
  const digiProps = definedProps({
    afText: text,
    afSize: sizeMap[size],
  }) as ComponentProps<typeof DigiLoaderSpinner>;

  return <DigiLoaderSpinner {...digiProps} />;
}
