import client from './client'

export type AiTask = 'OPTIMIZE' | 'EXPLAIN'

export interface AiAnalyzeBody {
  task: AiTask
  code: string
  language?: string
  problemTitle?: string
}

export const aiApi = {
  analyze: (body: AiAnalyzeBody) =>
    client.post<{ result: string }>('/ai/analyze', body).then((r) => r.data.result),
}
