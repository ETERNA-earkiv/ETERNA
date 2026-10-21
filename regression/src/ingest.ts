import { ApiError, type EternaClient } from './client';
import type { Cleanup } from './cleanup';
import { sleep, waitForJob, waitForReports, type PollOptions } from './jobs';
import type { SipPackage } from './sip';
import type { IndexedAIP, Job, Report, TransferredResource } from './types';

export const INGEST_PLUGIN = 'org.roda.core.plugins.base.ingest.v2.ConfigurableIngestPlugin';
export const SIP_TO_AIP_PLUGIN = 'org.roda.core.plugins.base.ingest.EARKSIP2ToAIPPlugin';
/** Report.NO_OUTCOME_OBJECT_ID: värdet när ingen AIP skapades. */
export const NO_OUTCOME_OBJECT_ID = 'NO_OUTCOME_ID';

const INDEX_WAIT_MS = 30_000;

export function ingestJobRequest(name: string, transferUuid: string) {
  return {
    name,
    plugin: INGEST_PLUGIN,
    // Stegen styrs av Boolean.parseBoolean på varje parameter, så alla anges explicit.
    pluginParameters: {
      'parameter.sip_to_aip_class': SIP_TO_AIP_PLUGIN,
      'parameter.do_virus_check': 'true',
      'parameter.do_descriptive_metadata_validation': 'true',
      'parameter.create.premis.skeleton': 'true',
      'parameter.do_file_format_identification': 'true',
      'parameter.do_producer_authorization_check': 'true',
      'parameter.do_apply_disposal_rules': 'true',
      'parameter.do_auto_accept': 'true',
    } as Record<string, string>,
    sourceObjects: { '@type': 'SelectedItemsListRequest', ids: [transferUuid] },
    sourceObjectsClass: 'org.roda.core.data.v2.ip.TransferredResource',
    priority: 'HIGH',
    parallelism: 'NORMAL',
  };
}

export function aipIdsFrom(reports: Report[]): string[] {
  const ids = reports
    .map((report) => report.outcomeObjectId)
    .filter((id): id is string => !!id && id !== NO_OUTCOME_OBJECT_ID);
  return [...new Set(ids)];
}

export interface IngestResult {
  transfer: TransferredResource;
  job: Job;
  reports: Report[];
  aipIds: string[];
}

/** Laddar upp SIP:et, kör standardinleveransen och väntar tills jobbet och rapporterna är klara. */
export async function ingestSip(
  client: EternaClient,
  sip: SipPackage,
  poll: PollOptions,
  cleanup: Cleanup,
): Promise<IngestResult> {
  const transfer = await client.upload(sip.fileName, sip.zip);
  cleanup.trackTransfer(transfer.uuid);
  const created = await client.postJson<Job>('/api/v2/jobs', ingestJobRequest(`regression ${sip.sipId}`, transfer.uuid));
  cleanup.trackIngestJob(created.id);
  const job = await waitForJob(client, created.id, poll);
  const reports = await waitForReports(client, job.id, 1, { timeoutMs: INDEX_WAIT_MS, pollMs: poll.pollMs });
  const aipIds = aipIdsFrom(reports);
  return { transfer, job, reports, aipIds };
}

/** Väntar tills AIP:n syns i indexet, eftersom indexeringen kan släpa efter jobbet. */
export async function waitForIndexedAip(client: EternaClient, aipId: string, pollMs: number): Promise<IndexedAIP> {
  const deadline = Date.now() + INDEX_WAIT_MS;
  for (;;) {
    try {
      return await client.getJson<IndexedAIP>(`/api/v2/aips/find/${encodeURIComponent(aipId)}`);
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 404 || Date.now() >= deadline) throw error;
    }
    await sleep(pollMs);
  }
}
