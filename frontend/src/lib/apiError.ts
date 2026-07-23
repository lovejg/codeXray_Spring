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

export function apiErrorCode(err: unknown): string | undefined {
  if (err instanceof AxiosError) {
    const data = err.response?.data as ErrorBody | undefined
    return data?.errorCode
  }
  return undefined
}
