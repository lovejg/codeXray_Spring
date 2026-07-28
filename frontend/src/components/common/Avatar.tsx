import { avatarColors } from '../../lib/avatar'

// 닉네임 첫 글자 + 이름별 고정 색 아바타. navbar/프로필 등에서 재사용.
export default function Avatar({ name, size = 28 }: { name: string; size?: number }) {
  const c = avatarColors(name || '?')
  return (
    <span
      className="inline-flex shrink-0 items-center justify-center rounded-full font-mono font-bold uppercase"
      style={{
        width: size,
        height: size,
        fontSize: Math.round(size * 0.42),
        backgroundColor: c.bg,
        color: c.fg,
        boxShadow: `inset 0 0 0 1px ${c.ring}`,
      }}
    >
      {(name || '?').charAt(0)}
    </span>
  )
}
