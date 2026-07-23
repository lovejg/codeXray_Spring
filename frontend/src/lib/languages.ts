// 코드 언어 선택지 (백엔드는 자유 문자열, 기본 python)
export const LANGUAGES = [
  'python',
  'java',
  'cpp',
  'c',
  'javascript',
  'typescript',
  'go',
  'kotlin',
  'swift',
  'rust',
  'sql',
] as const

export const LANGUAGE_LABEL: Record<string, string> = {
  python: 'Python',
  java: 'Java',
  cpp: 'C++',
  c: 'C',
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  go: 'Go',
  kotlin: 'Kotlin',
  swift: 'Swift',
  rust: 'Rust',
  sql: 'SQL',
}

export function languageLabel(lang: string): string {
  return LANGUAGE_LABEL[lang] ?? lang
}
