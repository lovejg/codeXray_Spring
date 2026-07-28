import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import Navbar from '../components/layout/Navbar'

// ─────────────────────────────────────────────────────────────
// 메인(랜딩) 페이지. 앱 공용 Navbar를 얹어 다른 기능들과 하나로 이어지게 함.
// 터미널/개발자 감성: 타이핑 애니메이션 + 기능별 터미널 창 예시.
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

// ── Hero: 타이핑 애니메이션 ──
function Hero() {
  const typed = useTypewriter(TYPE_LINES)

  return (
    <section className="relative overflow-hidden">
      <div
        aria-hidden
        className="pointer-events-none absolute left-1/2 top-[-20%] h-[420px] w-[720px] -translate-x-1/2 rounded-full opacity-20 blur-[120px]"
        style={{ background: 'radial-gradient(circle, #2dd4bf 0%, transparent 70%)' }}
      />
      <div className="mx-auto max-w-6xl px-6 py-24 text-center">
        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-slate-800 bg-slate-900/60 px-3 py-1 font-mono text-xs text-slate-400">
          <span className="h-1.5 w-1.5 rounded-full bg-teal-400 shadow-[0_0_6px_#2dd4bf]" />
          알고리즘 학습 기록 &amp; 분석 플랫폼
        </div>

        <h1 className="font-mono text-4xl font-bold leading-tight tracking-tight text-white sm:text-6xl">
          <span className="text-teal-400">❯</span> codeXray
        </h1>

        <div className="mt-6 flex h-8 items-center justify-center font-mono text-lg text-slate-300 sm:text-2xl">
          <span className="text-slate-600">$&nbsp;</span>
          <span className="text-slate-200">{typed}</span>
          <span className="cursor-blink ml-0.5 text-teal-400">▊</span>
        </div>

        <p className="mx-auto mt-8 max-w-xl text-slate-400">
          백준·프로그래머스·LeetCode 문제를 한곳에서. 풀이를 기록하고,
          AI로 분석받고, 나만의 노트로 복습하세요.
        </p>

        <div className="mt-10 flex items-center justify-center gap-3">
          <Link to="/register" className="btn-primary px-6 py-3 text-base">
            시작하기 <span aria-hidden>→</span>
          </Link>
          <Link to="/problems" className="btn-ghost px-6 py-3 text-base">
            문제 둘러보기
          </Link>
        </div>
      </div>
    </section>
  )
}

// ── 기능 소개 카드 ──
const FEATURES = [
  { cmd: 'problems', title: '문제 탐색', desc: '689개 이상의 문제를 티어·출처·태그로 검색. `/` 키로 바로 검색하세요.' },
  { cmd: 'solutions', title: '풀이 & AI 분석', desc: '언어별 문법 하이라이팅 에디터로 풀이를 기록하고, AI가 복잡도를 짚어줍니다.' },
  { cmd: 'notes', title: '복습 노트', desc: '마크다운으로 실수와 아이디어를 정리. 코드블럭·복사 버튼까지 그대로.' },
  { cmd: 'community', title: '커뮤니티', desc: '문제에 얽힌 질문과 풀이를 공유하고, 다른 사람의 접근을 참고하세요.' },
]

