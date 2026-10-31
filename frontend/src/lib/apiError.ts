import { AxiosError } from 'axios'

// 백엔드 ErrorResponse { statusCode, errorCode, message, fieldErrors }
interface ErrorBody {
  statusCode?: number
  errorCode?: string
  message?: string
  fieldErrors?: { field: string; message: string }[] | null
}

// axios 에러에서 사용자용 메시지를 뽑는다. 필드 에러가 있으면 첫 항목을 우선.
export function apiErrorMessage(err: unknown, fallback = '요청을 처리하지 못했습니다.'): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as ErrorBody | undefined
    if (data?.fieldErrors && data.fieldErrors.length > 0) {
      return data.fieldErrors[0].message
    }
    if (data?.message) return data.message
  }
  return fallback
}

// AI 분석·힌트 패널이 같이 쓰는 오류 문구. 요청 실패, 처리 실패(FAILED 잡), 폴링 시간 초과를 한 곳에서 고른다
export function aiErrorText(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message === 'TIMEOUT') return '처리가 지연되고 있어요. 잠시 후 다시 시도해 주세요.'
  const code = apiErrorCode(err)
  if (code === 'AI_RATE_LIMIT_EXCEEDED') return '오늘 AI 사용 한도를 모두 썼습니다. 내일 다시 시도해 주세요.'
  if (code === 'AI_UNAVAILABLE') return 'AI 기능을 지금 사용할 수 없습니다.'
  return apiErrorMessage(err, fallback)
}

// FAILED 로 끝난 잡의 errorCode → 화면 문구
export function aiJobFailText(errorCode: string | null, fallback: string): string {
  if (errorCode === 'AI_UNAVAILABLE') return 'AI 기능을 지금 사용할 수 없습니다.'
  if (errorCode === 'AI_REFUSED') return 'AI가 이 요청에는 답하지 않았어요. 코드나 요청을 바꿔 다시 시도해 주세요.'
  if (errorCode === 'AI_TIMEOUT') return '처리가 너무 오래 걸려 중단됐어요. 다시 시도해 주세요.'
  return fallback
}

export function apiErrorCode(err: unknown): string | undefined {
  if (err instanceof AxiosError) {
    const data = err.response?.data as ErrorBody | undefined
    return data?.errorCode
  }
  return undefined
}
