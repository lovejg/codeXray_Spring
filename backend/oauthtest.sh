#!/usr/bin/env bash
cd ~/spring/codeXray/backend

DB="docker exec codexray-db psql -U postgres -d codexray"
$DB -c "TRUNCATE users RESTART IDENTITY CASCADE;" >/dev/null 2>&1
$DB -c "TRUNCATE email_verification_token RESTART IDENTITY CASCADE;" >/dev/null 2>&1

export ADMIN_EMAIL="admin@codexray.com"
export GOOGLE_TOKEN_URI="http://localhost:9099/token"
export GOOGLE_USERINFO_URI="http://localhost:9099/userinfo"

# ── 가짜 구글 서버 ──────────────────────────────
cat <<'PYEOF' > /tmp/mockoauth.py
import os, json
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs
ADMIN = os.environ.get("ADMIN_EMAIL", "admin@codexray.com")
USERS = {
    "u_normal":   {"sub": "g-normal",   "email": "normal@codexray.com", "name": "노멀"},
    "u_admin":    {"sub": "g-admin",    "email": ADMIN,                 "name": "관리자"},
    "u_conflict": {"sub": "g-conflict", "email": "dupe@codexray.com",   "name": "충돌"},
}
class H(BaseHTTPRequestHandler):
    def _send(self, obj):
        body = json.dumps(obj).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def do_POST(self):     # /token : 받은 code를 그대로 access_token으로 echo
        n = int(self.headers.get("Content-Length", 0))
        form = parse_qs(self.rfile.read(n).decode())
        code = form.get("code", [""])[0]
        self._send({"access_token": code, "token_type": "Bearer", "expires_in": 3600})
    def do_GET(self):      # /userinfo : Bearer 토큰(=code)으로 유저 매핑
        token = self.headers.get("Authorization", "").replace("Bearer ", "")
        self._send(USERS.get(token, {"sub": "x", "email": "x@x.com", "name": "x"}))
    def log_message(self, *a): pass
HTTPServer(("127.0.0.1", 9099), H).serve_forever()
PYEOF
python3 /tmp/mockoauth.py &
MOCK_PID=$!
sleep 1

./gradlew bootRun --console=plain > /tmp/boot.log 2>&1 &
APP_PID=$!
for i in $(seq 1 60); do
  grep -q "Started BackendApplication" /tmp/boot.log 2>/dev/null && break
  grep -qiE "APPLICATION FAILED|BUILD FAILED" /tmp/boot.log 2>/dev/null && { echo "BOOT FAILED"; tail -40 /tmp/boot.log; kill $APP_PID $MOCK_PID 2>/dev/null; exit 1; }
  sleep 1
done

BASE=http://localhost:8080/api

gl() {  # google login with given code
  curl -s -X POST $BASE/auth/oauth/google -H "Content-Type: application/json" -d "{\"code\":\"$1\"}"
}

echo "=== 1) 신규 구글 로그인(u_normal) — 기대: 200 + accessToken ==="
RES=$(gl u_normal)
echo "$RES" | grep -o '"accessToken":"[^"]*"' >/dev/null && echo "  accessToken 발급됨 ✓" || echo "  실패! $RES"
echo "  유저 수: $($DB -tAc "SELECT count(*) FROM users;")  (기대: 1)"
echo "  provider/verified/pw-null: $($DB -tAc "SELECT provider||' / verified='||email_verified||' / pwNull='||(password IS NULL) FROM users WHERE email='normal@codexray.com';")"

echo ""
echo "=== 2) 같은 계정 재로그인(u_normal) — 기대: 200, 유저 수 그대로 1 ==="
gl u_normal | grep -o '"accessToken":"[^"]*"' >/dev/null && echo "  로그인 OK ✓"
echo "  유저 수: $($DB -tAc "SELECT count(*) FROM users;")  (기대: 1 — 중복생성 안 함)"

echo ""
echo "=== 3) 관리자 이메일 로그인(u_admin) — 기대: role=ADMIN ==="
gl u_admin >/dev/null
echo "  role: $($DB -tAc "SELECT role FROM users WHERE email='admin@codexray.com';")  (기대: ADMIN)"

echo ""
echo "=== 4) 이미 일반가입된 이메일과 충돌 — 기대: 409 EMAIL_DUPLICATED ==="
curl -s -o /dev/null -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"dupe@codexray.com","password":"password123","nickname":"기존회원"}'
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/auth/oauth/google -H "Content-Type: application/json" \
  -d '{"code":"u_conflict"}'

echo ""
echo "=== 5) code 없이 — 기대: 400 검증 실패 ==="
curl -s -w "HTTP %{http_code}\n" -o /dev/null -X POST $BASE/auth/oauth/google \
  -H "Content-Type: application/json" -d '{}'

kill $APP_PID $MOCK_PID 2>/dev/null
echo ""
echo "=== stopped ==="
