import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

/**
 * Format a timestamp for display across all pages.
 * @param {number|string} ts — Unix timestamp in milliseconds
 * @returns {string} Formatted date string like "2026-06-15 14:30", or "—" if falsy
 */
export function formatDate(ts) {
  if (!ts) return '—'
  return dayjs(Number(ts)).format('YYYY-MM-DD HH:mm')
}

/**
 * Format a timestamp as relative time (Jira-style).
 * @param {number|string} ts — Unix timestamp in milliseconds
 * @returns {string} e.g. "2 hours ago", "yesterday", "Jun 15" if > 7 days
 */
export function formatRelative(ts) {
  if (!ts) return '—'
  const d = dayjs(Number(ts))
  const now = dayjs()
  const diffDays = now.diff(d, 'day')
  if (diffDays < 1) return d.fromNow()          // "2 hours ago", "5 minutes ago"
  if (diffDays === 1) return 'yesterday at ' + d.format('HH:mm')
  if (diffDays < 7) return d.format('ddd [at] HH:mm')  // "Mon at 14:30"
  return d.format('MMM D, YYYY')                 // "Jun 15, 2026"
}

/**
 * Format a timestamp as full date+time on hover tooltip.
 * @param {number|string} ts — Unix timestamp in milliseconds
 * @returns {string} e.g. "Jun 16, 2026, 3:42 PM"
 */
export function formatDateTime(ts) {
  if (!ts) return '—'
  return dayjs(Number(ts)).format('MMM D, YYYY, h:mm A')
}
