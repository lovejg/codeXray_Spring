import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/layout/Navbar'

// ─────────────────────────────────────────────────────────────
// 메인(랜딩) 페이지. 앱 공용 Navbar를 얹어 다른 기능들과 하나로 이어지게 함.
// 터미널/개발자 감성: 타이핑 애니메이션 + 기능별 터미널 창 예시.
// 꾸밈은 터미널 화면에서 실제로 볼 법한 것만 쓴다(빛 번짐, 알약 배지, 창 신호등 점, 떠오르는 카드 없음).
// 왼쪽 정렬, 각진 모서리, 선 하나로 칸을 나눈다
// ─────────────────────────────────────────────────────────────

const TYPE_LINES = [
  '알고리즘 문제를 기록한다',
  '내 풀이를 AI로 분석한다',
  '실수를 노트로 남긴다',
  '동료와 함께 성장한다',
]

export default function LandingPage() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <Hero />
      <Features />
      <Showcases />
      <FinalCta />
      <LandingFooter />
    </div>
  )
}

// 랜딩 안에서 쓰는 버튼 두 가지. 꽉 찬 색 버튼 대신 터미널의 선택지처럼 테두리와 글자만
const primaryLink =
  'inline-flex items-center gap-2 border border-teal-400/60 px-5 py-2.5 font-mono text-sm text-teal-300 transition hover:bg-teal-400/10'
const ghostLink =
  'inline-flex items-center gap-2 border border-slate-700 px-5 py-2.5 font-mono text-sm text-slate-300 transition hover:border-slate-500 hover:text-white'

// ── Hero: 타이핑 애니메이션 ──
function Hero() {
  const typed = useTypewriter(TYPE_LINES)

  return (
    <section className="border-b border-slate-800">
      <div className="mx-auto max-w-6xl px-6 pb-20 pt-20">
        <p className="mb-6 font-mono text-xs text-slate-500"># 알고리즘 학습 기록 &amp; 분석 플랫폼</p>

        <h1 className="font-mono text-4xl font-bold leading-tight tracking-tight text-white sm:text-6xl">
          <span className="text-teal-400">❯</span> codeXray
        </h1>

        <div className="mt-6 flex h-8 items-center font-mono text-lg text-slate-300 sm:text-2xl">
          <span className="text-slate-600">$&nbsp;</span>
          <span className="text-slate-200">{typed}</span>
          <span className="cursor-blink ml-0.5 text-teal-400">▊</span>
        </div>

        <p className="mt-8 max-w-xl text-slate-400">
          프로그래머스 문제를 한곳에서. 풀이를 기록하고,
          AI로 분석받고, 나만의 노트로 복습하세요.
        </p>

        <div className="mt-10 flex flex-wrap items-center gap-3">
          <Link to="/register" className={primaryLink}>
            시작하기 <span aria-hidden>→</span>
          </Link>
          <Link to="/problems" className={ghostLink}>
            문제 둘러보기
          </Link>
        </div>
      </div>
    </section>
  )
}

// ── 기능 소개 ──
const FEATURES = [
  { cmd: 'problems', title: '문제 탐색', desc: '689개 이상의 문제를 티어·출처·태그로 검색. `/` 키로 바로 검색하세요.' },
  { cmd: 'solutions', title: '풀이 & AI 분석', desc: '언어별 문법 하이라이팅 에디터로 풀이를 기록하고, AI가 복잡도를 짚어줍니다.' },
  { cmd: 'notes', title: '복습 노트', desc: '마크다운으로 실수와 아이디어를 정리. 코드블럭·복사 버튼까지 그대로.' },
  { cmd: 'community', title: '커뮤니티', desc: '문제에 얽힌 질문과 풀이를 공유하고, 다른 사람의 접근을 참고하세요.' },
]

function SectionHead({ label, title }: { label: string; title: string }) {
  return (
    <div className="mb-10">
      <p className="section-title mb-2">{label}</p>
      <h2 className="font-mono text-2xl font-bold text-white sm:text-3xl">{title}</h2>
    </div>
  )
}

