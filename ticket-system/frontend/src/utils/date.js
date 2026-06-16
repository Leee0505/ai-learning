import dayjs from 'dayjs'

/**
 * Format a timestamp for display across all pages.
 * @param {number|string} ts — Unix timestamp in milliseconds
 * @returns {string} Formatted date string like "2026-06-15 14:30", or "—" if falsy
 */
export function formatDate(ts) {
  if (!ts) return '—'
  return dayjs(Number(ts)).format('YYYY-MM-DD HH:mm')
}
