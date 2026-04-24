import { DigiFormCheckbox } from '@designsystem-se/af-react';
import type { ComponentProps } from 'react';
import { definedProps } from './helpers';

export interface PortalCheckboxProps {
  label: string;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  indeterminate?: boolean;
  name?: string;
  value?: string;
}

export function PortalCheckbox({
  label,
  checked,
  onChange,
  indeterminate,
  name,
  value,
}: PortalCheckboxProps) {
  const digiProps = definedProps({
    afLabel: label,
    afChecked: checked,
    afIndeterminate: indeterminate,
    afName: name,
    afValue: value,
    onAfOnChange: (e: CustomEvent) => {
      const nativeEvent = e.detail;
      const checked = nativeEvent?.target?.checked ?? nativeEvent?.checked ?? false;
      onChange?.(checked);
    },
  }) as ComponentProps<typeof DigiFormCheckbox>;

  return (
    <DigiFormCheckbox {...digiProps} />
  );
}
