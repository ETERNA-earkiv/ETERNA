/** RODA user and group types. */

export interface RodaUser {
    uuid: string;
    id: string;
    name: string;
    fullName?: string;
    email: string;
    active: boolean;
    allRoles: string[];
    directRoles: string[];
    groups: string[];
    extra?: Array<{ id: string; options: Record<string, string> }>;
}

export interface RodaGroup {
    uuid: string;
    id: string;
    name: string;
    fullName?: string;
    active: boolean;
    allRoles: string[];
    directRoles: string[];
    users: string[];
}

/** Access key returned by RODA V2 API. Secret is only present on create/regenerate (shown once). */
export interface AccessKey {
    id: string;
    name?: string;
    key?: string;
    secret?: string;
    createdOn?: string;
    lastUsed?: string;
    expiresOn?: string;
    active?: boolean;
    status?: string;
    [extra: string]: unknown;
}

/** Request body for creating an access key. */
export interface CreateAccessKeyRequest {
    name?: string;
    expiresOn?: string;
}

/** A single permission role with metadata for display. */
export interface RoleDefinition {
    role: string;
    label: string;
    category: string;
}

/**
 * All known RODA permission roles grouped by category, with Swedish labels.
 * Order matches the RODA admin UI reference screenshot.
 */
export const RODA_ROLE_DEFINITIONS: RoleDefinition[] = [
    // Logiska enheter (AIP)
    { role: 'aip.read', label: 'Hämta logiska enheter (AIP)', category: 'Logiska enheter (AIP)' },
    { role: 'aip.view', label: 'Lista och sök efter logiska enheter (AIP)', category: 'Logiska enheter (AIP)' },
    { role: 'aip.appraisal', label: 'Acceptera eller avvisa i ankomstkontroll', category: 'Logiska enheter (AIP)' },
    { role: 'aip.create.top', label: 'Skapa logiska toppenheter', category: 'Logiska enheter (AIP)' },
    { role: 'aip.create', label: 'Skapa logiska enheter', category: 'Logiska enheter (AIP)' },
    { role: 'aip.create.below', label: 'Skapa logiska enheter under befintliga', category: 'Logiska enheter (AIP)' },
    { role: 'aip.update', label: 'Uppdatera logiska enheter', category: 'Logiska enheter (AIP)' },
    { role: 'aip.delete', label: 'Ta bort logiska enheter', category: 'Logiska enheter (AIP)' },

    // Representationer och datorfiler
    { role: 'representation.view', label: 'Lista och sök i representationer och datorfiler', category: 'Representationer' },
    { role: 'representation.read', label: 'Hämta representationer och datorfiler', category: 'Representationer' },
    { role: 'representation.create', label: 'Skapa representationer och datorfiler', category: 'Representationer' },
    { role: 'representation.update', label: 'Uppdatera representationer och datorfiler', category: 'Representationer' },
    { role: 'representation.delete', label: 'Ta bort representationer och datorfiler', category: 'Representationer' },

    // Beskrivande metadata
    { role: 'descriptive_metadata.read', label: 'Lista och hämta beskrivande metadata', category: 'Beskrivande metadata' },
    { role: 'descriptive_metadata.create', label: 'Skapa beskrivande metadata', category: 'Beskrivande metadata' },
    { role: 'descriptive_metadata.update', label: 'Uppdatera beskrivande metadata', category: 'Beskrivande metadata' },
    { role: 'descriptive_metadata.delete', label: 'Ta bort beskrivande metadata', category: 'Beskrivande metadata' },

    // Bevarandemetadata
    { role: 'preservation_metadata.read', label: 'Lista och hämta bevarandemetadata', category: 'Bevarandemetadata' },
    { role: 'preservation_metadata.create', label: 'Skapa bevarandemetadata', category: 'Bevarandemetadata' },
    { role: 'preservation_metadata.delete', label: 'Ta bort bevarandemetadata', category: 'Bevarandemetadata' },

    // Inleverans
    { role: 'transfer.read', label: 'Lista och hämta filer i inleverans', category: 'Inleverans' },
    { role: 'transfer.create', label: 'Skapa filer under inleverans', category: 'Inleverans' },
    { role: 'transfer.update', label: 'Uppdatera filer under inleverans', category: 'Inleverans' },
    { role: 'transfer.delete', label: 'Ta bort filer från inleverans', category: 'Inleverans' },

    // Processer (jobb)
    { role: 'job.read', label: 'Lista och visa processer', category: 'Processer' },
    { role: 'job.manage', label: 'Hantera processer', category: 'Processer' },

    // Användare och grupper
    { role: 'member.read', label: 'Lista och visa användare och grupper', category: 'Användare & grupper' },
    { role: 'member.manage', label: 'Hantera användare och grupper', category: 'Användare & grupper' },
    { role: 'permission.read', label: 'Visa behörigheter', category: 'Användare & grupper' },

    // Aviseringar
    { role: 'notification.read', label: 'Lista och visa aviseringar', category: 'Aviseringar' },
    { role: 'notification.manage', label: 'Hantera aviseringar', category: 'Aviseringar' },

    // Loggposter
    { role: 'log_entry.read', label: 'Lista och visa loggposter', category: 'Loggposter' },
    { role: 'log_entry.create', label: 'Skapa loggposter', category: 'Loggposter' },
    { role: 'log_entry.delete', label: 'Ta bort loggposter', category: 'Loggposter' },

    // Risker
    { role: 'risk.read', label: 'Lista och visa risker', category: 'Risker' },
    { role: 'risk.manage', label: 'Hantera risker', category: 'Risker' },

    // Representationsinformation (format)
    { role: 'ri.read', label: 'Lista och visa representationsinformation', category: 'Representationsinformation' },
    { role: 'ri.manage', label: 'Hantera representationsinformation', category: 'Representationsinformation' },

    // Gallring (disposal)
    { role: 'disposal_schedule.read', label: 'Visa gallringsscheman', category: 'Gallring' },
    { role: 'disposal_schedule.manage', label: 'Hantera gallringsscheman', category: 'Gallring' },
    { role: 'disposal_schedule.associate', label: 'Koppla gallringsscheman', category: 'Gallring' },
    { role: 'disposal_rule.read', label: 'Visa gallringsregler', category: 'Gallring' },
    { role: 'disposal_rule.manage', label: 'Hantera gallringsregler', category: 'Gallring' },
    { role: 'disposal_hold.read', label: 'Visa gallringsspärrar', category: 'Gallring' },
    { role: 'disposal_hold.manage', label: 'Hantera gallringsspärrar', category: 'Gallring' },
    { role: 'disposal_hold.apply', label: 'Tillämpa gallringsspärrar', category: 'Gallring' },
    { role: 'disposal_confirmation.read', label: 'Visa gallringsbekräftelser', category: 'Gallring' },
    { role: 'disposal_confirmation.manage', label: 'Hantera gallringsbekräftelser', category: 'Gallring' },
    { role: 'disposal_confirmation.restore', label: 'Återställa efter gallring', category: 'Gallring' },
    { role: 'disposal_confirmation.destroy', label: 'Verkställa gallring', category: 'Gallring' },
    { role: 'disposal_confirmation.delete_bin', label: 'Tömma gallringskorg', category: 'Gallring' },

    // Åtkomstnycklar
    { role: 'access_key.read', label: 'Visa åtkomstnycklar', category: 'Åtkomstnycklar' },
    { role: 'access_key.manage', label: 'Hantera åtkomstnycklar', category: 'Åtkomstnycklar' },

    // Konfiguration
    { role: 'local_instance_configuration.read', label: 'Visa instans-konfiguration', category: 'Konfiguration' },
    { role: 'local_instance_configuration.manage', label: 'Hantera instans-konfiguration', category: 'Konfiguration' },

    // Distribuerade instanser
    { role: 'distributed_instances.read', label: 'Visa distribuerade instanser', category: 'Distribuerade instanser' },
    { role: 'distributed_instances.manage', label: 'Hantera distribuerade instanser', category: 'Distribuerade instanser' },
];

