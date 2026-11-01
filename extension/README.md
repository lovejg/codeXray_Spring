# codeXray Saver (브라우저 확장)

프로그래머스 문제 페이지에서 풀이를 codeXray에 원클릭 저장하는 크롬 확장(Manifest V3).

## 설치 (개발자 모드)
1. `chrome://extensions` → 개발자 모드 켜기
2. "압축해제된 확장 프로그램을 로드" → 이 `extension/` 폴더 선택
3. 확장 아이콘 → codeXray 계정으로 로그인
4. 프로그래머스 문제 페이지에서 우측 `❯ 저장` 버튼 사용

## 구성
- `manifest.json` — 권한/스크립트 선언
- `config.js` — API/앱 주소
- `background.js` — 저장 오케스트레이션(추출·매칭·저장), 토큰 자동 갱신
- `content.js` — 페이지에 버튼 주입 + 저장 UX
- `popup.html` / `popup.js` — 로그인 팝업

## 주소
운영 사이트(https://codexray.org)를 가리킵니다. 화면과 API가 같은 주소(nginx가 `/api`만 백엔드로 넘김)라 API 도메인을 따로 두지 않습니다.

로컬 백엔드로 개발할 때는 **2곳**을 함께 바꿉니다(백엔드 CORS 설정은 아래 "확장 ID와 CORS"):
```js
// config.js
var CODEXRAY_API = 'http://localhost:8080/api'
var CODEXRAY_APP_ORIGIN = 'http://localhost:5173'
```
```json
// manifest.json host_permissions
"http://localhost:8080/*"
```

## 확장 ID와 CORS
확장이 보내는 요청에는 `Origin: chrome-extension://<확장 ID>`가 붙고, 백엔드는 허용 목록(`APP_CORS_ORIGINS`)에 없는 출처를 403으로 막습니다.
압축해제 로드는 보통 폴더 경로에 따라 ID가 바뀌는데, `manifest.json`의 `key`(공개키)로 **`dfhfbggmaigglacilpailkkdfplheofp`에 고정**해 두었습니다. 운영은 `deploy/compose.yml`에서 이 ID 하나만 허용합니다(모든 확장을 열면 아무 확장이나 refresh 쿠키로 토큰을 받아 갈 수 있어서).
로컬 백엔드로 개발할 때는 `backend/.env`에 `APP_CORS_ORIGINS=chrome-extension://dfhfbggmaigglacilpailkkdfplheofp`를 넣습니다.
`key`를 바꾸거나 지우면 ID가 달라져 로그인이 403이 됩니다. 크롬 웹 스토어에 올릴 때는 스토어가 정한 ID로 허용 목록을 바꿉니다.

## 알아둘 점
- 팝업 로그인은 이메일·비밀번호 방식만 됩니다. Google/네이버로 가입한 계정은 비밀번호가 없어서 확장에 로그인할 수 없습니다.
- 웹에서 비밀번호를 바꾸면 확장의 로그인도 끊깁니다. 다시 로그인하면 됩니다.
