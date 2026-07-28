import { type ReactNode, useEffect } from 'react'
import { X } from 'lucide-react'

type ModalSize = 'md' | 'lg' | 'xl' | '2xl'

interface Props {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  size?: ModalSize
}

// 크기별 최대 너비. 코드가 들어가는 모달은 'xl' 이상을 권장.
const SIZE_CLASS: Record<ModalSize, string> = {
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-3xl',
  '2xl': 'max-w-5xl',
}

export default function Modal({ open, onClose, title, children, size = 'md' }: Props) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={onClose}>
      <div
        className={`w-full ${SIZE_CLASS[size]} max-h-[90vh] overflow-y-auto rounded-2xl border border-slate-800 bg-slate-900 p-5`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-mono text-base font-semibold text-white">{title}</h2>
          <button onClick={onClose} className="text-slate-500 hover:text-white" aria-label="닫기">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
