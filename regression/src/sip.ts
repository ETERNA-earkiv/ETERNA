import { createHash, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { strToU8, zipSync } from 'fflate';

// Bygger E-ARK CSIP-SIP:ar i minnet. Formatet är portat från
// eterna-stack/test-data/create_sip.py, vars paket ingestas av EARKSIP2ToAIPPlugin.

export interface SipInputFile {
  name: string;
  mimeType: string;
  content: Uint8Array;
}

export interface SipFileInfo {
  name: string;
  size: number;
  sha256: string;
}

export interface SipPackage {
  sipId: string;
  fileName: string;
  zip: Buffer;
}

export interface BuiltSip extends SipPackage {
  title: string;
  files: SipFileInfo[];
}

/** Förväntade PRONOM-ID:n från Siegfried för filerna i buildValidSip. */
export const VALID_SIP_PRONOM: Record<string, string> = {
  'document.txt': 'x-fmt/111',
  'record.xml': 'fmt/101',
};

const SCHEMA_DIR = join(__dirname, '..', 'fixtures', 'schemas');
const SCHEMA_FILES = ['mets1_12.xsd', 'xlink.xsd', 'DILCISExtensionMETS.xsd', 'DILCISExtensionSIPMETS.xsd'];
const REP = 'representation_1';
const PROFILE = 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml';
const METS_NS =
  'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.loc.gov/METS/" ' +
  'xmlns:csip="https://DILCIS.eu/XML/METS/CSIPExtensionMETS" xmlns:xlink="http://www.w3.org/1999/xlink" ' +
  'xmlns:sip="https://DILCIS.eu/XML/METS/SIPExtensionMETS"';

export const sha256 = (data: Uint8Array): string => createHash('sha256').update(data).digest('hex').toUpperCase();

export function escapeXml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

const newId = (): string => `uuid-${randomUUID().toUpperCase()}`;
const href = (rel: string): string => encodeURIComponent(rel);

function schemaLocation(prefix: string): string {
  return [
    `http://www.loc.gov/METS/ ${prefix}schemas/mets1_12.xsd`,
    `http://www.w3.org/1999/xlink ${prefix}schemas/xlink.xsd`,
    `https://DILCIS.eu/XML/METS/CSIPExtensionMETS ${prefix}schemas/DILCISExtensionMETS.xsd`,
    `https://DILCIS.eu/XML/METS/SIPExtensionMETS ${prefix}schemas/DILCISExtensionSIPMETS.xsd`,
  ].join(' ');
}

function dublinCore(title: string, identifier: string): string {
  return `<?xml version="1.0" encoding="UTF-8"?>
<simpledc xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <title>${escapeXml(title)}</title>
  <identifier>${escapeXml(identifier)}</identifier>
  <type>Text</type>
</simpledc>
`;
}

function representationMets(files: SipInputFile[], created: string): string {
  const dataGroup = newId();
  const entries = files.map((file) => {
    const id = newId();
    const xml =
      `            <file ID="${id}" MIMETYPE="${file.mimeType}" SIZE="${file.content.length}" CREATED="${created}" ` +
      `CHECKSUM="${sha256(file.content)}" CHECKSUMTYPE="SHA-256">\n` +
      `                <FLocat xlink:type="simple" xlink:href="${href(`data/${file.name}`)}" LOCTYPE="URL"/>\n` +
      `            </file>`;
    return { id, xml };
  });
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<mets ${METS_NS} OBJID="${REP}" LABEL="" TYPE="Mixed" csip:CONTENTINFORMATIONTYPE="MIXED" PROFILE="${PROFILE}" xsi:schemaLocation="${schemaLocation('../../')}">
    <metsHdr CREATEDATE="${created}" LASTMODDATE="${created}" RECORDSTATUS="NEW" csip:OAISPACKAGETYPE="SIP"/>
    <amdSec ID="${newId()}"/>
    <fileSec ID="${newId()}">
        <fileGrp ID="${dataGroup}" USE="Data">
${entries.map((entry) => entry.xml).join('\n')}
        </fileGrp>
        <fileGrp ID="${newId()}" USE="Schemas"/>
        <fileGrp ID="${newId()}" USE="Documentation"/>
    </fileSec>
    <structMap ID="${newId()}" TYPE="PHYSICAL" LABEL="CSIP">
        <div ID="${newId()}" TYPE="ORIGINAL" LABEL="${REP}">
            <div ID="${newId()}" LABEL="Metadata"/>
            <div ID="${newId()}" LABEL="Metadata/Other"/>
            <div ID="${newId()}" LABEL="Data">
                <fptr FILEID="${dataGroup}"/>
${entries.map((entry) => `                <fptr FILEID="${entry.id}"/>`).join('\n')}
            </div>
            <div ID="${newId()}" LABEL="Schemas"/>
            <div ID="${newId()}" LABEL="Documentation"/>
        </div>
    </structMap>
</mets>
`;
}

function rootMets(sipId: string, title: string, dc: Uint8Array, repMets: Uint8Array, created: string): string {
  const dmd = newId();
  const repGroup = newId();
  const dcRel = 'metadata/descriptive/dc.xml';
  const repRel = `representations/${REP}/METS.xml`;
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<mets ${METS_NS} OBJID="${escapeXml(sipId)}" LABEL="${escapeXml(title)}" TYPE="Mixed" PROFILE="${PROFILE}" xsi:schemaLocation="${schemaLocation('')}">
    <metsHdr CREATEDATE="${created}" LASTMODDATE="${created}" RECORDSTATUS="NEW" csip:OAISPACKAGETYPE="SIP">
        <agent ROLE="CREATOR" TYPE="OTHER" OTHERTYPE="SOFTWARE">
            <name>ETERNA regression suite</name>
            <note csip:NOTETYPE="SOFTWARE VERSION">1.0</note>
        </agent>
    </metsHdr>
    <dmdSec ID="${dmd}" CREATED="${created}" STATUS="CURRENT">
        <mdRef ID="${newId()}" LOCTYPE="URL" xlink:type="simple" xlink:href="${href(dcRel)}" MDTYPE="DC" MIMETYPE="application/xml" SIZE="${dc.length}" CREATED="${created}" CHECKSUM="${sha256(dc)}" CHECKSUMTYPE="SHA-256"/>
    </dmdSec>
    <amdSec ID="${newId()}"/>
    <fileSec ID="${newId()}">
        <fileGrp ID="${newId()}" USE="Data"/>
        <fileGrp ID="${repGroup}" USE="Representations/${REP}">
            <file ID="${newId()}" MIMETYPE="application/xml" SIZE="${repMets.length}" CREATED="${created}" CHECKSUM="${sha256(repMets)}" CHECKSUMTYPE="SHA-256">
                <FLocat xlink:type="simple" xlink:href="${href(repRel)}" LOCTYPE="URL"/>
            </file>
        </fileGrp>
    </fileSec>
    <structMap ID="${newId()}" TYPE="PHYSICAL" LABEL="CSIP">
        <div ID="${newId()}" LABEL="${escapeXml(sipId)}">
            <div ID="${newId()}" DMDID="${dmd}" LABEL="Metadata"/>
            <div ID="${newId()}" LABEL="Data"/>
            <div ID="${newId()}" LABEL="Representations/${REP}">
                <mptr LOCTYPE="URL" xlink:type="simple" xlink:href="${href(repRel)}" xlink:title="${repGroup}"/>
            </div>
        </div>
    </structMap>
</mets>
`;
}

function assertSipId(sipId: string): void {
  if (!/^[A-Za-z0-9_-]+$/.test(sipId)) throw new Error(`Ogiltigt sipId "${sipId}": bara A-Z, a-z, 0-9, _ och - är tillåtna`);
}

export function buildSip(sipId: string, title: string, files: SipInputFile[]): BuiltSip {
  assertSipId(sipId);
  const created = new Date().toISOString();
  const dc = strToU8(dublinCore(title, sipId));
  const repMets = strToU8(representationMets(files, created));
  const entries: Record<string, Uint8Array> = {
    [`${sipId}/METS.xml`]: strToU8(rootMets(sipId, title, dc, repMets, created)),
    [`${sipId}/metadata/descriptive/dc.xml`]: dc,
    [`${sipId}/representations/${REP}/METS.xml`]: repMets,
  };
  for (const file of files) entries[`${sipId}/representations/${REP}/data/${file.name}`] = file.content;
  for (const schema of SCHEMA_FILES) entries[`${sipId}/schemas/${schema}`] = readFileSync(join(SCHEMA_DIR, schema));
  return {
    sipId,
    title,
    fileName: `${sipId}.zip`,
    zip: Buffer.from(zipSync(entries)),
    files: files.map((file) => ({ name: file.name, size: file.content.length, sha256: sha256(file.content) })),
  };
}

/** Giltigt SIP med en txt- och en xml-fil. Titeln innehåller körnings-ID:t för sökning. */
export function buildValidSip(runId: string): BuiltSip {
  return buildSip(`${runId}-valid`, `Regressionstest ${runId}`, [
    { name: 'document.txt', mimeType: 'text/plain', content: strToU8(`Regressionstest för ETERNA.\nKörning: ${runId}\n`) },
    {
      name: 'record.xml',
      mimeType: 'application/xml',
      content: strToU8(`<?xml version="1.0" encoding="UTF-8"?>\n<record><run>${runId}</run></record>\n`),
    },
  ]);
}

/** Zip utan METS.xml, som ska avvisas av ingesten. */
export function buildBrokenSip(sipId: string): SipPackage {
  assertSipId(sipId);
  const zip = zipSync({ [`${sipId}/README.txt`]: strToU8('Detta SIP saknar METS.xml med avsikt.\n') });
  return { sipId, fileName: `${sipId}.zip`, zip: Buffer.from(zip) };
}
