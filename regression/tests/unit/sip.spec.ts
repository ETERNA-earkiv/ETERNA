import { expect, test } from '@playwright/test';
import { strFromU8, strToU8, unzipSync } from 'fflate';
import { buildBrokenSip, buildSip, buildValidSip, sha256 } from '../../src/sip';

const RUN = 'regtest20261010t123456';

test('giltigt SIP har CSIP-strukturen under <sipId>/', () => {
  const sip = buildValidSip(RUN);
  const root = `${RUN}-valid`;
  expect(sip.fileName).toBe(`${root}.zip`);
  expect(Object.keys(unzipSync(sip.zip)).sort()).toEqual(
    [
      `${root}/METS.xml`,
      `${root}/metadata/descriptive/dc.xml`,
      `${root}/representations/representation_1/METS.xml`,
      `${root}/representations/representation_1/data/document.txt`,
      `${root}/representations/representation_1/data/record.xml`,
      `${root}/schemas/DILCISExtensionMETS.xsd`,
      `${root}/schemas/DILCISExtensionSIPMETS.xsd`,
      `${root}/schemas/mets1_12.xsd`,
      `${root}/schemas/xlink.xsd`,
    ].sort(),
  );
});

test('titeln innehåller körnings-ID:t och datafilernas checksummor finns i representationens METS', () => {
  const sip = buildValidSip(RUN);
  expect(sip.title).toContain(RUN);
  const entries = unzipSync(sip.zip);
  const repMets = strFromU8(entries[`${RUN}-valid/representations/representation_1/METS.xml`]);
  for (const file of sip.files) {
    expect(file.sha256).toBe(sha256(entries[`${RUN}-valid/representations/representation_1/data/${file.name}`]));
    expect(repMets).toContain(`CHECKSUM="${file.sha256}"`);
  }
});

test('titel XML-escapas i dc.xml och METS', () => {
  const sip = buildSip('x', 'A & B <C>', [{ name: 'a.txt', mimeType: 'text/plain', content: strToU8('a') }]);
  const entries = unzipSync(sip.zip);
  expect(strFromU8(entries['x/metadata/descriptive/dc.xml'])).toContain('<title>A &amp; B &lt;C&gt;</title>');
  expect(strFromU8(entries['x/METS.xml'])).toContain('LABEL="A &amp; B &lt;C&gt;"');
});

test('ogiltigt sipId avvisas', () => {
  expect(() => buildSip('a/b', 't', [])).toThrow(/sipId/);
});

test('trasigt SIP saknar METS.xml', () => {
  const sip = buildBrokenSip(`${RUN}-broken`);
  expect(sip.fileName).toBe(`${RUN}-broken.zip`);
  expect(Object.keys(unzipSync(sip.zip))).toEqual([`${RUN}-broken/README.txt`]);
});
