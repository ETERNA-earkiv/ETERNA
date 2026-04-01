import { useState } from "react";
import type { AstroUser } from "@/lib/store/userStore";

interface NavItem {
  key: string;
  label: string;
  href?: string;
  children?: NavItem[];
  roles?: string[];
}

const NAV_ITEMS: NavItem[] = [
  {
    key: "browse",
    label: "Browse",
    href: "/browse",
    roles: ["dissemination.browse"],
  },
  {
    key: "search",
    label: "Search",
    href: "/search",
    roles: ["dissemination.metadata.read"],
  },
  {
    key: "ingest",
    label: "Ingest",
    children: [
      { key: "ingest_pre", label: "Pre-ingest", href: "/ingest/pre", roles: ["ingest.pre.ingest"] },
      { key: "ingest_transfer", label: "Transfer", href: "/ingest/transfer", roles: ["ingest.submission.upload"] },
      { key: "ingest_process", label: "Process", href: "/ingest/process", roles: ["ingest.list"] },
      { key: "ingest_appraisal", label: "Appraisal", href: "/ingest/appraisal", roles: ["ingest.appraisal.access"] },
    ],
    roles: ["ingest.pre.ingest", "ingest.submission.upload", "ingest.list", "ingest.appraisal.access"],
  },
  {
    key: "disposal",
    label: "Disposal",
    children: [
      { key: "disposal_policy", label: "Policy", href: "/disposal/policy", roles: ["disposal.policy.read"] },
      { key: "disposal_confirmations", label: "Confirmations", href: "/disposal/confirmations", roles: ["disposal.confirmation.read"] },
      { key: "disposal_destroyed", label: "Destroyed records", href: "/disposal/destroyed", roles: ["disposal.confirmation.read"] },
    ],
    roles: ["disposal.policy.read", "disposal.confirmation.read"],
  },
  {
    key: "planning",
    label: "Planning",
    children: [
      { key: "planning_representation_information", label: "Representation information", href: "/planning/representation-information", roles: ["representation.information.manage"] },
      { key: "planning_risk", label: "Risk register", href: "/planning/risks", roles: ["risk.management.read"] },
      { key: "planning_event", label: "Preservation events", href: "/planning/events", roles: ["preservation.events.read"] },
      { key: "planning_agent", label: "Preservation agents", href: "/planning/agents", roles: ["preservation.events.read"] },
    ],
    roles: ["representation.information.manage", "risk.management.read", "preservation.events.read"],
  },
  {
    key: "administration",
    label: "Administration",
    children: [
      { key: "administration_actions", label: "Actions", href: "/management/actions", roles: ["process.run"] },
      { key: "administration_internal_actions", label: "Internal actions", href: "/management/internal", roles: ["process.run.internal"] },
      { key: "administration_user", label: "Users and groups", href: "/management/members", roles: ["user.management.read"] },
      { key: "administration_log", label: "Audit logs", href: "/management/log", roles: ["log.view"] },
      { key: "administration_notifications", label: "Notifications", href: "/management/notifications", roles: ["notification.view"] },
      { key: "administration_statistics", label: "Statistics", href: "/management/statistics", roles: ["administration.reporting.access"] },
    ],
    roles: ["process.run", "user.management.read", "log.view", "notification.view", "administration.reporting.access"],
  },
  {
    key: "help",
    label: "Help",
    href: "/help",
  },
];

function hasRole(userRoles: string[], required?: string[]): boolean {
  if (!required || required.length === 0) return true;
  return required.some((r) => userRoles.includes(r));
}

interface DropdownProps {
  item: NavItem;
  userRoles: string[];
  currentPath: string;
}

function Dropdown({ item, userRoles, currentPath }: DropdownProps) {
  const [open, setOpen] = useState(false);

  const visibleChildren = (item.children ?? []).filter((c) =>
    hasRole(userRoles, c.roles),
  );
  if (visibleChildren.length === 0) return null;

  return (
    <div className="relative" onMouseLeave={() => setOpen(false)}>
      <button
        className="px-3 py-2 text-sm font-medium text-white hover:bg-white/10 rounded flex items-center gap-1"
        onMouseEnter={() => setOpen(true)}
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        {item.label}
        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && (
        <div className="absolute left-0 top-full mt-1 w-52 bg-white shadow-lg rounded border border-gray-200 z-50">
          {visibleChildren.map((child) => (
            <a
              key={child.key}
              href={child.href}
              className={`block px-4 py-2 text-sm hover:bg-gray-50 ${
                currentPath.startsWith(child.href ?? "") ? "bg-blue-50 text-blue-700 font-medium" : "text-gray-700"
              }`}
            >
              {child.label}
            </a>
          ))}
        </div>
      )}
    </div>
  );
}

interface HeaderProps {
  user: AstroUser | null;
  currentPath: string;
}

export default function Header({ user, currentPath }: HeaderProps) {
  const userRoles = user?.roles ?? [];

  return (
    <header
      className="bg-[var(--color-primary)] text-white shadow-md sticky top-0 z-40"
      style={{ height: "var(--header-height)" }}
    >
      <div className="max-w-[1400px] mx-auto px-4 h-full flex items-center gap-4">
        {/* Logo / Home */}
        <a href="/" className="flex items-center gap-2 shrink-0 font-bold text-lg text-white hover:no-underline">
          <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 7a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 3v4M8 3v4M3 11h18" />
          </svg>
          ETERNA
        </a>

        {/* Navigation */}
        <nav className="flex items-center gap-1 flex-1">
          {NAV_ITEMS.map((item) => {
            if (!hasRole(userRoles, item.roles)) return null;
            if (item.children) {
              return (
                <Dropdown key={item.key} item={item} userRoles={userRoles} currentPath={currentPath} />
              );
            }
            return (
              <a
                key={item.key}
                href={item.href}
                className={`px-3 py-2 text-sm font-medium rounded ${
                  currentPath.startsWith(item.href ?? "") && item.href !== "/"
                    ? "bg-white/20"
                    : "hover:bg-white/10"
                } text-white`}
              >
                {item.label}
              </a>
            );
          })}
        </nav>

        {/* User menu */}
        <div className="flex items-center gap-3 shrink-0">
          {user && !user.isGuest ? (
            <UserMenu user={user} />
          ) : (
            <a href="/login" className="text-sm text-white/80 hover:text-white">
              Sign in
            </a>
          )}
        </div>
      </div>
    </header>
  );
}

function UserMenu({ user }: { user: AstroUser }) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative" onMouseLeave={() => setOpen(false)}>
      <button
        className="flex items-center gap-2 text-sm text-white hover:text-white/80"
        onClick={() => setOpen((v) => !v)}
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
        <span>{user.name}</span>
      </button>
      {open && (
        <div className="absolute right-0 top-full mt-1 w-44 bg-white shadow-lg rounded border border-gray-200 z-50">
          <a href="/auth/profile" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
            Profile
          </a>
          <hr className="my-1 border-gray-100" />
          <a href="/api/v2/members/users/logout" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
            Sign out
          </a>
        </div>
      )}
    </div>
  );
}
