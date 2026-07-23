import type { Author } from '../../types'

// 작성자 표시. 탈퇴(id=0)면 링크 없이 회색.
export default function AuthorName({ author }: { author: Author }) {
  if (author.id === 0) {
    return <span className="text-slate-500">{author.nickname}</span>
  }
  return <span className="text-slate-300">{author.nickname}</span>
}