function Features() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-16">
      <SectionHead label="// features" title="기록하고, 분석하고, 성장하기" />

      {/* 카드 대신 선으로 나눈 칸 네 개. 칸 사이 1px 틈에 테두리 색 배경이 비쳐 선이 된다(겹치는 선 없음) */}
      <div className="grid gap-px border border-slate-800 bg-slate-800 sm:grid-cols-2 lg:grid-cols-4">
        {FEATURES.map((f) => (
          <div key={f.cmd} className="bg-[#0a0d13] p-6">
            <div className="mb-3 font-mono text-sm text-teal-400">
              <span className="text-slate-600">$ codeXray </span>
              {f.cmd}
            </div>
            <h3 className="mb-2 font-mono text-base font-semibold text-white">{f.title}</h3>
            <p className="text-sm leading-relaxed text-slate-400">{f.desc}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

// ── 공용 터미널 창: 신호등 점 대신 제목 줄 하나 ──
function TerminalWindow({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="border border-slate-800 bg-[#0d1117]">
      <div className="flex items-center justify-between border-b border-slate-800 px-4 py-2 font-mono text-xs text-slate-500">
        <span>{title}</span>
        <span className="text-slate-700">codeXray</span>
      </div>
      {children}
    </div>
  )
}

// ── 기능별 예시(쇼케이스): 왼쪽 설명, 오른쪽 터미널 창 ──
function Showcases() {
  const items: { label: string; title: string; desc: string; preview: ReactNode }[] = [
    {
      label: '// problems',
      title: '689개+ 문제를 한눈에',
      desc: '티어·출처·태그로 걸러내고, 제목만 타이핑하면 실시간으로 찾아줍니다. 프로그래머스 URL을 붙여넣어 바로 풀이를 등록할 수도 있어요.',
      preview: <ProblemsPreview />,
    },
    {
      label: '// ai-review',
      title: '풀이 한 줄까지, AI가 함께 봅니다',
      desc: '내가 짠 코드를 붙여넣으면 시간·공간복잡도와 개선 포인트를 짚어줍니다. 브라우저 확장으로 저지 페이지에서 원클릭 저장까지.',
      preview: <AiPreview />,
    },
    {
      label: '// notes',
      title: '틀린 이유를 노트로 남기기',
      desc: '마크다운으로 실수·핵심 개념·코드를 정리해 두고, 복습할 때 다시 꺼내봅니다. 문법 하이라이팅과 복사 버튼은 기본.',
      preview: <NotePreview />,
    },
    {
      label: '// community',
      title: '막히면 함께 풀기',
      desc: '문제에 얽힌 질문을 올리고 다른 사람의 접근을 참고하세요. 추천·댓글로 더 나은 풀이를 함께 만들어갑니다.',
      preview: <CommunityPreview />,
    },
  ]

  return (
    <section className="mx-auto max-w-6xl px-6 py-16">
      <SectionHead label="// how it works" title="실제로 이렇게 쓰여요" />

      <div className="divide-y divide-slate-800 border-y border-slate-800">
        {items.map((it) => (
          <div key={it.label} className="grid items-start gap-8 py-12 md:grid-cols-[2fr_3fr]">
            <div>
              <p className="section-title mb-3">{it.label}</p>
              <h3 className="mb-3 font-mono text-xl font-bold text-white sm:text-2xl">{it.title}</h3>
              <p className="leading-relaxed text-slate-400">{it.desc}</p>
            </div>
            {/* min-w-0: 그리드 칸이 안의 긴 코드 줄만큼 늘어나 페이지가 옆으로 넓어지지 않게. 코드는 창 안에서만 가로로 민다 */}
            <div className="min-w-0">{it.preview}</div>
          </div>
        ))}
      </div>
    </section>
  )
}

// ── 예시 미리보기들 ──
// 문제, 티어, 태그는 실제 문제 기준(2026-10-29 운영 값). 티어는 체감 난이도가 쌓이면 바뀔 수 있다
function ProblemsPreview() {
  const rows = [
    { t: '완주하지 못한 선수', tier: 'S3', tags: ['해시'] },
    { t: '기차 선로', tier: 'P1', tags: ['구현', '백트래킹'] },
    { t: '섬 연결하기', tier: 'G1', tags: ['유니온파인드', '그리디'] },
  ]
  return (
    <TerminalWindow title="problems">
      <div className="divide-y divide-slate-800/70 font-mono text-[13px]">
        {rows.map((r) => (
          <div key={r.t} className="flex items-center gap-2 px-4 py-3">
            <span className="text-teal-400">❯</span>
            <span className="text-slate-200">{r.t}</span>
            <span className="ml-auto border border-amber-500/40 px-1.5 py-0.5 text-[11px] text-amber-300">{r.tier}</span>
            <span className="hidden gap-1 sm:flex">
              {r.tags.map((tg) => (
                <span key={tg} className="border border-teal-500/30 px-1.5 py-0.5 text-[11px] text-teal-300">#{tg}</span>
              ))}
            </span>
          </div>
        ))}
      </div>
    </TerminalWindow>
  )
}

function AiPreview() {
  return (
    <TerminalWindow title="solution.py">
      <div className="overflow-x-auto p-5 font-mono text-[13px] leading-relaxed">
        <pre className="text-slate-300">
{`def solution(participant, completion):
    count = {}
    for name in participant:
        count[name] = count.get(name, 0) + 1
    for name in completion:
        count[name] -= 1
    return next(n for n, c in count.items() if c > 0)`}
        </pre>
      </div>
      <div className="border-t border-slate-800 px-5 py-4 font-mono text-[13px]">
        <div className="mb-2 text-teal-400">── AI 분석</div>
        <p className="text-slate-400">
          <span className="text-slate-500">시간</span> O(n) · <span className="text-slate-500">공간</span> O(n) —
          이름별 개수를 해시맵에 세어 두 번의 순회로 해결했습니다. 동명이인이 있어도 맞습니다.
        </p>
      </div>
    </TerminalWindow>
  )
}

function NotePreview() {
  return (
    <TerminalWindow title="note.md">
      <div className="p-5 font-mono text-[13px] leading-relaxed">
        <div className="text-slate-200"><span className="text-teal-400">##</span> 이분탐색 경계 실수</div>
        <div className="mt-3 text-slate-400"><span className="text-slate-600">-</span> lo/hi 경계에서 <span className="text-slate-300">&lt;=</span> vs <span className="text-slate-300">&lt;</span> 를 자주 헷갈림</div>
        <div className="text-slate-400"><span className="text-slate-600">-</span> 정답 조건은 보통 <span className="text-slate-300">while (lo &lt;= hi)</span></div>
        <div className="mt-3 border border-slate-800 bg-[#0a0d13] px-3 py-2 text-slate-300">
          mid = (lo + hi) // 2
        </div>
      </div>
    </TerminalWindow>
  )
}

function CommunityPreview() {
  return (
    <TerminalWindow title="community">
      <div className="p-5 font-mono text-[13px] leading-relaxed">
        <div className="flex items-center gap-2">
          <span className="border border-teal-500/30 px-1.5 py-0.5 text-[11px] text-teal-300">질문</span>
          <span className="text-slate-200">DP 점화식이 안 세워져요</span>
        </div>
        <p className="mt-2 text-slate-400">배낭 문제인데 무게 제한을 어떻게 상태로 잡아야 할까요?</p>
        <div className="mt-4 border-l-2 border-slate-700 pl-3">
          <div className="text-slate-500"><span className="text-teal-400/70">@</span>reviewer</div>
          <p className="mt-1 text-slate-400">dp[i][w] = i번째까지 봤을 때 무게 w에서의 최대 가치로 잡아보세요.</p>
        </div>
      </div>
    </TerminalWindow>
  )
}

// ── 하단 CTA: 상자와 빛 번짐 없이 프롬프트 한 줄 ──
function FinalCta() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <p className="mb-6 font-mono text-lg text-slate-300">
        <span className="text-slate-600">$ </span>
        codeXray init
        <span className="cursor-blink ml-0.5 text-teal-400">▊</span>
      </p>
      <h2 className="mb-3 font-mono text-2xl font-bold text-white sm:text-3xl">지금 바로 시작하세요</h2>
      <p className="mb-8 text-slate-400">이메일 하나면 충분합니다. 무료로 이용해보세요.</p>
      <Link to="/register" className={primaryLink}>
        무료로 시작하기 <span aria-hidden>→</span>
      </Link>
    </section>
  )
}

// ── 푸터 ──
function LandingFooter() {
  return (
    <footer className="border-t border-slate-800">
      <div className="mx-auto flex max-w-6xl flex-col gap-2 px-6 py-10 font-mono text-xs text-slate-600 sm:flex-row">
        <span className="text-slate-500">❯ codeXray</span>
        <span>알고리즘 학습 기록 &amp; 분석</span>
        <span className="sm:ml-auto">v0.1.0 · 학습용 프로젝트</span>
      </div>
    </footer>
  )
}

// ─────────────────────────────────────────────────────────────
// 타이핑 애니메이션 훅: 여러 줄을 한 글자씩 쳤다 지웠다 반복.
// ─────────────────────────────────────────────────────────────
function useTypewriter(lines: string[]) {
  const [text, setText] = useState('')
  const idx = useRef(0)
  const char = useRef(0)
  const deleting = useRef(false)

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>

    function tick() {
      const full = lines[idx.current]
      if (!deleting.current) {
        char.current += 1
        setText(full.slice(0, char.current))
        if (char.current === full.length) {
          deleting.current = true
          timer = setTimeout(tick, 1600)
          return
        }
        timer = setTimeout(tick, 70)
      } else {
        char.current -= 1
        setText(full.slice(0, char.current))
        if (char.current === 0) {
          deleting.current = false
          idx.current = (idx.current + 1) % lines.length
          timer = setTimeout(tick, 400)
          return
        }
        timer = setTimeout(tick, 35)
      }
    }

    timer = setTimeout(tick, 500)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return text
}
