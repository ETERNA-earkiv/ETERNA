/**
 * V2 API Type Definitions
 * Based on RODA V2 OpenAPI spec and V2_API_NOTES.md
 */

// ============================================================================
// Filter Parameters (for search requests)
// ============================================================================

/**
 * Base interface for all filter parameters
 */
export interface FilterParameter {
    type: string;
}

/**
 * Simple filter parameter for exact matches
 */
export interface SimpleFilterParameter extends FilterParameter {
    type: 'SimpleFilterParameter';
    name: string;
    value: string;
}

/**
 * Basic search filter for text search
 */
export interface BasicSearchFilterParameter extends FilterParameter {
    type: 'BasicSearchFilterParameter';
    name: string;
    value: string;
}

/**
 * Date range filter parameter
 */
export interface DateRangeFilterParameter extends FilterParameter {
    type: 'DateRangeFilterParameter';
    name: string;
    fromValue: string;
    toValue: string;
}

/**
 * AND filter combining multiple parameters
 */
export interface AndFiltersParameters extends FilterParameter {
    type: 'AndFiltersParameters';
    values: FilterParameter[];
}

/**
 * OR filter combining multiple parameters
 */
export interface OrFiltersParameters extends FilterParameter {
    type: 'OrFiltersParameters';
    values: FilterParameter[];
}

/**
 * All filter parameter - matches everything
 */
export interface AllFilterParameter extends FilterParameter {
    type: 'AllFilterParameter';
}

/**
 * Empty key filter - matches records where a field is empty/null
 */
export interface EmptyKeyFilterParameter extends FilterParameter {
    type: 'EmptyKeyFilterParameter';
    name: string;
}

// ============================================================================
// Request Types
// ============================================================================

/**
 * Sublist for pagination
 */
export interface Sublist {
    firstElementIndex: number;
    maximumElementCount: number;
}

/**
 * Sort parameter for ordering results
 */
export interface SortParameter {
    name: string;
    descending: boolean;
}

/**
 * Sorter containing sort parameters
 */
export interface Sorter {
    parameters: SortParameter[];
}

/**
 * Search filter containing filter parameters
 */
export interface SearchFilter {
    parameters: FilterParameter[];
}

/**
 * FindRequest - Main search request body for V2 API
 * Used with POST /api/v2/aips/find, /api/v2/files/find, etc.
 */
export interface FindRequest {
    filter: SearchFilter;
    onlyActive?: boolean;
    sublist?: Sublist;
    sorter?: Sorter;
    facets?: Record<string, unknown>;
    exportFacets?: boolean;
    fieldsToReturn?: string[];
}

// ============================================================================
// Response Types
// ============================================================================

/**
 * Generic index result wrapper
 */
export interface IndexResult<T> {
    offset: number;
    limit: number;
    totalCount: number;
    results: T[];
    facetResults: FacetResult[];
    date: number;
}

/**
 * Facet result for search aggregations
 */
export interface FacetResult {
    field: string;
    values: FacetValue[];
}

/**
 * Individual facet value
 */
export interface FacetValue {
    label: string;
    value: string;
    count: number;
}

// ============================================================================
// AIP Types
// ============================================================================

/**
 * Indexed AIP (Archival Information Package)
 */
export interface IndexedAIP {
    uuid: string;
    id: string;
    state: string;
    type: string;
    level: string;
    title: string;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    parentID?: string;
    ancestors?: string[];
    hasRepresentations: boolean;
    numberOfSubmissionFiles: number;
    numberOfDocumentationFiles: number;
    numberOfSchemaFiles: number;
    ingestSIPIds?: string[];
    ingestJobId?: string;
    permissions?: Permissions;
    fields?: Record<string, unknown>;
}

/**
 * Response wrapper for AIP in search results
 */
export interface AIPResult {
    aip: IndexedAIP;
}

/**
 * AIP with enriched metadata for detail view
 */
export interface AIPWithMetadata extends IndexedAIP {
    descriptiveMetadataId?: string | string[];
    descriptiveMetadata?: MetadataResult[];
    representations?: Representation[];
    totalRepresentations?: number;
}

// ============================================================================
// Representation Types
// ============================================================================

/**
 * Representation of an AIP
 */
export interface Representation {
    uuid: string;
    id: string;
    aipId: string;
    original: boolean;
    representationStates: string[];
    type: string;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    descriptiveMetadata?: DescriptiveMetadata[];
    files?: FileNode[];
}

