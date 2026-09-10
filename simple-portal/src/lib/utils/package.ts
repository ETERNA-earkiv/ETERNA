/**
 * Package download — ZIP med metadata-PDF och alla filer.
 * Porterad exakt från Angular package.service.ts.
 */

import JSZip from 'jszip';
import { jsPDF } from 'jspdf';
import { saveAs } from 'file-saver';
import type { FileNode } from '../types/api';
import type { MetadataField } from '../api/metadata';

interface MetadataSection {
  id: string;
  label: string;
  fields: MetadataField[];
}

/**
 * Skapar och laddar ner ett förenklat ZIP-paket:
 * 1. metadata.pdf (eller metadata.html som fallback)
 * 2. Alla filer i en undermapp
 *
 * `sourceText` är kundspecifik (t.ex. "Informationen är hämtad från …") och
 * kommer från config.json (downloadConfig.sourceText). Tom sträng = ingen rad.
 */
export async function downloadSimplifiedPackage(
  title: string,
  aipId: string,
  metadata: MetadataSection[],
  files: FileNode[],
  sourceText = '',
): Promise<void> {
  const zip = new JSZip();
  const folderName = sanitizeFilename(title || aipId);
  const rootFolder = zip.folder(folderName);
  if (!rootFolder) throw new Error('Kunde inte skapa ZIP-mapp');

  // 1. Generera metadata-PDF. Kort namn med underscore-prefix: sorterar först,
  //    krockar inte med innehållsfiler och håller sökvägen kort (Windows MAX_PATH).
  const pdfName = '_metadata.pdf';
  try {
    const pdfBlob = generateMetadataPdf(title, metadata, sourceText);
    rootFolder.file(pdfName, pdfBlob);
  } catch {
    const htmlName = '_metadata.html';
    const html = generateMetadataHtml(title, metadata, sourceText);
    rootFolder.file(htmlName, html);
    rootFolder.file('metadata_note.txt', `PDF-generering misslyckades. Metadata finns i ${htmlName}.`);
  }

  // 2. Ladda ner alla filer i batchar om 5
  const fileList = files.filter((f) => !f.isDirectory);
  const BATCH_SIZE = 5;
  const failedFiles: string[] = [];

  for (let i = 0; i < fileList.length; i += BATCH_SIZE) {
    const batch = fileList.slice(i, i + BATCH_SIZE);
    await Promise.all(
      batch.map(async (file) => {
        try {
          const res = await fetch(`/api/v2/files/${file.id}/preview`, { credentials: 'include' });
          if (!res.ok) throw new Error(`${res.status}`);
          const blob = await res.blob();

          let filePath = file.name;
          if (file.path && file.path.length > 1) {
            const pathParts = [...file.path];
            pathParts.pop();
            if (pathParts.length > 0) pathParts.shift();
            if (pathParts.length > 0) filePath = `${pathParts.join('/')}/${file.name}`;
          }

          rootFolder.file(filePath, blob);
        } catch {
          failedFiles.push(file.name);
        }
      }),
    );
  }

  // Lägg till felrapport om filer misslyckades
  if (failedFiles.length > 0) {
    const report = [
      `${failedFiles.length} av ${fileList.length} filer kunde inte laddas ner:`,
      '',
      ...failedFiles.map((f) => `  - ${f}`),
      '',
      'Försök ladda ner paketet igen eller kontakta administratören.',
    ].join('\n');
    rootFolder.file('_misslyckade_filer.txt', report);
  }

  // 3. Generera ZIP och ladda ner
  const content = await zip.generateAsync({ type: 'blob' });
  saveAs(content, `${folderName}.zip`);

  if (failedFiles.length > 0) {
    throw new Error(`Paketet laddades ner, men ${failedFiles.length} av ${fileList.length} filer kunde inte inkluderas.`);
  }
}

/**
 * Generera metadata-PDF med jsPDF.
 * Rubrik (radbruten) + källa + hämtningsdatum, därefter alla metadatafält.
 */
