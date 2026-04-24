import { getLevelLabel } from '@lib/utils/i18n';
import { PortalCheckbox } from '../portal-ui/PortalCheckbox';
import './LevelSelector.css';

const ALL_LEVEL_IDS = [
  'fonds', 'class', 'collection', 'recordgrp', 'subgrp',
  'subfonds', 'series', 'subseries', 'file', 'item',
];

interface Props {
  selected: string[];
  onChange: (levels: string[]) => void;
}

export function LevelSelector({ selected, onChange }: Props) {
  function toggle(id: string, checked: boolean) {
    if (checked) {
      onChange([...selected, id]);
    } else {
      onChange(selected.filter((l) => l !== id));
    }
  }

  return (
    <div className="level-selector">
      <h3>Tillåtna beskrivningsnivåer</h3>
      <p className="level-selector__help">
        Välj vilka arkivnivåer som ska visas i sökresultaten. Lämna alla omarkerade för att visa alla nivåer.
      </p>
      <div className="level-selector__grid">
        {ALL_LEVEL_IDS.map((id) => (
          <PortalCheckbox
            key={id}
            label={getLevelLabel(id)}
            checked={selected.includes(id)}
            onChange={(checked) => toggle(id, checked)}
          />
        ))}
      </div>
    </div>
  );
}
