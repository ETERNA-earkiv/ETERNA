import { DigiFormInputSearch } from '@designsystem-se/af-react'
import { useFindRequest } from '../hooks/useFindRequest'
import { DataTable, type Column } from '../components/common/DataTable'
import { Pagination } from '../components/common/Pagination'
import { useTranslation } from '../i18n/TranslationContext'
import type { RODAMember } from '../api/members'

type InputSearchEl = HTMLElement & { afValue: string }

const MemberTypeBadge = ({ member }: { member: RODAMember }) => (
  <span
    style={{
      padding: '0.2rem 0.5rem',
      borderRadius: '2px',
      fontSize: '0.8rem',
      background: member.isUser ? '#e8f0fe' : '#f3e8fd',
      color: member.isUser ? '#1a56db' : '#7b1fa2',
    }}
  >
    {member.isUser ? 'Användare' : 'Grupp'}
  </span>
)

const ActiveBadge = ({ active }: { active: boolean }) => (
  <span
    style={{
      padding: '0.2rem 0.5rem',
      borderRadius: '2px',
      fontSize: '0.8rem',
      background: active ? '#e6f4ea' : '#f5f5f5',
      color: active ? '#2d7a3a' : '#555',
    }}
  >
    {active ? 'Ja' : 'Nej'}
  </span>
)

export function UserManagement() {
  const { t } = useTranslation()

  const { data: members, totalCount, isLoading, error, page, pageSize, setPage, query, setQuery } =
    useFindRequest<RODAMember>({
      endpoint: '/members/find',
      queryKey: 'members',
      onlyActive: false,
    })

  const totalPages = Math.ceil(totalCount / pageSize)

  const columns: Column<RODAMember>[] = [
    {
      key: 'name',
      header: t('label.name', 'Namn'),
      render: (m) => <strong>{m.name}</strong>,
    },
    {
      key: 'fullName',
      header: t('user.fullname', 'Fullständigt namn'),
      render: (m) => m.fullName || '—',
    },
    {
      key: 'email',
      header: t('label.email', 'E-post'),
      render: (m) => m.email || '—',
    },
    {
      key: 'type',
      header: t('label.type', 'Typ'),
      render: (m) => <MemberTypeBadge member={m} />,
      width: '100px',
    },
    {
      key: 'active',
      header: t('label.active', 'Aktiv'),
      render: (m) => <ActiveBadge active={m.active} />,
      width: '80px',
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>{t('user.title', 'Användarhantering')}</h1>

      <div style={{ marginBottom: '1.5rem' }}>
        <DigiFormInputSearch
          afLabel={t('user.search.label', 'Sök användare')}
          afId="search-members"
          afValue={query}
          onAfOnInput={(e) => setQuery((e.target as InputSearchEl).afValue)}
          onAfOnSubmitSearch={(e) => setQuery(e.detail as string || query)}
        />
      </div>

      <DataTable
        columns={columns}
        rows={members}
        rowKey={(m) => m.id}
        isLoading={isLoading}
        error={error?.message ?? null}
        emptyMessage={t('user.none', 'Inga användare hittades')}
        summary={!isLoading && !error ? `Visar ${members.length} av ${totalCount} poster` : undefined}
      />

      <Pagination
        page={page}
        totalPages={totalPages}
        onPageChange={setPage}
        resetKey={query}
      />
    </div>
  )
}
