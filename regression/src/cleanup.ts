import type { EternaClient } from './client';
import { equals } from './filters';
import { runJob, type PollOptions } from './jobs';
import type { IndexedAIP, Job } from './types';

const selected = (ids: string[]) => ({ '@type': 'SelectedItemsListRequest', ids });

function assertCompleted(what: string, job: Job): void {
  if (job.state !== 'COMPLETED' || job.jobStats.sourceObjectsProcessedWithFailure > 0) {
    throw new Error(
      `${what}: jobbet ${job.id} slutade i ${job.state} med ${job.jobStats.sourceObjectsProcessedWithFailure} fel`,
    );
  }
}

/** Spårar testdata och tar bort den via API:t. Fel samlas och rapporteras i stället för att döljas. */
export class Cleanup {
  private readonly ingestJobs = new Set<string>();
  private readonly transfers = new Set<string>();
  private readonly users = new Set<string>();

  /** Spåras direkt när jobbet skapats, så att AIP:erna hittas även om ingesten avbryts. */
  trackIngestJob(jobId: string): void {
    this.ingestJobs.add(jobId);
  }

  trackTransfer(uuid: string): void {
    this.transfers.add(uuid);
  }

  trackUser(name: string): void {
    this.users.add(name);
  }

  async run(client: EternaClient, poll: PollOptions): Promise<void> {
    const errors: string[] = [];
    const attempt = async (what: string, action: () => Promise<void>): Promise<void> => {
      try {
        await action();
      } catch (error) {
        errors.push(`${what}: ${(error as Error).message}`);
      }
    };

    for (const name of this.users) {
      await attempt(`användare ${name}`, () =>
        client.deleteIfExists(`/api/v2/members/users/${encodeURIComponent(name)}`),
      );
    }

    if (this.ingestJobs.size > 0) {
      await attempt('AIP:er', async () => {
        const aips = new Set<string>();
        for (const jobId of this.ingestJobs) {
          const found = await client.find<IndexedAIP>('aips', [equals('ingestJobId', jobId)], 100);
          for (const aip of found.results) aips.add(aip.id);
        }
        if (aips.size === 0) return;
        const body = { itemsToDelete: selected([...aips]), details: 'Städning efter regressionstest' };
        assertCompleted('Radering av AIP:er', await runJob(client, '/api/v2/aips/delete', body, poll));
      });
    }

    await attempt('transfers', async () => {
      // Ingesten kan redan ha flyttat bort transfern, så bara de som finns kvar raderas.
      const remaining: string[] = [];
      for (const uuid of this.transfers) {
        if (await client.exists(`/api/v2/transfers/${encodeURIComponent(uuid)}`)) remaining.push(uuid);
      }
      if (remaining.length > 0) {
        assertCompleted('Radering av transfers', await runJob(client, '/api/v2/transfers/delete', selected(remaining), poll));
      }
    });

    if (errors.length > 0) throw new Error(`Städningen misslyckades delvis:\n${errors.join('\n')}`);
  }
}
