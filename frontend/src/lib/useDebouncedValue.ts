import { useEffect, useState } from 'react'

// 값이 바뀐 뒤 delayMs 동안 추가 변경이 없을 때에만 갱신되는 "디바운스된" 값.
// 검색어를 타이핑하는 동안 매 글자마다 API를 호출하지 않고, 입력이 잠깐 멈추면 한 번만 반영한다.
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(t) // 다음 입력이 들어오면 이전 타이머 취소 → 마지막 입력만 살아남음
  }, [value, delayMs])

  return debounced
}
