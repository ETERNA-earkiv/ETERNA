import { DigiNavigationPagination } from '@designsystem-se/af-react';
import type { ComponentProps } from 'react';
import { definedProps } from './helpers';

export interface PortalPaginationProps {
  currentPage: number;
  totalPages: number;
  totalResults?: number;
  resultName?: string;
  onPageChange: (page: number) => void;
}

export function PortalPagination({
  currentPage,
  totalPages,
  totalResults,
  resultName,
  onPageChange,
}: PortalPaginationProps) {
  const digiProps = definedProps({
    afInitActivePage: currentPage,
    afTotalPages: totalPages,
    afTotalResults: totalResults,
    afResultName: resultName,
    onAfOnPageChange: (e: CustomEvent) => onPageChange(e.detail),
  }) as ComponentProps<typeof DigiNavigationPagination>;

  return <DigiNavigationPagination {...digiProps} />;
}
