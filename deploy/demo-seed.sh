#!/bin/bash
# 시연(샘플) 데이터를 새로 넣는다. 내용은 demo-seed.sql(시연 계정 5개, 풀이·메모·체감 난이도·노트, 커뮤니티 글과 건의사항, 댓글, 추천)
#   서버(/srv/codexray):  DEMO_PASSWORD='...' bash demo-seed.sh
#   로컬(deploy/ 폴더):  docker compose 로 띄운 뒤 같은 명령
# 계정: demo1~5@codexray-demo.example. 비밀번호는 모두 DEMO_PASSWORD
# 비밀번호는 git 에 두지 않는다. 운영 DB 에 들어가는 계정이라, 공개된 비밀번호면 누구나 시연 계정으로 글을 쓸 수 있다
# 몇 번을 돌려도 같은 모양이다. 넣은 뒤 backend 를 다시 띄워(약 20초 끊김) 티어를 앱의 계산식으로 다시 채운다
set -euo pipefail
cd "$(dirname "$0")"

read -ra COMPOSE <<< "${COMPOSE:-docker compose}"
PW=${DEMO_PASSWORD:?시연 계정 비밀번호를 DEMO_PASSWORD 로 주세요}
if [ ${#PW} -lt 8 ]; then echo "비밀번호는 8자 이상으로 주세요"; exit 1; fi
DB_USER=$(grep -m1 '^DB_USERNAME=' .env | cut -d= -f2-)

# Spring 의 BCryptPasswordEncoder 와 같은 해시를 만든다. htpasswd 는 $2y$ 로 내놓는데 알고리즘이 같아서 $2a$ 로 바꾼다
# 비밀번호는 명령 인자가 아니라 표준 입력(-i)으로 넘긴다. 인자로 주면 그동안 ps 에 보인다
HASH=$(printf '%s' "$PW" | docker run --rm -i httpd:2.4-alpine htpasswd -niBC 10 x | cut -d: -f2 | sed 's/^\$2y\$/$2a$/')

"${COMPOSE[@]}" exec -T postgres psql -U "$DB_USER" -d codexray -q -v pw_hash="'$HASH'" < demo-seed.sql

# 티어를 다시 채운다: demo-seed.sql 이 티어를 비웠고, backend 가 뜰 때 TierBootstrap 이 전체 재계산을 돌린다(캐시도 함께 비운다)
"${COMPOSE[@]}" restart backend < /dev/null > /dev/null
CID=$("${COMPOSE[@]}" ps -q backend < /dev/null)
for _ in $(seq 60); do
  [ "$(docker inspect -f '{{.State.Health.Status}}' "$CID")" = healthy ] && break
  sleep 3
done
"${COMPOSE[@]}" exec -T postgres psql -U "$DB_USER" -d codexray -Atc \
  "select '티어 있는 문제 ' || count(tier) || ' / ' || count(*) from problems" < /dev/null
echo "시연 데이터를 넣었다"
