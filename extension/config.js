// API/앱 주소. 로컬 백엔드로 개발할 때는 이 두 값과 manifest.json 의 host_permissions 를 함께 localhost 로 바꾼다
// (API 'http://localhost:8080/api', 앱 'http://localhost:5173', 권한 'http://localhost:8080/*').
// var 로 선언 → 팝업/콘텐트스크립트/서비스워커(importScripts) 전역에서 공유됨.
var CODEXRAY_API = 'https://codexray.org/api'
var CODEXRAY_APP_ORIGIN = 'https://codexray.org'
