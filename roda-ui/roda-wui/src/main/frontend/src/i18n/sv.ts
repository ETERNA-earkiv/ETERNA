// Swedish (default) translations
// Keys mirror GWT ClientMessages.properties where applicable
export const sv: Record<string, string> = {
  // Navigation
  'nav.browse': 'Arkivobjekt',
  'nav.ingest': 'Ingest',
  'nav.jobs': 'Jobb',
  'nav.audit': 'Logg',
  'nav.users': 'Användare',
  'nav.logout': 'Logga ut',

  // Common actions
  'action.search': 'Sök',
  'action.create': 'Skapa',
  'action.edit': 'Redigera',
  'action.delete': 'Ta bort',
  'action.cancel': 'Avbryt',
  'action.confirm': 'Bekräfta',
  'action.save': 'Spara',
  'action.back': 'Tillbaka',
  'action.download': 'Ladda ner',

  // Common labels
  'label.all': 'Alla',
  'label.title': 'Titel',
  'label.state': 'Status',
  'label.level': 'Nivå',
  'label.created': 'Skapad',
  'label.modified': 'Ändrad',
  'label.id': 'ID',
  'label.size': 'Storlek',
  'label.format': 'Format',
  'label.type': 'Typ',
  'label.name': 'Namn',
  'label.description': 'Beskrivning',
  'label.username': 'Användarnamn',
  'label.email': 'E-post',
  'label.active': 'Aktiv',

  // Facet labels
  'facet.state': 'Tillstånd',
  'facet.level': 'Nivå',
  'facet.type': 'Typ',
  'facet.plugin': 'Plugin',

  // AIP browse
  'aip.title': 'Arkivobjekt',
  'aip.search.label': 'Sök arkivobjekt',
  'aip.showing': 'Visar {0} av {1} arkivobjekt',
  'aip.none': 'Inga arkivobjekt hittades',
  'aip.error': 'Kunde inte hämta arkivobjekt.',

  // AIP detail
  'aip.detail.representations': 'Representationer',
  'aip.detail.metadata': 'Metadata',
  'aip.detail.events': 'Händelser',
  'aip.detail.risks': 'Risker',
  'aip.detail.original': 'Original',
  'aip.detail.files': 'filer',

  // Jobs
  'job.title': 'Jobb',
  'job.none': 'Inga jobb hittades',
  'job.error': 'Kunde inte hämta jobb.',
  'job.start': 'Startad',
  'job.end': 'Avslutad',
  'job.user': 'Användare',
  'job.plugin': 'Plugin',
  'job.progress': 'Framsteg',
  'job.stop': 'Stoppa',
  'job.objects': '{0}/{1} objekt ({2} misslyckade)',

  // Audit log
  'audit.title': 'Logg',
  'audit.none': 'Inga loggposter hittades',
  'audit.error': 'Kunde inte hämta loggen.',
  'audit.search.label': 'Sök i logg',
  'audit.component': 'Komponent',
  'audit.method': 'Metod',
  'audit.object': 'Objekt',
  'audit.duration': 'Varaktighet',

  // User management
  'user.title': 'Användare',
  'user.none': 'Inga användare hittades',
  'user.error': 'Kunde inte hämta användare.',
  'user.search.label': 'Sök användare',
  'user.fullname': 'Namn',
  'user.groups': 'Grupper',

  // Ingest
  'ingest.title': 'Ingest',
  'ingest.upload': 'Välj SIP-fil',
  'ingest.drop': 'eller dra och släpp filen här',
  'ingest.start': 'Starta ingest',
  'ingest.uploading': 'Laddar upp...',
  'ingest.success': 'Ingest-jobb startat',
  'ingest.error.upload': 'Uppladdningen misslyckades.',
  'ingest.error.job': 'Kunde inte starta ingest-jobbet.',

  // Login
  'login.title': 'Logga in',
  'login.username': 'Användarnamn',
  'login.password': 'Lösenord',
  'login.submit': 'Logga in',
  'login.error': 'Felaktigt användarnamn eller lösenord.',

  // Errors
  'error.generic': 'Ett fel uppstod. Försök igen.',
  'error.notfound': 'Resursen hittades inte.',
  'error.forbidden': 'Du har inte behörighet till denna resurs.',
  'error.loading': 'Kunde inte ladda data.',
}
