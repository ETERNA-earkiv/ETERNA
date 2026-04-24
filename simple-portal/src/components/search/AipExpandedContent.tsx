import { useEffect, useState } from 'react';
import DOMPurify from 'dompurify';
import { getMetadataHtml } from '@lib/api/aip';
import { getRepresentationsForAIP } from '@lib/api/representations';
import { getFilesForRepresentation, buildFileTree } from '@lib/api/files';
import { getAvailableMetadataIds, getMetadataXml } from '@lib/api/metadata';
import { parseXmlToFields } from '@lib/utils/metadata-parser';
import { formatStandardName, normalizeMetadataStandardId } from '@lib/utils/i18n';
import type { FileNode } from '@lib/types/api';
import type { MetadataField } from '@lib/api/metadata';
import { PortalSpinner } from '../portal-ui/PortalSpinner';
import { PortalErrorMessage } from '../portal-ui/PortalErrorMessage';
import { PortalAlert } from '../portal-ui/PortalAlert';
import { FileGrid } from './FileGrid';
// Lazy-load: jsPDF + JSZip laddas först vid nedladdning
const loadPackageUtil = () => import('@lib/utils/package').then(m => m.downloadSimplifiedPackage);
import { loadConfig } from '@lib/api/config';
import './AipExpandedContent.css';

interface MetadataSection {
  id: string;
  label: string;
  fields: MetadataField[];
  html: string;
}

interface Props {
  aipId: string;
  showHeaderButtons?: boolean;
}

