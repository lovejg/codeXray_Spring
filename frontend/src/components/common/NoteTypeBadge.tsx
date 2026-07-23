import { NOTE_TYPE_COLOR, NOTE_TYPE_LABEL, type NoteType } from '../../types'

export default function NoteTypeBadge({ type }: { type: NoteType }) {
  const c = NOTE_TYPE_COLOR[type]
  return (
    <span
      className="inline-flex items-center rounded px-1.5 py-0.5 text-[11px] font-medium"
      style={{ color: c.text, backgroundColor: c.bg }}
    >
      {NOTE_TYPE_LABEL[type]}
    </span>
  )
}
