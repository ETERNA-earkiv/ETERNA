/**
 * Visibility store — XPath-baserade synlighetsregler och cache.
 */

import { map, computed } from 'nanostores';
import { $visibilityConfig } from './config';
import type { SimpleFilterParameter, FilterParameter } from '../types/api';
import type { XpathRule } from '../api/config';

export type VisibilityLevel = 'full' | 'title-only' | 'hidden';

// Cache: aipId → visibility level
export const $xpathCache = map<Record<string, VisibilityLevel>>({});

export const $allowedLevels = computed($visibilityConfig, (c) => c?.allowedLevels ?? []);
export const $xpathRules = computed($visibilityConfig, (c) => c?.xpathRules ?? []);
export const $xpathRulesEnabled = computed($visibilityConfig, (c) => c?.xpathRulesEnabled ?? false);
export const $hasLevelFilter = computed($allowedLevels, (levels) => levels.length > 0);

/**
 * Build a filter parameter that restricts results to allowed description levels.
 */
export function buildLevelFilter(): FilterParameter | null {
  const levels = $allowedLevels.get();
  if (levels.length === 0) return null;

  if (levels.length === 1) {
    return {
      type: 'SimpleFilterParameter',
      name: 'level',
      value: levels[0],
    } as SimpleFilterParameter;
  }

  return {
    type: 'OrFiltersParameters',
    operands: levels.map((level) => ({
      type: 'SimpleFilterParameter',
      name: 'level',
      value: level,
    })),
  } as FilterParameter;
}

/**
 * Find the first XPath rule matching any of the given metadata IDs.
 */
export function findMatchingRule(metadataIds: string[]): XpathRule | null {
  const rules = $xpathRules.get();
  for (const rule of rules) {
    const normalizedRuleId = rule.metadataId.replace(/[-_]/g, '').toLowerCase();
    for (const id of metadataIds) {
      const normalizedId = id.replace(/[-_]/g, '').toLowerCase();
      if (normalizedId === normalizedRuleId || normalizedId.startsWith(normalizedRuleId)) {
        return rule;
      }
    }
  }
  return null;
}

/**
 * Determine visibility based on an XPath value and a rule.
 */
export function resolveVisibility(xpathValue: string | null, rule: XpathRule): VisibilityLevel {
  if (!xpathValue) return 'full';

  const value = xpathValue.trim().toLowerCase();

  if (rule.hiddenValues?.some((v) => v.toLowerCase() === value)) return 'hidden';
  if (rule.titleOnlyValues?.some((v) => v.toLowerCase() === value)) return 'title-only';
  if (rule.fullAccessValues?.some((v) => v.toLowerCase() === value)) return 'full';

  return 'full';
}

export function getCachedVisibility(aipId: string): VisibilityLevel | undefined {
  return $xpathCache.get()[aipId];
}

export function cacheVisibility(aipId: string, level: VisibilityLevel): void {
  $xpathCache.setKey(aipId, level);
}

export function clearVisibilityCache(): void {
  $xpathCache.set({});
}
