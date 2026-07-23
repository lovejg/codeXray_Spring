// access token 만 localStorage 로 관리.
// refresh token 은 백엔드가 httpOnly 쿠키로 관리하므로 JS 에서 저장하지 않는다.
const ACCESS_KEY = 'token'

export const getAccessToken = () => localStorage.getItem(ACCESS_KEY)
export const setAccessToken = (token: string) => localStorage.setItem(ACCESS_KEY, token)
export const clearAccessToken = () => localStorage.removeItem(ACCESS_KEY)
