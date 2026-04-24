import { DigiLink } from '@designsystem-se/af-react';
import type { ComponentProps, ReactNode } from 'react';
import { definedProps } from './helpers';

export interface PortalLinkProps {
  href: string;
  external?: boolean;
  download?: string;
  ariaLabel?: string;
  children: ReactNode;
}

export function PortalLink({
  href,
  external,
  download,
  ariaLabel,
  children,
}: PortalLinkProps) {
  const digiProps = definedProps({
    afHref: href,
    afTarget: external ? '_blank' : undefined,
    afDownload: download,
    afAriaLabel: ariaLabel,
  }) as ComponentProps<typeof DigiLink>;

  return <DigiLink {...digiProps}>{children}</DigiLink>;
}
