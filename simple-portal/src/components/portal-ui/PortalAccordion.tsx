import { DigiExpandableAccordion } from '@designsystem-se/af-react';
import type { ComponentProps, ReactNode } from 'react';
import { definedProps } from './helpers';

export interface PortalAccordionProps {
  heading: string;
  expanded?: boolean;
  onToggle?: () => void;
  children: ReactNode;
}

export function PortalAccordion({
  heading,
  expanded = false,
  onToggle,
  children,
}: PortalAccordionProps) {
  const digiProps = definedProps({
    afHeading: heading,
    afExpanded: expanded,
    onAfOnClick: () => onToggle?.(),
  }) as ComponentProps<typeof DigiExpandableAccordion>;

  return (
    <DigiExpandableAccordion {...digiProps}>
      {children}
    </DigiExpandableAccordion>
  );
}
