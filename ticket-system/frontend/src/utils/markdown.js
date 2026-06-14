import { marked } from 'marked'

// Configure marked for safe rendering
marked.setOptions({
  breaks: true,       // single line breaks → <br>
  gfm: true,          // GitHub Flavored Markdown (tables, task lists, strikethrough)
  mangle: false,      // don't obfuscate email addresses
  headerIds: false    // don't add id attributes to headings
})

// Strip dangerous HTML tags/attributes after markdown rendering
function sanitize(html) {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<iframe[\s\S]*?<\/iframe>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')   // onclick, onload, etc.
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
    .replace(/<a /gi, '<a rel="noopener noreferrer" target="_blank" ')
}

export function renderMarkdown(text) {
  if (!text) return ''
  try {
    return sanitize(marked.parse(text))
  } catch {
    return text // fallback to plain text on parse error
  }
}
