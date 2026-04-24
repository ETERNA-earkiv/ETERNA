import { useEffect, useState, useCallback, Component, type ReactNode } from 'react';
import { useStore } from '@nanostores/react';
import {
  $query, $results, $totalCount, $loading, $error,
  $currentPage, $totalPages,
  search, goToPage, resetSearch,
} from '@lib/stores/search';
import { initConfig } from '@lib/stores/config';
import { loadRemoteLabels } from '@lib/utils/i18n';
import { SearchResultCard } from './SearchResultCard';
import { SearchFilterPanel } from './SearchFilterPanel';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import { PortalErrorMessage } from '../portal-ui/PortalErrorMessage';
import { PortalEmptyState } from '../portal-ui/PortalEmptyState';
import { PortalPagination } from '../portal-ui/PortalPagination';
import type { FilterParameter } from '@lib/types/api';
import './SearchPage.css';

/* ── Error Boundary ── */
class SearchErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state: { error: Error | null } = { error: null };
  static getDerivedStateFromError(error: Error) { return { error }; }
  render() {
    if (this.state.error) {
      return (
        <PortalErrorMessage
          error={this.state.error.message || 'Söksidan kunde inte visas.'}
          onRetry={() => this.setState({ error: null })}
        />
      );
    }
    return this.props.children;
  }
}

/* ── Search Page Content ── */
function SearchPageContent() {
  const query = useStore($query);
  const results = useStore($results);
  const totalCount = useStore($totalCount);
  const loading = useStore($loading);
  const error = useStore($error);
  const currentPage = useStore($currentPage);
  const totalPages = useStore($totalPages);
  const [localQuery, setLocalQuery] = useState('');
  const [initialized, setInitialized] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  useEffect(() => {
    Promise.all([initConfig(), loadRemoteLabels()]).then(() => {
      const params = new URLSearchParams(window.location.search);
      const q = params.get('q') || '';
      setLocalQuery(q);
      if (q) search(q);
      setInitialized(true);
    });
  }, []);

  const handleSearch = useCallback((q?: string) => {
    const searchQuery = q ?? localQuery;
    setLocalQuery(searchQuery);
    const url = new URL(window.location.href);
    if (searchQuery) { url.searchParams.set('q', searchQuery); }
    else { url.searchParams.delete('q'); }
    history.replaceState(null, '', url.toString());
    search(searchQuery);
  }, [localQuery]);

  function handleClear() {
    setLocalQuery('');
    resetSearch();
    const url = new URL(window.location.href);
    url.searchParams.delete('q');
    history.replaceState(null, '', url.toString());
  }

  function handlePageChange(page: number) {
    goToPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  if (!initialized) return <PortalSpinner />;

  return (
    <div className="search-page">
      <header className="search-header">
        <h2>Sök i arkivet</h2>

        <form
          className="search-form"
          onSubmit={(e) => { e.preventDefault(); handleSearch(); }}
          role="search"
        >
          <div className="search-input-wrapper">
            <svg className="search-input-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.3-4.3" />
            </svg>
            <input id="search-query"
              type="search"
              className="search-input"
              value={localQuery}
              onChange={(e) => setLocalQuery(e.target.value)}
              placeholder="Sök i arkivet"
              aria-label="Sök i arkivet"
              autoComplete="off"
            />
            {localQuery && (
              <button type="button" className="search-clear-btn" onClick={handleClear} aria-label="Rensa sökning">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
              </button>
            )}
          </div>
          <button
            type="button"
            className={`search-advanced-btn ${showAdvanced ? 'search-advanced-btn--active' : ''}`}
            onClick={(e) => { e.preventDefault(); e.stopPropagation(); setShowAdvanced(!showAdvanced); }}
            aria-expanded={showAdvanced}
            aria-label="Avancerad sökning"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <line x1="4" x2="4" y1="21" y2="14" /><line x1="4" x2="4" y1="10" y2="3" /><line x1="12" x2="12" y1="21" y2="12" /><line x1="12" x2="12" y1="8" y2="3" /><line x1="20" x2="20" y1="21" y2="16" /><line x1="20" x2="20" y1="12" y2="3" /><line x1="2" x2="6" y1="14" y2="14" /><line x1="10" x2="14" y1="8" y2="8" /><line x1="18" x2="22" y1="16" y2="16" />
            </svg>
          </button>
          <button type="submit" className="search-submit-btn">Sök</button>
        </form>

        <p className="search-help">Sök efter titel, beskrivning eller annat innehåll i arkivet.</p>

        {showAdvanced && (
          <SearchFilterPanel
            onFiltersChange={(filters: FilterParameter[]) => {
              search(localQuery, filters);
            }}
          />
        )}
      </header>

      <section className="search-results" aria-live="polite" aria-label="Sökresultat">
        {loading && <PortalSpinner text="Söker..." />}

        {!loading && error && (
          <PortalErrorMessage error={error} onRetry={() => handleSearch()} />
        )}

        {!loading && !error && results.length > 0 && (
          <>
            <div className="search-results-list" role="list">
              {results.map((aip) => (
                <SearchResultCard key={aip.uuid || aip.id} aip={aip} />
              ))}
            </div>

            {totalPages > 1 && (
              <PortalPagination
                key={currentPage}
                currentPage={currentPage}
                totalPages={totalPages}
                totalResults={totalCount}
                onPageChange={handlePageChange}
              />
            )}
          </>
        )}

        {!loading && !error && results.length === 0 && query && (
          <PortalEmptyState
            title="Inga resultat"
            message={`Inga arkivobjekt matchade "${query}". Prova ett annat sökord.`}
          />
        )}

{/* Inget meddelande vid tom startsida — sökfältet är självförklarande */}
      </section>
    </div>
  );
}

export function SearchPage() {
  return (
    <SearchErrorBoundary>
      <SearchPageContent />
    </SearchErrorBoundary>
  );
}

