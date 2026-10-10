// Minimala typer för de delar av V2 API:ts svar som sviten läser.

export interface IndexResult<T> {
  offset: number;
  limit: number;
  totalCount: number;
  results: T[];
}

export interface TransferredResource {
  uuid: string;
  id: string;
  name: string;
}

export interface JobStats {
  sourceObjectsCount: number;
  sourceObjectsProcessedWithSuccess: number;
  sourceObjectsProcessedWithPartialSuccess: number;
  sourceObjectsProcessedWithFailure: number;
}

export interface Job {
  id: string;
  name: string;
  state: string;
  stateDetails?: string;
  jobStats: JobStats;
}

export interface Report {
  id: string;
  jobId: string;
  sourceObjectId: string;
  outcomeObjectId: string;
  outcomeObjectClass: string;
  plugin: string;
  pluginName: string;
  pluginState: string;
  pluginDetails: string;
  reports?: Report[];
}

export interface IndexedAIP {
  id: string;
  title: string;
  state: string;
  ingestJobId: string;
}

export interface IndexedRepresentation {
  uuid: string;
  id: string;
  aipId: string;
}

export interface IndexedFile {
  uuid: string;
  id: string;
  aipId: string;
  originalName: string;
  size: number;
  fileFormat?: { pronom?: string; mimeType?: string };
}

export interface PreservationEvent {
  id: string;
  aipID: string;
  eventType: string;
  eventOutcome: string;
}

export interface User {
  id: string;
  name: string;
  type: string;
}
