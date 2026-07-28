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

// 비동기 잡: 요청하면 PENDING 으로 생성되고, 폴링으로 DONE/FAILED 를 확인.
export interface AiJob {
  id: number
  kind: string
  status: 'PENDING' | 'DONE' | 'FAILED'
  result: string | null
  errorCode: string | null
}

export const aiApi = {
  analyze: (body: AiAnalyzeBody) => client.post<AiJob>('/ai/analyze', body).then((r) => r.data),
  hint: (body: AiHintBody) => client.post<AiJob>('/ai/hint', body).then((r) => r.data),
  job: (id: number) => client.get<AiJob>(`/ai/jobs/${id}`).then((r) => r.data),
}

// 잡이 끝날 때까지(1.5초 간격) 폴링. 너무 오래 걸리면 TIMEOUT.
export async function pollAiJob(id: number, intervalMs = 1500, maxAttempts = 60): Promise<AiJob> {
  for (let i = 0; i < maxAttempts; i++) {
    const job = await aiApi.job(id)
    if (job.status !== 'PENDING') return job
    await new Promise((r) => setTimeout(r, intervalMs))
  }
  throw new Error('TIMEOUT')
}