function generateMetadataPdf(
  title: string,
  sections: MetadataSection[],
  sourceText: string,
): Blob {
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  const pw = doc.internal.pageSize.getWidth();
  const ph = doc.internal.pageSize.getHeight();
  const m = 20;
  const maxW = pw - m * 2;
  let y = 30;

  // Rubrik — radbryts så att hela titeln syns
  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  const titleLines: string[] = doc.splitTextToSize(title, maxW);
  doc.text(titleLines, m, y);
  y += titleLines.length * 9 + 1;

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(100);

  // Källa (kundspecifik, från config)
  if (sourceText) {
    doc.text(sourceText, m, y);
    y += 6;
  }

  // Datum
  doc.text(`Hämtad: ${new Date().toLocaleDateString('sv-SE')} ${new Date().toLocaleTimeString('sv-SE', { hour: '2-digit', minute: '2-digit' })}`, m, y);
  doc.setTextColor(0);
  y += 4;

  // Linje
  doc.setDrawColor(50);
  doc.setLineWidth(0.5);
  doc.line(m, y, pw - m, y);
  y += 12;

  // Metadatafält
  doc.setFontSize(9);
  for (const section of sections) {
    if (section.fields.length === 0) continue;

    for (const field of section.fields) {
      if (y > ph - 25) { doc.addPage(); y = 20; }

      // Label (uppercase, grå)
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(95, 99, 104); // #5f6368
      doc.text(field.label.toUpperCase(), m, y);
      y += 4;

      // Value
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(32, 33, 36); // #202124
      const lines = doc.splitTextToSize(field.value || '—', maxW);
      doc.text(lines, m, y);
      y += lines.length * 4 + 5;
    }

    y += 6;
  }

  return doc.output('blob');
}

/**
 * Generera HTML-metadata (fallback om PDF misslyckas).
 */
function generateMetadataHtml(
  title: string,
  sections: MetadataSection[],
  sourceText: string,
): string {
  const sectionsHtml = sections
    .filter((s) => s.fields.length > 0)
    .map((s, i) => {
      const fieldsHtml = s.fields.map((f) =>
        `<div class="meta-item"><dt>${esc(f.label)}</dt><dd>${esc(f.value)}</dd></div>`,
      ).join('');
      return `
        <div class="section"${i > 0 ? ' style="page-break-before:always"' : ''}>
          <div class="section-card"><dl class="metadata-grid">${fieldsHtml}</dl></div>
        </div>`;
    }).join('');

  const sourceHtml = sourceText ? `<p class="source">${esc(sourceText)}</p>` : '';

  return `<!DOCTYPE html>
<html lang="sv"><head><meta charset="UTF-8"><title>${esc(title)}</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'Segoe UI',Arial,sans-serif;font-size:14px;line-height:1.5;color:#333;padding:40px;background:#fff}
.header{border-bottom:3px solid #333;padding-bottom:20px;margin-bottom:30px}
h1{font-size:28px;font-weight:500;margin-bottom:8px;overflow-wrap:anywhere}
.source{font-size:13px;color:#666}
.section{margin-bottom:30px}
.section-card{background:#fff;border:1px solid #e0e0e0;border-radius:8px;padding:24px;margin-top:10px}
.metadata-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:24px 32px}
.meta-item{display:flex;flex-direction:column;gap:4px}
.meta-item dt{font-size:11px;text-transform:uppercase;letter-spacing:0.05em;color:#5f6368;font-weight:600}
.meta-item dd{font-size:14px;color:#202124;margin:0;word-break:break-word}
</style></head><body>
<div class="header"><h1>${esc(title)}</h1>${sourceHtml}</div>
${sectionsHtml}
</body></html>`;
}

/** Max längd på mapp-/zipnamn — titeln upprepas i den extraherade sökvägen
 * (Explorer-mapp + rotmapp) och Windows har en gräns på 260 tecken totalt. */
const MAX_FOLDER_NAME_LENGTH = 50;

function sanitizeFilename(name: string): string {
  const cleaned = name
    .replace(/[^a-z0-9à-ö]/gi, '_')
    .replace(/_{2,}/g, '_')
    .replace(/^_+|_+$/g, '');
  return (cleaned || 'paket').slice(0, MAX_FOLDER_NAME_LENGTH).replace(/_+$/, '');
}

function esc(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
