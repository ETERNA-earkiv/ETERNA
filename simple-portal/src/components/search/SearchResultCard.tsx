import { lazy, Suspense, useState } from 'react';
import type { IndexedAIP } from '@lib/types/api';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import './SearchResultCard.css';

const AipExpandedContent = lazy(() => import('./AipExpandedContent'));

interface Props {
  aip: IndexedAIP;
}

export function SearchResultCard({ aip }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const [hasBeenOpened, setHasBeenOpened] = useState(false);
  const title = aip.title || aip.id || 'Utan titel';
  const date = aip.createdOn ? new Date(aip.createdOn).toLocaleDateString('sv-SE') : null;
  const aipId = aip.uuid || aip.id;

  function handleOpen() {
    setIsOpen(true);
    setHasBeenOpened(true);
  }

  return (
    <div className={`result-card ${isOpen ? 'result-card--expanded' : ''}`}>
      {/* Header — alltid synlig */}
      <div
        className={`result-card__main ${isOpen ? 'result-card__main--collapse' : ''}`}
        onClick={isOpen ? () => setIsOpen(false) : handleOpen}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); isOpen ? setIsOpen(false) : handleOpen(); } }}
        aria-expanded={isOpen}
        aria-label={isOpen ? 'Dölj detaljer' : `${title}, visa detaljer`}
        style={isOpen ? { cursor: 'pointer' } : undefined}
      >
        <div className="result-card__header-row">
          <h3 className="result-card__title">{title}</h3>
        </div>
        <div className="result-card__meta-row">
          {date && (
            <span className="result-card__meta-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect width="18" height="18" x="3" y="4" rx="2" /><line x1="16" x2="16" y1="2" y2="6" /><line x1="8" x2="8" y1="2" y2="6" /><line x1="3" x2="21" y1="10" y2="10" />
              </svg>
              Skapad: {date}
            </span>
          )}
          {aip.hasRepresentations && (
            <span className="result-card__meta-item">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48" />
              </svg>
              Har filer
            </span>
          )}
        </div>
        <div className={`result-card__expand-row`} aria-hidden="true">
          <span>{isOpen ? 'Dölj detaljer' : 'Visa detaljer'}</span>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d={isOpen ? 'm18 15-6-6-6 6' : 'm6 9 6 6 6-6'} />
          </svg>
        </div>
      </div>

      {/* Expanded content — monteras vid första öppning, döljs med CSS vid collapse */}
      {hasBeenOpened && (
        <div className="result-card__expanded-view" hidden={!isOpen}>
          <Suspense fallback={<PortalSpinner text="Laddar detaljer..." />}>
            <AipExpandedContent aipId={aipId} showHeaderButtons={true} />
          </Suspense>
        </div>
      )}
    </div>
  );
}
