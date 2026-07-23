import { useState } from 'react'
import { Sparkles } from 'lucide-react'
import { aiApi, type AiTask } from '../../api/ai'
import { apiErrorCode, apiErrorMessage } from '../../lib/apiError'
import Markdown from './Markdown'

// 풀이 코드를 Claude 로 분석(최적화/설명). 하루 한도 초과 시 429 안내.
export default function AiAnalyzePanel({ code, language, problemTitle }: { code: string; language?: string; problemTitle?: string }) {
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [task, setTask] = useState<AiTask>('EXPLAIN')

  async function run(t: AiTask) {
    setTask(t)
    setLoading(true)
    setError('')
    setResult('')
    try {
      const md = await aiApi.analyze({ task: t, code, language, problemTitle })
      setResult(md)
    } catch (err) {
      const codeStr = apiErrorCode(err)
      if (codeStr === 'AI_RATE_LIMIT_EXCEEDED') {
        setError('오늘 AI 분석 한도를 모두 사용했습니다. 내일 다시 시도해 주세요.')
      } else if (codeStr === 'AI_UNAVAILABLE') {
        setError('AI 기능을 현재 사용할 수 없습니다. (서버에 API 키가 설정되지 않았을 수 있어요.)')
      } else {
        setError(apiErrorMessage(err, 'AI 분석에 실패했습니다.'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="rounded-lg border border-violet-800/40 bg-violet-950/20 p-4">
      <div className="flex items-center gap-2">
        <Sparkles size={15} className="text-violet-300" />
        <span className="text-sm font-semibold text-violet-300">AI 분석</span>
        <div className="ml-auto flex gap-2">
          <button onClick={() => run('EXPLAIN')} disabled={loading} className="rounded-lg border border-violet-700 bg-violet-500/10 px-3 py-1.5 text-xs text-violet-200 hover:bg-violet-500/20 disabled:opacity-50">
            풀이 설명
          </button>
          <button onClick={() => run('OPTIMIZE')} disabled={loading} className="rounded-lg border border-violet-700 bg-violet-500/10 px-3 py-1.5 text-xs text-violet-200 hover:bg-violet-500/20 disabled:opacity-50">
            최적화 제안
          </button>
        </div>
      </div>
      <p className="mt-1 text-[11px] text-slate-500">하루 2회 제한 · {task === 'EXPLAIN' ? '설명' : '최적화'}</p>

      {loading && <p className="mt-3 text-sm text-slate-400">분석 중… (수 초 소요)</p>}
      {error && <p className="mt-3 text-sm text-rose-400">{error}</p>}
      {result && (
        <div className="mt-3 rounded-lg border border-slate-800 bg-slate-950/40 p-3">
          <Markdown>{result}</Markdown>
        </div>
      )}
    </div>
  )
}
