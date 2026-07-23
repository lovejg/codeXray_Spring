import type { AppNotification } from '../types'

export interface FormattedNotification {
  text: string
  link: string | null
}

// 알림 타입별 사용자용 문구 + 이동 링크.
export function formatNotification(n: AppNotification): FormattedNotification {
  const p = n.payload
  const postLink = p.postId ? `/community/${p.postId}` : null

  switch (n.type) {
    case 'COMMENT':
      return { text: `${p.commenterNickname ?? '누군가'}님이 "${p.postTitle ?? '내 글'}"에 댓글을 남겼습니다.`, link: postLink }
    case 'ADMIN_REPLY':
      return { text: `건의사항 "${p.postTitle ?? ''}"에 관리자 답변이 등록되었습니다.`, link: p.postId ? `/suggestions/${p.postId}` : null }
    case 'STATUS_CHANGE':
      return { text: `건의사항 "${p.postTitle ?? ''}" 상태가 변경되었습니다.`, link: p.postId ? `/suggestions/${p.postId}` : null }
    case 'POST_HIDDEN':
      return { text: `내 게시글 "${p.postTitle ?? ''}"이(가) 관리자에 의해 숨김 처리되었습니다.`, link: postLink }
    case 'REPORT_RESOLVED':
      return { text: `신고하신 "${p.postTitle ?? ''}" 건이 ${p.resolution === 'DISMISSED' ? '기각' : '처리 완료'}되었습니다.`, link: postLink }
    case 'NEW_REPORT':
      return { text: `새 신고가 접수되었습니다: "${p.postTitle ?? ''}"`, link: '/admin/reports' }
    case 'TIER_UP':
      return { text: `축하합니다! ${p.family ?? ''} 티어 문제 "${p.problemTitle ?? ''}"를 처음 해결했습니다.`, link: p.problemId ? `/problems/${p.problemId}` : null }
    case 'STALE_SUGGESTION':
      return { text: `미처리 건의사항이 ${p.count ?? 0}건 있습니다 (가장 오래된 건 ${p.oldestDays ?? 0}일 경과).`, link: '/suggestions' }
    default:
      return { text: '새 알림이 있습니다.', link: null }
  }
}