/** Quick-access role ID constants. */
export const RODA_ROLES = {
    AIP_READ: 'aip.read',
    AIP_VIEW: 'aip.view',
    AIP_CREATE: 'aip.create',
    AIP_CREATE_TOP: 'aip.create.top',
    AIP_CREATE_BELOW: 'aip.create.below',
    AIP_UPDATE: 'aip.update',
    AIP_DELETE: 'aip.delete',
    AIP_APPRAISAL: 'aip.appraisal',
    REPRESENTATION_VIEW: 'representation.view',
    REPRESENTATION_CREATE: 'representation.create',
    REPRESENTATION_READ: 'representation.read',
    REPRESENTATION_UPDATE: 'representation.update',
    REPRESENTATION_DELETE: 'representation.delete',
    DESCRIPTIVE_METADATA_CREATE: 'descriptive_metadata.create',
    DESCRIPTIVE_METADATA_READ: 'descriptive_metadata.read',
    DESCRIPTIVE_METADATA_UPDATE: 'descriptive_metadata.update',
    DESCRIPTIVE_METADATA_DELETE: 'descriptive_metadata.delete',
    PRESERVATION_METADATA_READ: 'preservation_metadata.read',
    PRESERVATION_METADATA_CREATE: 'preservation_metadata.create',
    PRESERVATION_METADATA_DELETE: 'preservation_metadata.delete',
    TRANSFER_CREATE: 'transfer.create',
    TRANSFER_READ: 'transfer.read',
    TRANSFER_UPDATE: 'transfer.update',
    TRANSFER_DELETE: 'transfer.delete',
    JOB_MANAGE: 'job.manage',
    JOB_READ: 'job.read',
    MEMBER_MANAGE: 'member.manage',
    MEMBER_READ: 'member.read',
    PERMISSION_READ: 'permission.read',
    NOTIFICATION_MANAGE: 'notification.manage',
    NOTIFICATION_READ: 'notification.read',
    LOG_ENTRY_READ: 'log_entry.read',
    LOG_ENTRY_CREATE: 'log_entry.create',
    LOG_ENTRY_DELETE: 'log_entry.delete',
    RISK_MANAGE: 'risk.manage',
    RISK_READ: 'risk.read',
    RI_MANAGE: 'ri.manage',
    RI_READ: 'ri.read',
    DISPOSAL_SCHEDULE_READ: 'disposal_schedule.read',
    DISPOSAL_SCHEDULE_MANAGE: 'disposal_schedule.manage',
    ACCESS_KEY_READ: 'access_key.read',
    ACCESS_KEY_MANAGE: 'access_key.manage',
    LOCAL_INSTANCE_CONFIGURATION_READ: 'local_instance_configuration.read',
    LOCAL_INSTANCE_CONFIGURATION_MANAGE: 'local_instance_configuration.manage',
    DISTRIBUTED_INSTANCES_MANAGE: 'distributed_instances.manage',
    DISTRIBUTED_INSTANCES_READ: 'distributed_instances.read',
} as const;

export type RodaRole = typeof RODA_ROLES[keyof typeof RODA_ROLES];