/**
 * Response wrapper for Representation in search results
 */
export interface RepresentationResult {
    representation: Representation;
}

// ============================================================================
// File Types
// ============================================================================

/**
 * Indexed file from the repository
 */
export interface IndexedFile {
    uuid: string;
    id: string;
    aipId: string;
    representationId: string;
    representationUUID: string;
    parentUUID?: string;
    path: string[];
    size: number;
    isDirectory: boolean;
    createdOn: string;
    fileFormat?: FileFormat;
    hash?: string[];
    storagePath?: string;
    originalName?: string;
    otherProperties?: Record<string, unknown>;
}

/**
 * Response wrapper for File in search results
 */
export interface FileResult {
    file: IndexedFile;
}

/**
 * File format information
 */
export interface FileFormat {
    mimeType: string;
    formatDesignationName: string;
    formatDesignationVersion: string;
    pronom?: string;
    extension?: string;
}

/**
 * File tree node for UI representation
 */
export interface FileNode {
    id: string;
    name: string;
    path?: string[];  // Full path segments for download URL
    isDirectory: boolean;
    size?: number;
    mimetype?: string;
    extension?: string;
    formatDesignationName?: string;
    formatDesignationVersion?: string;
    pronom?: string;
    createdOn?: string;
    hash?: string[];
    storagePath?: string;
    otherProperties?: Record<string, unknown>;
    children?: FileNode[];
}

// ============================================================================
// Metadata Types
// ============================================================================

/**
 * Descriptive metadata reference
 */
export interface DescriptiveMetadata {
    id: string;
    aipId: string;
    representationId?: string;
    type: string;
    version: string;
}

/**
 * A single metadata key-value field parsed from XML
 */
export interface MetadataField {
    label: string;
    value: string;
}

/**
 * Metadata result with rendered HTML and parsed fields
 */
export interface MetadataResult {
    id: string;
    html: string;
    fields: MetadataField[];
    children?: MetadataResult[];
}

/**
 * Metadata standard info from RODA's descriptive metadata information endpoint
 */
export interface MetadataInfo {
    id: string;
    label: string;
}

// ============================================================================
// Permission Types
// ============================================================================

/**
 * Permission set for CRUD operations
 */
export interface Permission {
    CREATE: string[];
    READ: string[];
    UPDATE: string[];
    DELETE: string[];
    GRANT: string[];
}

/**
 * Permissions for users and groups
 */
export interface Permissions {
    users: Permission;
    groups: Permission;
}

// ============================================================================
// Type aliases for common result types
// ============================================================================

export type AIPIndexResult = IndexResult<AIPResult>;
export type RepresentationIndexResult = IndexResult<RepresentationResult>;
export type FileIndexResult = IndexResult<FileResult>;

// ============================================================================
// Preservation Event Types (PREMIS)
// ============================================================================

/** Indexed preservation event from RODA */
export interface PreservationEvent {
    uuid?: string;  // @JsonIgnore in RODA Java — not in API response; extract from id URN
    id: string;
    aipId: string;
    representationUUID?: string;
    fileUUID?: string;
    objectClass?: string;
    eventType: string;
    eventDateTime: string;
    eventDetail: string;
    eventOutcome: string;
    eventOutcomeDetailNote?: string;
    eventOutcomeDetailExtension?: string;
    /** Agent URNs linked to this event (e.g. "urn:roda:premis:agent:admin") */
    linkingAgentIdentifier?: string[];
    /** Outcome object URNs linked to this event */
    linkingOutcomeObjectIdentifier?: string[];
    fields?: Record<string, unknown>;
    createdOn: string;
    updatedOn?: string;
}

/** Preservation agent linked to an event */
export interface PreservationAgent {
    id: string;
    name: string;
    type: string;
    version?: string;
    note?: string;
    roles?: string[];
}

export type PreservationEventIndexResult = IndexResult<PreservationEvent>;

/**
 * Response from GET /preservation/events/{id}/objects.
 * Contains linked files, AIPs, representations, and object references.
 */
export interface PreservationEventObjects {
    aips: Record<string, unknown>;
    representations: Record<string, unknown>;
    files: Record<string, IndexedFile>;
    transferredResources: Record<string, unknown>;
    sourceObjectIds: Array<{ type: string; value: string; roles: string[] }>;
    outcomeObjectIds: Array<{ type: string; value: string; roles: string[] }>;
    uris: unknown[];
}

// ============================================================================
// Transfer Types (Ingest)
// ============================================================================

