#!/usr/bin/env bash
cd ~/spring/codeXray/frontend
npm run dev > /tmp/vite.log 2>&1 &
for i in $(seq 1 40); do
  grep -qiE "Local:|ready in" /tmp/vite.log 2>/dev/null && break
  sleep 0.5
done
sleep 1
echo "=== vite log ==="
grep -iE "Local:|ready" /tmp/vite.log | head -3
echo "=== 1) index.html 서빙? ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:5173/
echo "=== 2) 프록시 통해 문제 목록 (5173 → 8080) ==="
curl -s "http://localhost:5173/api/problems?size=2" | grep -oE '"total":[0-9]+|"title":"[^"]*"' | head -3
echo "=== 3) 미인증 보호 API 401 확인 ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:5173/api/users/me
# vite 종료
pkill -f "vite" 2>/dev/null
echo "=== done ==="
