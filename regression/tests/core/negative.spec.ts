import { buildEicarSip } from '../../src/eicar';
import { attachReports, expect, poll, test } from '../../src/fixtures';
import { equals } from '../../src/filters';
import { ingestSip, waitForIndexedAip, type IngestResult } from '../../src/ingest';
import { buildBrokenSip } from '../../src/sip';
import type { PreservationEvent } from '../../src/types';

test('SIP utan METS.xml ger ett misslyckat källobjekt och ingen AIP', async ({ admin, cfg, runId, cleanup }, testInfo) => {
  test.setTimeout(cfg.jobTimeoutMs + 120_000);
  const result = await ingestSip(admin, buildBrokenSip(`${runId}-broken`), poll(cfg), cleanup);
  await attachReports(testInfo, result.reports);
  expect(result.job.state).toBe('COMPLETED');
  expect(result.job.jobStats.sourceObjectsProcessedWithFailure).toBe(1);
  expect(result.aipIds).toEqual([]);
});

test.describe('SIP med EICAR-testvirus', () => {
  let result: IngestResult;

  test.beforeAll(async ({ admin, cfg, runId, cleanup }) => {
    test.setTimeout(cfg.jobTimeoutMs + 120_000);
    result = await ingestSip(admin, buildEicarSip(runId), poll(cfg), cleanup);
  });

  test('ClamAV hittar viruset och virus check-eventet får FAILURE', async ({ admin }, testInfo) => {
    await attachReports(testInfo, result.reports);
    // Jobbrapporten är platt (inga delsteg), så antivirussteget syns i ingestrapportens
    // pluginDetails och som preservation-event, inte som egen rapport.
    expect(result.job.state).toBe('COMPLETED');
    expect(result.job.jobStats.sourceObjectsProcessedWithPartialSuccess).toBe(1);
    expect(result.aipIds).toHaveLength(1);
    const details = result.reports.map((report) => report.pluginDetails).join('\n');
    expect(details).toContain('Eicar-Test-Signature FOUND');
    const events = await admin.find<PreservationEvent>('preservation/events', [equals('aipID', result.aipIds[0])], 500);
    const virusChecks = events.results.filter((event) => event.eventType === 'virus check');
    expect(virusChecks.map((event) => event.eventOutcome)).toEqual(['FAILURE']);
  });

  test('infekterat SIP blir inte en ACTIVE AIP', async ({ admin, cfg }) => {
    // Förutsätter exakt en AIP, vilket föregående test kontrollerar (annars vore loop-passet tomt).
    // Känt beteende (docs/api-probes.md, rad 15): virussteget i ConfigurableIngestPlugin
    // är mandatory=false, så ett infekterat SIP blir ändå en ACTIVE AIP. test.fail()
    // dokumenterar avvikelsen. Testet larmar ("unexpected pass") när beteendet ändras.
    test.fail();
    for (const id of result.aipIds) {
      const aip = await waitForIndexedAip(admin, id, cfg.jobPollMs);
      expect(aip.state).not.toBe('ACTIVE');
    }
  });
});
