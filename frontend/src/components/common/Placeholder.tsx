// 아직 구현 전인 페이지의 임시 자리. 순차적으로 실제 페이지로 교체 예정.
export default function Placeholder({ title }: { title: string }) {
  return (
    <div className="py-20 text-center">
      <h1 className="text-xl font-bold text-white">{title}</h1>
      <p className="mt-2 text-sm text-slate-500">준비 중입니다.</p>
    </div>
  )
}
