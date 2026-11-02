import { Sparkles } from 'lucide-react'
import { aiApi, type AiTask } from '../../api/ai'
import { useAiJob } from '../../hooks/useAiJob'
import Markdown from './Markdown'

// 풀이 코드를 Claude 로 분석(최적화/설명). 하루 한도 초과 시 429 안내.
// 마지막 분석은 풀이별로 기억해 두어, 새로고침하거나 다시 들어와도 결과(또는 진행 중인 분석)를 이어서 본다
export default function AiAnalyzePanel({
  solutionId,
  code,
  language,
  problemTitle,
}: {
  solutionId: number
  code: string
  language?: string
  problemTitle?: string
}) {
  const { result, loading, error, meta, start } = useAiJob(`ai-job:analyze:${solutionId}`, 'AI 분석에 실패했습니다.')
  const task: AiTask = meta === 'OPTIMIZE' ? 'OPTIMIZE' : 'EXPLAIN'

  function run(t: AiTask) {
    void start(() => aiApi.analyze({ task: t, code, language, problemTitle }), t)
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

      {loading && <p className="mt-3 text-sm text-slate-400">분석 중… (수 초 소요, 페이지를 떠나도 계속 진행돼요)</p>}
      {error && <p className="mt-3 text-sm text-rose-400">{error}</p>}
      {result && (
        <div className="mt-3 rounded-lg border border-slate-800 bg-slate-950/40 p-3">
          <Markdown>{result}</Markdown>
        </div>
      )}
    </div>
  )
}
