import { useState } from 'react'
import { Lightbulb } from 'lucide-react'
import { aiApi, pollAiJob } from '../../api/ai'
import { apiErrorCode, apiErrorMessage } from '../../lib/apiError'
import Markdown from './Markdown'

// 문제가 안 풀릴 때, 정답 코드 없이 단계별 힌트를 받는 패널. 하루 한도 공유.
export default function AiHintPanel({ problemId }: { problemId: number }) {
  const [partialCode, setPartialCode] = useState('')
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function run() {
    setLoading(true)
    setError('')
    setResult('')
    try {
      const created = await aiApi.hint({ problemId, partialCode: partialCode.trim() || undefined })
      const job = await pollAiJob(created.id) // 컨슈머 처리 완료까지 폴링
      if (job.status === 'DONE') {
        setResult(job.result ?? '')
      } else {
        setError(job.errorCode === 'AI_UNAVAILABLE' ? 'AI 기능을 현재 사용할 수 없습니다.' : 'AI 힌트에 실패했습니다.')
      }
    } catch (err) {
      if (err instanceof Error && err.message === 'TIMEOUT') {
        setError('처리가 지연되고 있어요. 잠시 후 다시 시도해 주세요.')
        return
      }
      const c = apiErrorCode(err)
      if (c === 'AI_RATE_LIMIT_EXCEEDED') {
        setError('오늘 AI 사용 한도를 모두 사용했습니다. 내일 다시 시도해 주세요.')
      } else if (c === 'AI_UNAVAILABLE') {
        setError('AI 기능을 현재 사용할 수 없습니다. (서버에 API 키가 없을 수 있어요.)')
      } else {
        setError(apiErrorMessage(err, 'AI 힌트에 실패했습니다.'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="rounded-lg border border-violet-800/40 bg-violet-950/20 p-4">
      <div className="flex flex-wrap items-center gap-2">
        <Lightbulb size={15} className="text-violet-300" />
        <span className="text-sm font-semibold text-violet-300">AI 힌트</span>
        <span className="text-[11px] text-slate-500">정답 코드 없이 단계별로 · 하루 2회</span>
        <button
          onClick={run}
          disabled={loading}
          className="ml-auto rounded-lg border border-violet-700 bg-violet-500/10 px-3 py-1.5 text-xs text-violet-200 transition hover:bg-violet-500/20 disabled:opacity-50"
        >
          {loading ? '생각 중…' : '힌트 받기'}
        </button>
      </div>

      <details className="mt-3">
        <summary className="cursor-pointer text-xs text-slate-500">막힌 코드 첨부 (선택) — 있으면 더 맞춤 힌트</summary>
        <textarea
          value={partialCode}
          onChange={(e) => setPartialCode(e.target.value)}
          rows={4}
          placeholder="지금까지 시도한 코드를 붙여넣으세요"
          className="input-field mt-2 resize-y"
        />
      </details>

      {loading && <p className="mt-3 text-sm text-slate-400">힌트 생성 중… (수 초 소요)</p>}
      {error && <p className="mt-3 text-sm text-rose-400">{error}</p>}
      {result && (
        <div className="mt-3 rounded-lg border border-slate-800 bg-slate-950/40 p-3">
          <Markdown>{result}</Markdown>
        </div>
      )}
    </div>
  )
}
