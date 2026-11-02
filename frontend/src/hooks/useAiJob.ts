import { useCallback, useEffect, useRef, useState } from 'react'
import { pollAiJob, type AiJob } from '../api/ai'
import { aiErrorText, aiJobFailText, apiErrorCode } from '../lib/apiError'

// AI 분석·힌트 패널이 같이 쓰는 잡 진행 상태.
// 잡은 서버(ai_jobs)에 남지만 화면은 jobId 를 잊으면 결과를 다시 찾을 수 없다. 그래서 마지막 jobId 를
// 화면 자리(storageKey: 풀이 id, 문제 id)별로 localStorage 에 적어 두고, 다시 열면 그 잡을 이어서 조회한다.
// - 아직 처리 중이면 로딩을 보여 주며 끝날 때까지 기다린다
// - 이미 끝났으면 결과를 바로 보여 준다(Claude 를 다시 부르지 않으니 하루 한도도 쓰지 않는다)
// - 남의 잡이거나 지워진 잡이면(404) 적어 둔 것을 지운다. 서버가 본인 잡만 돌려주므로 jobId 가 새도 결과는 안 보인다
interface Saved {
  id: number
  meta?: string // 분석 패널의 작업 종류(EXPLAIN/OPTIMIZE) 같은 화면 표시용 값
}

function read(key: string): Saved | null {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const saved = JSON.parse(raw) as Saved
    return Number.isInteger(saved.id) ? saved : null
  } catch {
    return null
  }
}

function write(key: string, saved: Saved | null) {
  try {
    if (saved) localStorage.setItem(key, JSON.stringify(saved))
    else localStorage.removeItem(key)
  } catch {
    // 저장소가 막혀 있으면 이어 보기만 안 될 뿐 요청은 그대로 된다
  }
}

export function useAiJob(storageKey: string, failText: string) {
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [meta, setMeta] = useState<string | undefined>()
  const abortRef = useRef<AbortController | null>(null)

  // 잡 하나를 끝날 때까지 따라간다. restoring: 저장해 둔 잡을 다시 여는 중이면 실패를 조용히 넘긴다
  const follow = useCallback(
    async (id: number, restoring: boolean) => {
      abortRef.current?.abort()
      const ctrl = new AbortController()
      abortRef.current = ctrl
      setLoading(true)
      setError('')
      setResult('')
      try {
        const job = await pollAiJob(id, ctrl.signal)
        if (job.status === 'DONE') setResult(job.result ?? '')
        else setError(aiJobFailText(job.errorCode, failText))
      } catch (err) {
        if (ctrl.signal.aborted) return
        if (apiErrorCode(err) === 'NOT_FOUND') {
          write(storageKey, null)
          if (restoring) return
        }
        if (!restoring || (err instanceof Error && err.message === 'TIMEOUT')) setError(aiErrorText(err, failText))
      } finally {
        if (!ctrl.signal.aborted) setLoading(false)
      }
    },
    [storageKey, failText],
  )

  // 화면을 열 때 이 자리의 마지막 잡을 이어서 본다. 떠나면 조회를 멈춘다
  useEffect(() => {
    const saved = read(storageKey)
    setMeta(saved?.meta)
    if (saved) void follow(saved.id, true)
    else {
      setResult('')
      setError('')
      setLoading(false)
    }
    return () => abortRef.current?.abort()
  }, [storageKey, follow])

  // 새 잡을 만들고 따라간다. 만들기 자체가 실패하면(한도 초과 등) 이전 결과 기록은 그대로 둔다
  const start = useCallback(
    async (create: () => Promise<AiJob>, nextMeta?: string) => {
      abortRef.current?.abort()
      setMeta(nextMeta)
      setLoading(true)
      setError('')
      setResult('')
      let created: AiJob
      try {
        created = await create()
      } catch (err) {
        setError(aiErrorText(err, failText))
        setLoading(false)
        return
      }
      write(storageKey, { id: created.id, meta: nextMeta })
      await follow(created.id, false)
    },
    [storageKey, failText, follow],
  )

  return { result, loading, error, meta, start }
}
