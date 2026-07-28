import hljs from 'highlight.js/lib/common'
import { LANGUAGES } from './languages'

// highlight.js 언어명이 우리 LANGUAGES 값과 그대로 일치하므로 후보로 바로 사용.
const SUBSET = LANGUAGES as unknown as string[]

// 붙여넣은 코드의 언어를 추정. 확신이 없으면 null(그대로 두기).
// - highlightAuto: 후보 언어들로 파싱해보고 relevance(그럴듯함 점수)가 가장 높은 언어를 반환
// - 너무 짧거나 점수가 낮으면 오탐 방지를 위해 포기
export function detectLanguage(code: string): string | null {
  const trimmed = code.trim()
  if (trimmed.length < 20) return null

  const result = hljs.highlightAuto(trimmed, SUBSET)
  const lang = result.language
  if (!lang || !SUBSET.includes(lang)) return null
  if (result.relevance < 5) return null // 신뢰도 낮으면 무시

  return lang
}
