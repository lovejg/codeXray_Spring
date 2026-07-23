import { POST_TYPE_LABEL, POST_TYPE_STYLE, type PostType } from '../../types'

export default function PostTypeBadge({ type }: { type: PostType }) {
  const s = POST_TYPE_STYLE[type]
  return (
    <span className="inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium" style={{ color: s.text, backgroundColor: s.bg }}>
      {POST_TYPE_LABEL[type]}
    </span>
  )
}