/** Transferred resource from the ingest area */
export interface TransferredResource {
    uuid: string;
    id: string;
    fullPath: string;
    parentId?: string;
    parentUUID?: string;
    parentPath?: string;
    relativePath: string;
    ancestorsPaths?: string[];
    name: string;
    size: number;
    file: boolean;
    creationDate: string;
    lastScanDate?: string;
    fields?: Record<string, unknown>;
}

export interface TransferResult {
    transferredResource: TransferredResource;
}

export type TransferIndexResult = IndexResult<TransferResult>;

// ============================================================================
// Job Types
// ============================================================================

/** Job state values */
export type JobState =
    | 'CREATED'
    | 'STARTED'
    | 'COMPLETED'
    | 'FAILED_DURING_CREATION'
    | 'FAILED_TO_COMPLETE'
    | 'STOPPED'
    | 'STOPPING'
    | 'TO_BE_CLEANED';

/** Job statistics */
export interface JobStats {
    completionPercentage: number;
    sourceObjectsCount: number;
    sourceObjectsBeingProcessed: number;
    sourceObjectsWaitingToBeProcessed: number;
    sourceObjectsProcessedWithSuccess: number;
    sourceObjectsProcessedWithPartialSuccess: number;
    sourceObjectsProcessedWithFailure: number;
    sourceObjectsProcessedWithSkipped: number;
    outcomeObjectsWithManualIntervention: number;
}

/** Plugin parameter */
export interface PluginParameter {
    id: string;
    name: string;
    type: string;
    defaultValue?: string;
    possibleValues?: string[];
    mandatory: boolean;
    readonly: boolean;
    description?: string;
    value?: string;
}

/** Certificate info for a plugin */
export interface CertificateInfo {
    certificateStatus: 'INTERNAL' | 'VERIFIED' | 'NOT_VERIFIED';
    certificate?: unknown;
}

/** Marketplace metadata for a plugin */
export interface MarketInfo {
    id?: string;
    name?: string;
    type?: string;
    version?: string;
    description?: string;
    installation?: string;
    license?: { name?: string; url?: string };
    vendor?: { name?: string; homepage?: string };
    compatibility?: string[];
}

/** Plugin info from RODA */
export interface PluginInfo {
    id: string;
    name: string;
    version: string;
    description: string;
    type: string;
    categories: string[];
    parameters: PluginParameter[];
    /** Object classes this plugin can operate on */
    objectClasses?: string[];
    /** Whether the plugin is installed in the current RODA instance */
    installed?: boolean;
    /** Whether the plugin is verified/signed */
    verified?: boolean;
    /** Certificate info */
    certificateInfo?: CertificateInfo;
    /** Marketplace metadata */
    marketInfo?: MarketInfo;
    /** Whether a license file exists */
    hasLicenseFile?: boolean;
    /** Whether a documentation file exists */
    hasDocumentationFile?: boolean;
}

/** Indexed job from RODA */
export interface Job {
    uuid: string;
    id: string;
    name: string;
    username: string;
    startDate: string;
    endDate?: string;
    state: JobState;
    stateDetails?: string;
    jobStats: JobStats;
    plugin: string;
    pluginType: string;
    pluginParameters?: Record<string, string>;
    sourceObjects?: {
        selectedClass: string;
        ids?: string[];
    };
    outcomeObjectsClass?: string;
    priority?: string;
    parallelism?: string;
    fields?: Record<string, unknown>;
}

export interface JobResult {
    job: Job;
}

export type JobIndexResult = IndexResult<JobResult>;

/** Job report */
export interface JobReport {
    uuid: string;
    id: string;
    jobId: string;
    sourceObjectId: string;
    sourceObjectClass: string;
    sourceObjectOriginalIds?: string[];
    sourceObjectOriginalName?: string;
    outcomeObjectId?: string;
    outcomeObjectClass?: string;
    outcomeObjectState?: string;
    title?: string;
    dateCreated: string;
    dateUpdated: string;
    completionPercentage: number;
    stepsCompleted: number;
    totalSteps: number;
    plugin: string;
    pluginName: string;
    pluginVersion: string;
    pluginState: string;
    pluginIsMandatory?: boolean;
    pluginDetails?: string;
    htmlPluginDetails: boolean;
    ingestType?: string;
    instanceId?: string;
    transactionId?: string;
    reports?: JobReport[];
    lineSeparator?: string;
    fields?: Record<string, unknown>;
}

