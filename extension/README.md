# codeXray Saver (브라우저 확장)

프로그래머스 문제 페이지에서 풀이를 codeXray에 원클릭 저장하는 크롬 확장(Manifest V3).

## 설치 (개발자 모드)
1. `chrome://extensions` → 개발자 모드 켜기
2. "압축해제된 확장 프로그램을 로드" → 이 `extension/` 폴더 선택
3. 확장 아이콘 → codeXray 계정으로 로그인
4. 프로그래머스 문제 페이지에서 우측 `❯ 저장` 버튼 사용

## 구성
- `manifest.json` — 권한/스크립트 선언
- `config.js` — **API/앱 주소 (배포 시 여기 수정)**
- `background.js` — 저장 오케스트레이션(추출·매칭·저장), 토큰 자동 갱신
- `content.js` — 페이지에 버튼 주입 + 저장 UX
- `popup.html` / `popup.js` — 로그인 팝업

## 배포용 주소 변경
지금 `config.js`는 로컬(`localhost:8080`, `localhost:5173`)을 가리킵니다. 운영 사이트(https://codexray.org)에 저장하려면 **2곳만** 수정:
1. `config.js` 의 `CODEXRAY_API`, `CODEXRAY_APP_ORIGIN`
2. `manifest.json` 의 `host_permissions` (사이트 도메인 추가)

운영은 화면과 API가 같은 주소(nginx가 `/api`만 백엔드로 넘김)라 API 도메인을 따로 두지 않습니다:
```js
// config.js
var CODEXRAY_API = 'https://codexray.org/api'
var CODEXRAY_APP_ORIGIN = 'https://codexray.org'
```
```json
// manifest.json host_permissions
"https://codexray.org/*"
```
