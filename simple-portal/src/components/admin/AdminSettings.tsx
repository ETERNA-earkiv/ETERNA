import { useRef, useState } from 'react';
import { PortalButton } from '../portal-ui/PortalButton';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { invalidateConfigCache } from '@lib/api/config';
import './AdminSettings.css';

export function AdminSettings() {
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  async function exportConfig() {
    setExporting(true);
    setError(null);
    try {
      const res = await fetch('/api/config');
      if (!res.ok) throw new Error('Kunde inte hämta konfigurationen.');
      const data = await res.json();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'config.json';
      a.click();
      URL.revokeObjectURL(url);
      setStatus('Konfigurationen exporterades.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export misslyckades.');
    } finally {
      setExporting(false);
    }
  }

  async function importConfig(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setStatus(null);
    try {
      const text = await file.text();
      const data = JSON.parse(text);

      const res = await fetch('/api/config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data),
      });

      if (!res.ok) throw new Error('Kunde inte spara. Är du inloggad?');
      invalidateConfigCache();
      setStatus('Konfigurationen importerades. Laddar om...');
      setTimeout(() => window.location.reload(), 1000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ogiltig JSON-fil.');
    }
    e.target.value = '';
  }

  return (
    <section className="admin-settings">
      <h1>Konfiguration</h1>
      <p className="admin-settings__desc">Exportera och importera portalens konfiguration.</p>

      {status && (
        <PortalAlert variant="success" size="small">
          {status}
        </PortalAlert>
      )}
      {error && (
        <PortalAlert variant="danger" size="small">
          {error}
        </PortalAlert>
      )}

      <div className="admin-settings__section">
        <h2>Exportera / importera</h2>
        <p>Exportera nuvarande config.json eller importera en tidigare sparad. Import skriver till den delade konfigurationen — alla besökare påverkas.</p>

        <div className="admin-settings__actions">
          <PortalButton
            variant="primary"
            onClick={exportConfig}
            disabled={exporting}
            loading={exporting}
            ariaLabel="Exportera konfiguration"
          >
            Exportera config.json
          </PortalButton>

          <PortalButton
            variant="secondary"
            onClick={() => fileRef.current?.click()}
            ariaLabel="Importera konfiguration"
          >
            Importera config.json
          </PortalButton>
          <input ref={fileRef} type="file" accept=".json" onChange={importConfig} className="sr-only" tabIndex={-1} />
        </div>
      </div>
    </section>
  );
}