export interface JobReportResult {
    jobReport: JobReport;
}

export type JobReportIndexResult = IndexResult<JobReportResult>;

/**
 * Selected items discriminated union for V2 API requests.
 * V2 API uses: @type = "SelectedItemsListRequest" | "SelectedItemsFilterRequest" | etc.
 * (Different from internal SelectedItems which uses type = "list" | "filter" | etc.)
 */
export interface SelectedItemsListRequest {
    '@type': 'SelectedItemsListRequest';
    ids: string[];
}

export interface SelectedItemsFilterRequest {
    '@type': 'SelectedItemsFilterRequest';
    filter: { parameters: FilterParameter[] };
    justActive?: boolean;
}

export interface SelectedItemsAllRequest {
    '@type': 'SelectedItemsAllRequest';
}

export interface SelectedItemsNoneRequest {
    '@type': 'SelectedItemsNoneRequest';
}

export type SelectedItemsRequest =
    | SelectedItemsListRequest
    | SelectedItemsFilterRequest
    | SelectedItemsAllRequest
    | SelectedItemsNoneRequest;

/** Request body for creating a new job */
export interface CreateJobRequest {
    name: string;
    plugin: string;
    pluginParameters?: Record<string, string>;
    sourceObjects: SelectedItemsRequest;
    sourceObjectsClass?: string;
    priority?: string;
    parallelism?: string;
}

/** Wrapper returned by GET /configurations/plugins */
export interface PluginInfoList {
    pluginInfoList: PluginInfo[];
}

// ============================================================================
// Member Types (Users & Groups)
// ============================================================================

/** Indexed member from RODA (can be either user or group) */
export interface IndexedMember {
    uuid: string;
    id: string;
    name: string;
    fullName?: string;
    isUser: boolean;
    isActive: boolean;
    groups?: string[];
    users?: string[];
    email?: string;
    allRoles?: string[];
    directRoles?: string[];
    fields?: Record<string, unknown>;
}

export interface MemberResult {
    member: IndexedMember;
}

export type MemberIndexResult = IndexResult<MemberResult>;

// ============================================================================
// Risk Types
// ============================================================================

/** Risk severity levels */
export type RiskSeverity = 'LOW' | 'MODERATE' | 'HIGH';

/** Indexed risk from RODA */
export interface Risk {
    uuid: string;
    id: string;
    name: string;
    description?: string;
    identifiedOn: string;
    identifiedBy: string;
    categories?: string[];
    notes?: string;
    preMitigationProbability: number;
    preMitigationImpact: number;
    preMitigationSeverity: number;
    preMitigationSeverityLevel?: RiskSeverity;
    preMitigationNotes?: string;
    postMitigationProbability: number;
    postMitigationImpact: number;
    postMitigationSeverity: number;
    postMitigationSeverityLevel?: RiskSeverity;
    postMitigationNotes?: string;
    mitigationStrategy?: string;
    mitigationOwnerType?: string;
    mitigationOwner?: string;
    mitigationRelatedEventIdentifierType?: string;
    mitigationRelatedEventIdentifierValue?: string;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    objectsSize: number;
    fields?: Record<string, unknown>;
}

export interface RiskResult {
    risk: Risk;
}

export type RiskIndexResult = IndexResult<RiskResult>;

/** Risk incidence */
export interface RiskIncidence {
    uuid: string;
    id: string;
    riskId: string;
    aipId: string;
    representationId?: string;
    fileId?: string;
    objectClass?: string;
    status: string;
    severity: string;
    detectedOn: string;
    detectedBy: string;
    mitigatedOn?: string;
    mitigatedBy?: string;
    mitigatedDescription?: string;
    fields?: Record<string, unknown>;
}

export interface RiskIncidenceResult {
    riskIncidence: RiskIncidence;
}

export type IncidenceIndexResult = IndexResult<RiskIncidenceResult>;

// ============================================================================
// Notification Types
// ============================================================================

/** RODA notification */
export interface RodaNotification {
    uuid: string;
    id: string;
    subject: string;
    body: string;
    sentOn: string;
    fromUser: string;
    recipientUsers: string[];
    acknowledged: boolean;
    acknowledgedUsers?: Record<string, string>;
    acknowledgeToken?: string;
    state?: string;
    instanceId?: string;
    instanceName?: string;
    fields?: Record<string, unknown>;
}

export interface NotificationResult {
    notification: RodaNotification;
}

