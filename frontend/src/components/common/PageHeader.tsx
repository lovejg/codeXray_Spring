import type { ReactNode } from 'react'

// 페이지 상단 헤더: 제목 + 설명 + (선택) 우측 액션/필터. 리스트 페이지 공통 구조.
export default function PageHeader({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle?: string
  children?: ReactNode
}) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="page-title flex items-center gap-2">
          <span className="text-teal-400">❯</span>
          {title}
        </h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  )
}