function Features() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-16">
      <p className="section-title mb-2 text-center">// features</p>
      <h2 className="mb-12 text-center font-mono text-2xl font-bold text-white sm:text-3xl">
        기록하고, 분석하고, 성장하기
      </h2>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {FEATURES.map((f) => (
          <div key={f.cmd} className="glass-card group p-6 transition hover:-translate-y-0.5 hover:border-teal-400/40 hover:bg-slate-900/70">
            <div className="mb-3 font-mono text-sm text-teal-400">
              <span className="text-slate-600">codeXray </span>
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

// ── 공용 터미널 창 ──
function TerminalWindow({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="glass-card overflow-hidden shadow-xl shadow-black/30">
      <div className="flex items-center gap-2 border-b border-slate-800 bg-slate-950/70 px-4 py-3">
        <span className="h-3 w-3 rounded-full bg-rose-500/80" />
        <span className="h-3 w-3 rounded-full bg-amber-400/80" />
        <span className="h-3 w-3 rounded-full bg-emerald-500/80" />
        <span className="ml-3 font-mono text-xs text-slate-500">{title}</span>
      </div>
      {children}
    </div>
  )
}

// ── 기능별 예시(쇼케이스): 설명 + 터미널 창을 좌우 번갈아 배치 ──
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
      <p className="section-title mb-2 text-center">// how it works</p>
      <h2 className="mb-16 text-center font-mono text-2xl font-bold text-white sm:text-3xl">
        실제로 이렇게 쓰여요
      </h2>

      <div className="space-y-20">
        {items.map((it, i) => (
          <div key={it.label} className="grid items-center gap-8 md:grid-cols-2">
            <div className={i % 2 === 1 ? 'md:order-2' : ''}>
              <p className="section-title mb-3">{it.label}</p>
              <h3 className="mb-3 font-mono text-xl font-bold text-white sm:text-2xl">{it.title}</h3>
              <p className="leading-relaxed text-slate-400">{it.desc}</p>
            </div>
            <div className={i % 2 === 1 ? 'md:order-1' : ''}>{it.preview}</div>
          </div>
        ))}
      </div>
    </section>
  )
}

// ── 예시 미리보기들 ──
function ProblemsPreview() {
  const rows = [
    { t: '두 수의 합', tier: 'S3', tags: ['해시', '완전탐색'] },
    { t: '기차 선로', tier: 'G2', tags: ['백트래킹', '구현'] },
    { t: '섬 연결하기', tier: 'G4', tags: ['그래프', 'MST'] },
  ]
  return (
    <TerminalWindow title="problems — codeXray">
      <div className="divide-y divide-slate-800/70 font-mono text-[13px]">
        {rows.map((r) => (
          <div key={r.t} className="flex items-center gap-2 px-4 py-3">
            <span className="text-teal-400">❯</span>
            <span className="text-slate-200">{r.t}</span>
            <span className="ml-auto rounded border border-amber-500/40 px-1.5 py-0.5 text-[11px] text-amber-300">{r.tier}</span>
            <span className="hidden gap-1 sm:flex">
              {r.tags.map((tg) => (
                <span key={tg} className="rounded border border-teal-500/30 px-1.5 py-0.5 text-[11px] text-teal-300">#{tg}</span>
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
    <TerminalWindow title="two-sum.py — codeXray">
      <div className="overflow-x-auto p-5 font-mono text-[13px] leading-relaxed">
        <pre className="text-slate-300">
{`def two_sum(nums, target):
    seen = {}
    for i, n in enumerate(nums):
        if target - n in seen:
            return [seen[target - n], i]
        seen[n] = i`}
        </pre>
      </div>
      <div className="border-t border-slate-800 bg-slate-950/50 px-5 py-4 font-mono text-[13px]">
        <div className="mb-2 flex items-center gap-2 text-teal-400">
          <span className="h-1.5 w-1.5 rounded-full bg-teal-400 shadow-[0_0_6px_#2dd4bf]" />
          AI 분석
        </div>
        <p className="text-slate-400">
          <span className="text-slate-500">시간</span> O(n) · <span className="text-slate-500">공간</span> O(n) —
          해시맵으로 한 번의 순회에 해결했습니다. 브루트포스 대비 최적입니다.
        </p>
      </div>
    </TerminalWindow>
  )
}

function NotePreview() {
  return (
    <TerminalWindow title="note.md — codeXray">
      <div className="p-5 font-mono text-[13px] leading-relaxed">
        <div className="text-slate-200"><span className="text-teal-400">##</span> 이분탐색 경계 실수</div>
        <div className="mt-3 text-slate-400"><span className="text-slate-600">-</span> lo/hi 경계에서 <span className="text-slate-300">&lt;=</span> vs <span className="text-slate-300">&lt;</span> 를 자주 헷갈림</div>
        <div className="text-slate-400"><span className="text-slate-600">-</span> 정답 조건은 보통 <span className="text-slate-300">while (lo &lt;= hi)</span></div>
        <div className="mt-3 rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-slate-300">
          mid = (lo + hi) // 2
        </div>
      </div>
    </TerminalWindow>
  )
}

function CommunityPreview() {
  return (
    <TerminalWindow title="community — codeXray">
      <div className="p-5 font-mono text-[13px] leading-relaxed">
        <div className="flex items-center gap-2">
          <span className="rounded border border-teal-500/30 px-1.5 py-0.5 text-[11px] text-teal-300">질문</span>
          <span className="text-slate-200">DP 점화식이 안 세워져요</span>
        </div>
        <p className="mt-2 text-slate-400">배낭 문제인데 무게 제한을 어떻게 상태로 잡아야 할까요?</p>
        <div className="mt-4 border-l-2 border-teal-500/40 pl-3">
          <div className="text-slate-500"><span className="text-teal-400/70">@</span>reviewer</div>
          <p className="mt-1 text-slate-400">dp[i][w] = i번째까지 봤을 때 무게 w에서의 최대 가치로 잡아보세요.</p>
        </div>
      </div>
    </TerminalWindow>
  )
}

// ── 하단 CTA ──
function FinalCta() {
  return (
    <section className="mx-auto max-w-6xl px-6 py-24 text-center">
      <div className="glass-card relative overflow-hidden px-6 py-16">
        <div
          aria-hidden
          className="pointer-events-none absolute left-1/2 top-1/2 h-[300px] w-[500px] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-[0.15] blur-[100px]"
          style={{ background: 'radial-gradient(circle, #2dd4bf 0%, transparent 70%)' }}
        />
        <p className="mb-6 font-mono text-lg text-slate-300">
          <span className="text-slate-600">$ </span>
          codeXray init
          <span className="cursor-blink ml-0.5 text-teal-400">▊</span>
        </p>
        <h2 className="mb-3 font-mono text-2xl font-bold text-white sm:text-3xl">지금 바로 시작하세요</h2>
        <p className="mb-8 text-slate-400">이메일 하나면 충분합니다. 무료로 이용해보세요.</p>
        <Link to="/register" className="btn-primary px-7 py-3 text-base">
          무료로 시작하기 <span aria-hidden>→</span>
        </Link>
      </div>
    </section>
  )
}

// ── 푸터 ──
function LandingFooter() {
  return (
    <footer className="border-t border-slate-800/80">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-2 px-6 py-10 font-mono text-xs text-slate-600 sm:flex-row">
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
