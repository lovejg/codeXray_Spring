import client from './client'

export type AiTask = 'OPTIMIZE' | 'EXPLAIN'

export interface AiAnalyzeBody {
  task: AiTask
  code: string
  language?: string
  problemTitle?: string
}

export interface AiHintBody {
  problemId: number
  partialCode?: string
}

// 비동기 잡: 요청하면 PENDING 으로 생성되고, 컨슈머가 가져가면 PROCESSING, 끝나면 DONE/FAILED.
export interface AiJob {
  id: number
  kind: string
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED'
  result: string | null
  errorCode: string | null
}

export const aiApi = {
  analyze: (body: AiAnalyzeBody) => client.post<AiJob>('/ai/analyze', body).then((r) => r.data),
  hint: (body: AiHintBody) => client.post<AiJob>('/ai/hint', body).then((r) => r.data),
  job: (id: number) => client.get<AiJob>(`/ai/jobs/${id}`).then((r) => r.data),
}

// 잡이 끝날 때까지(2초 간격, 최대 4분) 폴링. 너무 오래 걸리면 TIMEOUT.
// 서버는 15분 넘게 멈춘 잡을 FAILED(AI_TIMEOUT)로 정리하므로, 화면이 먼저 포기해도 잡은 언젠가 끝난다
export async function pollAiJob(id: number, intervalMs = 2000, maxAttempts = 120): Promise<AiJob> {
  for (let i = 0; i < maxAttempts; i++) {
    const job = await aiApi.job(id)
    if (job.status === 'DONE' || job.status === 'FAILED') return job
    await new Promise((r) => setTimeout(r, intervalMs))
  }
  throw new Error('TIMEOUT')
}
