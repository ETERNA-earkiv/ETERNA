import { lazy, Suspense, useState, useEffect } from 'react';
import type { IndexedAIP } from '@lib/types/api';
import { getAvailableMetadataIds, getMetadataXml } from '@lib/api/metadata';
import { parseXmlToFields } from '@lib/utils/metadata-parser';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import './SearchResultCard.css';

const AipExpandedContent = lazy(() => import('./AipExpandedContent'));

interface Props {
  aip: IndexedAIP;
}

export function SearchResultCard({ aip }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const [hasBeenOpened, setHasBeenOpened] = useState(false);
  const [originator, setOriginator] = useState<string | null>(null);
  const [startDate, setStartDate] = useState<string | null>(null);
  const title = aip.title || aip.id || 'Utan titel';
  const aipId = aip.uuid || aip.id;

  useEffect(() => {
    let cancelled = false;
    async function fetchDetails() {
      try {
        const ids = await getAvailableMetadataIds(aipId);
        const metaId = ids.find(id => id.toLowerCase().includes('ead_3')) || ids[0];
        if (!metaId) return;
        const xml = await getMetadataXml(aipId, metaId);
        const fields = parseXmlToFields(xml);
        if (cancelled) return;
        const creator = fields.find(f => f.label === 'Arkivbildare');
        if (creator) setOriginator(creator.value);
        const start = fields.find(f => f.label === 'Startdatum');
        if (start?.value) {
          const parsed = new Date(start.value);
          setStartDate(Number.isNaN(parsed.getTime()) ? start.value : parsed.toLocaleDateString('sv-SE'));
        }
      } catch {}
    }
    fetchDetails();
    return () => { cancelled = true; };
  }, [aipId]);

  function handleToggle() {
    if (!isOpen && !hasBeenOpened) setHasBeenOpened(true);
    setIsOpen((prev) => !prev);
  }

  return (
    <div className={`result-row${isOpen ? ' result-row--expanded' : ''}`}>
      <div
        className="result-row__header"
        onClick={handleToggle}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleToggle(); }
        }}
        aria-expanded={isOpen}
        aria-label={isOpen ? `Dölj detaljer för ${title}` : `Visa detaljer för ${title}`}
      >
        <svg
          className={`result-row__chevron${isOpen ? ' result-row__chevron--open' : ''}`}
          width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
          aria-hidden="true"
        >
          <path d="m9 18 6-6-6-6" />
        </svg>
        <span className="result-row__title">{title}</span>
        {originator && <span className="result-row__creator">{originator}</span>}
        {startDate && <span className="result-row__date">{startDate}</span>}
        {aip.hasRepresentations ? (
          <svg
            className="result-row__file-icon"
            width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            aria-label="Har filer"
          >
            <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48" />
          </svg>
        ) : (
          <span className="result-row__file-icon result-row__file-icon--empty" aria-hidden="true" />
        )}
      </div>

      {hasBeenOpened && (
        <div className="result-row__expanded-view" hidden={!isOpen}>
          <Suspense fallback={<PortalSpinner text="Laddar detaljer..." />}>
            <AipExpandedContent aipId={aipId} showHeaderButtons={true} />
          </Suspense>
        </div>
      )}
    </div>
  );
}
