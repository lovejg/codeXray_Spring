// 닉네임을 해시해 사람마다 고정된 색을 만든다(같은 이름 → 항상 같은 색).
// 다크 테마에 맞춰 배경은 반투명, 글자는 밝게.
export function avatarColors(name: string) {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) | 0 // 간단한 문자열 해시
  }
  const hue = Math.abs(hash) % 360
  return {
    bg: `hsl(${hue} 60% 45% / 0.20)`, // 반투명 배경
    fg: `hsl(${hue} 75% 72%)`, // 밝은 글자
    ring: `hsl(${hue} 70% 60% / 0.35)`, // 얇은 링
  }
}
