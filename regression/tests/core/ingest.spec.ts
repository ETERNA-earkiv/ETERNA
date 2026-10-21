import { attachReports, expect, test } from '../../src/fixtures';

test('giltigt SIP ingestas utan fel och ger exakt en AIP', async ({ validIngest }, testInfo) => {
  await attachReports(testInfo, validIngest.reports);
  expect(validIngest.job.state).toBe('COMPLETED');
  expect(validIngest.job.jobStats.sourceObjectsProcessedWithFailure).toBe(0);
  expect(validIngest.job.jobStats.sourceObjectsProcessedWithSuccess).toBe(1);
  expect(validIngest.aipIds).toHaveLength(1);
});