export type NotificationIndexResult = IndexResult<NotificationResult>;

// ============================================================================
// Audit Log Types
// ============================================================================

/** Audit log entry from RODA */
export interface AuditLogEntry {
    uuid: string;
    id: string;
    address: string;
    datetime: string;
    username: string;
    actionComponent: string;
    actionMethod: string;
    relatedObjectID?: string;
    duration: number;
    state: string;
    parameters?: string;
    fields?: Record<string, unknown>;
}

export interface AuditLogResult {
    logEntry: AuditLogEntry;
}

export type AuditLogIndexResult = IndexResult<AuditLogResult>;

// ============================================================================
// Disposal Types
// ============================================================================

/** Disposal schedule */
export interface DisposalSchedule {
    id: string;
    title: string;
    description?: string;
    mandate?: string;
    scopeNotes?: string;
    actionCode?: string;
    retentionTriggerElementId?: string;
    retentionPeriodDuration?: number;
    retentionPeriodIntervalCode?: string;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    firstTimeUsed?: string;
    apiCounter?: number;
    state?: string;
}

/** Wrapper returned by GET /disposal/schedules */
export interface DisposalSchedulesResponse {
    disposalSchedules: DisposalSchedule[];
}

/** Disposal rule */
export interface DisposalRule {
    id: string;
    title: string;
    description?: string;
    type: string;
    conditionKey?: string;
    conditionValue?: string;
    disposalScheduleId?: string;
    disposalScheduleName?: string;
    order?: number;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
}

/** Wrapper returned by GET /disposal/rules */
export interface DisposalRulesResponse {
    disposalRules: DisposalRule[];
}

/** Disposal hold */
export interface DisposalHold {
    id: string;
    title: string;
    description?: string;
    mandate?: string;
    scopeNotes?: string;
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    originatedOn?: string;
    originatedBy?: string;
    firstTimeUsed?: string;
    liftedOn?: string;
    liftedBy?: string;
    aipCounter?: number;
    state?: string;
}

/** Wrapper returned by GET /disposal/holds */
export interface DisposalHoldsResponse {
    disposalHolds: DisposalHold[];
}

/** Disposal confirmation (indexed — from /find) */
export interface DisposalConfirmation {
    uuid: string;
    id: string;
    title: string;
    createdOn: string;
    createdBy: string;
    executedOn?: string;
    executedBy?: string;
    restoredOn?: string;
    restoredBy?: string;
    numberOfAIPs: number;
    size: number;
    state: string;
    fields?: Record<string, unknown>;
}

export interface DisposalConfirmationResult {
    disposalConfirmation: DisposalConfirmation;
}

export type DisposalConfirmationIndexResult = IndexResult<DisposalConfirmationResult>;

// ============================================================================
// Representation Information (Format Registry) Types
// ============================================================================

/** Representation information */
export interface RepresentationInformation {
    uuid: string;
    id: string;
    name: string;
    description?: string;
    family?: string;
    categories?: string[];
    extras?: string;
    support?: string;
    relations?: RepInfoRelation[];
    filters?: string[];
    createdOn: string;
    createdBy: string;
    updatedOn: string;
    updatedBy: string;
    tags?: string[];
    fields?: Record<string, unknown>;
}

export interface RepInfoRelation {
    relationType: string;
    objectType: string;
    link: string;
    title?: string;
}

export interface RepInfoResult {
    representationInformation: RepresentationInformation;
}

export type RepInfoIndexResult = IndexResult<RepInfoResult>;

// ============================================================================
// Distributed Instance / Configuration Types
// ============================================================================

/** Distributed instance (RODA cluster node) */
export interface DistributedInstance {
    id: string;
    name: string;
    description?: string;
    status?: string;
    lastSynchronizationDate?: string;
    createdOn: string;
    createdBy?: string;
    updatedOn?: string;
    updatedBy?: string;
    username?: string;
}

/** Wrapper returned by GET /distributed-instances */
export interface DistributedInstancesResponse {
    distributed_instances: DistributedInstance[];
}

/** Local instance info from GET /distributed-instances/local */
export interface LocalInstanceConfig {
    id?: string;
    name?: string;
    status?: string;
    lastSynchronizationDate?: string;
    createdOn?: string;
    isSubscribed?: boolean;
    centralInstanceURL?: string;
}

// ============================================================================
// Metrics Types
// ============================================================================

/** Wrapper for GET /metrics response */
export interface MetricsResponse {
    metrics: Record<string, string>;
}
