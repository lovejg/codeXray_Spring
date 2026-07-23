export type TierFamily = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND'

export type Tier =
  | 'BRONZE_III' | 'BRONZE_II' | 'BRONZE_I'
  | 'SILVER_III' | 'SILVER_II' | 'SILVER_I'
  | 'GOLD_III' | 'GOLD_II' | 'GOLD_I'
  | 'PLATINUM_III' | 'PLATINUM_II' | 'PLATINUM_I'
  | 'DIAMOND_III' | 'DIAMOND_II' | 'DIAMOND_I'

// 낮음 → 높음. 티어 범위 슬라이더/정렬 인덱스에 사용.
export const TIER_ORDER: Tier[] = [
  'BRONZE_III', 'BRONZE_II', 'BRONZE_I',
  'SILVER_III', 'SILVER_II', 'SILVER_I',
  'GOLD_III', 'GOLD_II', 'GOLD_I',
  'PLATINUM_III', 'PLATINUM_II', 'PLATINUM_I',
  'DIAMOND_III', 'DIAMOND_II', 'DIAMOND_I',
]

const FAMILY_COLOR: Record<TierFamily, string> = {
  BRONZE: '#b06f42',
  SILVER: '#9aa4b2',
  GOLD: '#f5b301',
  PLATINUM: '#22d3ee',
  DIAMOND: '#5b8cff',
}

const FAMILY_KO: Record<TierFamily, string> = {
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  DIAMOND: '다이아',
}

export function tierFamily(tier: Tier): TierFamily {
  return tier.split('_')[0] as TierFamily
}

export function tierSub(tier: Tier): string {
  return tier.split('_')[1] // III | II | I
}

export function tierColor(tier: Tier): string {
  return FAMILY_COLOR[tierFamily(tier)]
}

export function familyColor(family: TierFamily): string {
  return FAMILY_COLOR[family]
}

export function tierLabel(tier: Tier): string {
  return `${FAMILY_KO[tierFamily(tier)]} ${tierSub(tier)}`
}

export function familyLabel(family: TierFamily): string {
  return FAMILY_KO[family]
}

export function tierIndex(tier: Tier): number {
  return TIER_ORDER.indexOf(tier)
}