export default function AipExpandedContent({ aipId, showHeaderButtons = true }: Props) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const [metadata, setMetadata] = useState<MetadataSection[]>([]);
  const [allFiles, setAllFiles] = useState<FileNode[]>([]);
  const [fileCount, setFileCount] = useState(0);
  const [isDownloading, setIsDownloading] = useState(false);
  const [visibleFields, setVisibleFields] = useState<Record<string, string[]>>({});

  useEffect(() => {
    loadData();
  }, [aipId]);

  async function loadData() {
    setLoading(true);
    setError(null);
    setWarning(null);
    const warnings: string[] = [];

    try {
      // Hämta synliga fält från config (cachad)
      const config = await loadConfig();
      const vf: Record<string, string[]> = config.visibilityConfig?.visibleMetadataFields || {};
      setVisibleFields(vf);

      let metaIds: string[] = [];
      let reps: Awaited<ReturnType<typeof getRepresentationsForAIP>> = [];

      try {
        [metaIds, reps] = await Promise.all([
          getAvailableMetadataIds(aipId),
          getRepresentationsForAIP(aipId),
        ]);
      } catch {
        // Om båda misslyckas — visa fel direkt
        setError('Kunde inte hämta information om paketet. Kontrollera att arkivet är tillgängligt.');
        return;
      }

      // Fetch metadata and files in parallel
      const [metaSections, fileData] = await Promise.all([
        Promise.all(
          metaIds.map(async (id): Promise<MetadataSection | null> => {
            try {
              let html = '';
              let xml = '';
              let partialFail = false;

              try { html = await getMetadataHtml(aipId, id); } catch { partialFail = true; }
              try { xml = await getMetadataXml(aipId, id); } catch { partialFail = true; }

              if (!html && !xml) {
                warnings.push(`Metadata "${id}" kunde inte laddas`);
                return null;
              }
              if (partialFail) {
                warnings.push(`Delar av metadata "${id}" kunde inte laddas`);
              }

              return { id, label: id, fields: parseXmlToFields(xml), html };
            } catch {
              warnings.push(`Metadata "${id}" kunde inte laddas`);
              return null;
            }
          }),
        ),
        Promise.all(
          reps.map(async (rep) => {
            try {
              const raw = await getFilesForRepresentation(rep.uuid);
              return { files: flattenFiles(buildFileTree(raw)), count: raw.filter(f => !f.isDirectory).length };
            } catch {
              warnings.push('En representation kunde inte laddas');
              return { files: [] as FileNode[], count: 0 };
            }
          }),
        ),
      ]);

      setMetadata(metaSections.filter((s): s is MetadataSection => s !== null));

      const allFetchedFiles = fileData.flatMap(d => d.files);
      const totalFileCount = fileData.reduce((sum, d) => sum + d.count, 0);
      setAllFiles(allFetchedFiles);
      setFileCount(totalFileCount);

      if (warnings.length > 0) {
        setWarning(`Delar av informationen kunde inte laddas: ${warnings.join(', ')}.`);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Kunde inte ladda detaljer');
    } finally {
      setLoading(false);
    }
  }

  async function downloadPackage() {
    setIsDownloading(true);
    try {
      const title = metadata[0]?.fields.find(f => f.label === 'Titel')?.value || aipId;
      // Filtrera metadata på synliga fält per standard
      const filteredMeta = metadata.map((s) => {
        const nid = normalizeMetadataId(s.id);
        const allowed = visibleFields[nid];
        return { ...s, fields: allowed && allowed.length > 0 ? s.fields.filter((f) => allowed.includes(f.label)) : s.fields };
      });
      const downloadFn = await loadPackageUtil();
      await downloadFn(title, aipId, filteredMeta, allFiles);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Okänt fel';
      // Partiella filfel = ZIP laddades ner men med saknade filer → varning
      if (msg.includes('kunde inte inkluderas')) {
        setWarning(msg);
      } else {
        setError(`Kunde inte skapa paketet: ${msg}`);
      }
    } finally {
      setIsDownloading(false);
    }
  }

  if (loading) return <PortalSpinner text="Laddar detaljer..." />;
  if (error) return <PortalErrorMessage error={error} onRetry={loadData} />;

  return (
    <div className="expanded-content">
      {warning && (
        <PortalAlert variant="warning" size="small">{warning}</PortalAlert>
      )}
      {showHeaderButtons && (
        <div className="expanded-content__actions">
          <button
            type="button"
            className="expanded-content__download-btn"
            onClick={downloadPackage}
            disabled={isDownloading}
            aria-busy={isDownloading}
            aria-label="Ladda ner komplett paket som ZIP-fil"
          >
            {isDownloading ? (
              <svg className="spin" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 12a9 9 0 1 1-6.219-8.56" /></svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><polyline points="7 10 12 15 17 10" /><line x1="12" x2="12" y1="15" y2="3" /></svg>
            )}
            Ladda ner komplett paket (.zip)
          </button>
        </div>
      )}

      {/* Metadata per standard — filtrerat på synliga fält */}
      {metadata.map((section) => {
        // Filtrera per standard
        const nid = normalizeMetadataId(section.id);
        const allowed = visibleFields[nid];
        const filteredFields = allowed && allowed.length > 0
          ? section.fields.filter((f) => allowed.includes(f.label))
          : section.fields;

        return filteredFields.length > 0 ? (
          <div key={section.id} className="expanded-content__meta-section">
            <h3 className="expanded-content__standard-heading">
              Information enligt: {formatStandardName(section.id)}
            </h3>
            <div className="expanded-content__section-card">
              <dl className="expanded-content__metadata-grid">
                {filteredFields.map((field, i) => (
                  <div key={i} className="expanded-content__meta-item">
                    <dt>{field.label}</dt>
                    <dd>{field.value}</dd>
                  </div>
                ))}
              </dl>
            </div>
          </div>
        ) : section.html ? (
          <div key={section.id} className="expanded-content__meta-section">
            <h3 className="expanded-content__standard-heading">
              Information enligt: {formatStandardName(section.id)}
            </h3>
            <div
              className="expanded-content__section-card expanded-content__meta-html"
              dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(section.html) }}
            />
          </div>
        ) : null;
      })}

      {/* Files */}
      <div className="expanded-content__files-header">
        <h3>Filer ({fileCount})</h3>
      </div>
      <div className="expanded-content__files-container">
        {allFiles.length > 0 ? (
          <FileGrid files={allFiles} />
        ) : (
          <p className="expanded-content__no-files">Inga filer hittades i detta paket.</p>
        )}
      </div>
    </div>
  );
}

/**
 * Normalisera metadata-ID för matchning mot config.
 * API:t returnerar t.ex. "ead_2002.xml" — ta bort .xml och lowercase.
 */
function normalizeMetadataId(id: string): string {
  return normalizeMetadataStandardId(id);
}

function flattenFiles(nodes: FileNode[]): FileNode[] {
  let result: FileNode[] = [];
  for (const node of nodes) {
    if (!node.isDirectory) result.push(node);
    if (node.children) result = [...result, ...flattenFiles(node.children)];
  }
  return result;
}
