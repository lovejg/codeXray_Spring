import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import PageHeader from '../components/common/PageHeader'

// 개인정보처리방침. 소셜 로그인 앱을 공개(구글 프로덕션 게시, 네이버 검수)하려면 이 주소가 있어야 한다.
// 내용은 실제 코드 기준이다. 모으는 칸, 탈퇴 때 지우는 범위(각 테이블의 FK), 토큰·백업 보관 기간이 바뀌면 여기도 고친다

const EFFECTIVE_DATE = '2026년 10월 29일'

export default function PrivacyPage() {
  return (
    <div className="max-w-3xl">
      <PageHeader title="개인정보처리방침" subtitle={`시행일 ${EFFECTIVE_DATE}`} />

      <div className="space-y-10 text-[15px] leading-relaxed text-slate-300">
        <p>
          codeXray(이하 "서비스")는 개인이 운영하는 학습용 프로젝트입니다. 서비스를 쓰는 데 꼭 필요한 정보만 모으고,
          아래에 적은 목적 밖으로는 쓰지 않습니다.
        </p>

        <Section title="1. 모으는 정보">
          <Table
            head={['언제', '무엇을']}
            rows={[
              ['이메일로 가입할 때', '이메일 주소, 닉네임, 비밀번호(복원할 수 없게 암호화해 저장)'],
              ['구글·네이버로 가입할 때', '이메일 주소, 이름 또는 별명(닉네임 초기값), 로그인 제공자 종류와 그 계정의 고유 번호. 비밀번호는 받지 않습니다'],
              ['서비스를 쓰는 동안', '직접 남긴 풀이 코드와 메모, 노트, 커뮤니티 글·댓글·추천·신고, 문제 체감 난이도 평가, 알림, AI 분석·힌트 요청 내용(붙여넣은 코드와 문제 이름)'],
              ['접속할 때 자동으로', '서버 접속 기록(IP 주소, 접속 시각, 요청한 주소)'],
            ]}
          />
        </Section>

        <Section title="2. 쓰는 곳">
          <ul className="list-disc space-y-1.5 pl-5">
            <li>가입, 로그인, 본인 확인(이메일 인증 메일 발송)</li>
            <li>풀이·노트 기록, 커뮤니티, 문제 추천처럼 화면에서 제공하는 기능</li>
            <li>AI 분석·힌트 요청을 처리해 결과를 보여 주기</li>
            <li>오류 확인과 부정 이용 방지(접속 기록)</li>
          </ul>
          <p className="mt-3 text-slate-400">광고, 마케팅, 제3자 판매에는 쓰지 않습니다.</p>
        </Section>

        <Section title="3. 맡겨서 처리하는 곳">
          <p className="mb-3">서비스 운영에 필요한 일부를 아래 업체에 맡깁니다. 모두 국외에 있습니다.</p>
          <Table
            head={['업체', '맡기는 일', '넘어가는 정보']}
            rows={[
              ['Anthropic (미국)', 'AI 분석·힌트 생성', 'AI 요청 때 붙여넣은 코드와 문제 이름'],
              ['Resend (미국)', '인증 메일 발송', '이메일 주소'],
              ['Google, NAVER', '소셜 로그인(직접 고른 경우만)', '로그인 확인 결과'],
              ['Oracle Cloud (일본)', '서버 운영', '서비스의 모든 데이터'],
              ['Cloudflare', '데이터베이스 백업 보관', '데이터베이스 백업 파일'],
            ]}
          />
          <p className="mt-3 text-slate-400">
            글꼴은 Google Fonts에서 불러오므로, 화면을 열 때 브라우저가 Google 서버에 접속합니다.
          </p>
        </Section>

        <Section title="4. 보관 기간과 지우기">
          <ul className="list-disc space-y-1.5 pl-5">
            <li>
              회원 정보와 직접 남긴 기록은 탈퇴할 때까지 보관합니다. 프로필 화면에서 탈퇴하면 계정, 풀이와 메모, 노트,
              체감 난이도 평가, 추천, 신고, 알림, AI 요청 기록을 바로 지웁니다.
            </li>
            <li>
              커뮤니티 글과 댓글은 다른 사람의 대화가 끊기지 않도록 남기되, 작성자 정보를 지워 누가 썼는지 알 수 없게
              합니다. 남기고 싶지 않은 글은 탈퇴 전에 직접 지워 주세요.
            </li>
            <li>이메일 인증 링크 기록은 만료되거나 쓰인 뒤 30일 안에 지웁니다.</li>
            <li>로그인 유지 정보는 14일이 지나면 자동으로 사라집니다.</li>
            <li>서버 접속 기록은 정해진 용량이 차면 오래된 것부터 지워집니다.</li>
            <li>
              데이터베이스 백업은 장애 복구용으로 하루 한 번 만들고 약 14일 뒤 자동으로 지웁니다. 탈퇴한 정보도 그 기간에는
              백업 안에 남아 있을 수 있습니다.
            </li>
          </ul>
        </Section>

        <Section title="5. 브라우저에 저장하는 것">
          <ul className="list-disc space-y-1.5 pl-5">
            <li>로그인 상태 확인용 값(로컬 저장소)과 로그인 유지용 쿠키(스크립트가 읽을 수 없는 쿠키)</li>
            <li>소셜 로그인 중간에 위조 요청을 막는 일회용 값(탭을 닫으면 사라짐)</li>
          </ul>
          <p className="mt-3 text-slate-400">방문 분석이나 광고용 쿠키는 쓰지 않습니다.</p>
        </Section>

        <Section title="6. 이용자의 권리">
          <p>
            언제든 프로필 화면에서 닉네임과 비밀번호를 바꾸거나 탈퇴할 수 있습니다. 그 밖에 내 정보 열람, 정정, 삭제 요청은
            아래 문의처로 보내 주세요.
          </p>
        </Section>

        <Section title="7. 문의처">
          <p>
            <Link to="/suggestions" className="text-teal-300 hover:text-teal-200">건의사항 게시판</Link>에 남겨 주세요.
          </p>
        </Section>

        <Section title="8. 바뀔 때">
          <p>이 방침이 바뀌면 시행일을 고치고 이 화면에 알립니다.</p>
        </Section>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="mb-3 font-mono text-lg font-semibold text-white">{title}</h2>
      {children}
    </section>
  )
}

// 좁은 화면에서는 표가 상자 안에서만 가로로 밀린다
function Table({ head, rows }: { head: string[]; rows: string[][] }) {
  return (
    <div className="overflow-x-auto border border-slate-800">
      <table className="w-full min-w-[520px] text-sm">
        <thead className="border-b border-slate-800 bg-slate-950/60 text-left font-mono text-xs text-slate-500">
          <tr>{head.map((h) => <th key={h} className="px-4 py-2.5 font-semibold">{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-slate-800/70">
          {rows.map((r) => (
            <tr key={r[0]}>
              {r.map((c, i) => (
                <td key={i} className={`px-4 py-3 align-top ${i === 0 ? 'whitespace-nowrap text-slate-200' : 'text-slate-400'}`}>{c}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
