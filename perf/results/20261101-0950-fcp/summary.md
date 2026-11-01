# 화면 첫 로딩 측정 결과

perf/run-frontend.sh 가 원본(fcp.json)에서 만든 표. 조건: 느린 4G(150ms·1.6Mbps/750kbps), CPU 4배 감속, 캐시 없음, 5회 중앙값.

| 경우 | 화면 | 받은 JS | JS 압축 | FCP | LCP |
|---|---|---|---|---|---|
| ① 고치기 전 | `/` | 1,741 KB (1개) | none | 9,785 ms | 9,888 ms |
| ① 고치기 전 | `/problems` | 1,741 KB (1개) | none | 9,784 ms | 9,881 ms |
| ② gzip | `/` | 570 KB (1개) | gzip | 3,569 ms | 3,678 ms |
| ② gzip | `/problems` | 570 KB (1개) | gzip | 3,566 ms | 3,665 ms |
| ③ gzip + 화면 나누기 | `/` | 111 KB (1개) | gzip | 1,034 ms | 1,137 ms |
| ③ gzip + 화면 나누기 | `/problems` | 121 KB (11개) | gzip/none | 1,042 ms | 1,485 ms |
| 운영 경로 재현: 고치기 전(HTTP/1.0 프록시, 압축 빠짐) | `/` | 340 KB (1개) | none | 2,405 ms | 2,511 ms |
| 운영 경로 재현: 고치기 전(HTTP/1.0 프록시, 압축 빠짐) | `/problems` | 355 KB (11개) | none | 2,402 ms | 2,856 ms |
| 운영 경로 재현: 고친 뒤 | `/` | 111 KB (1개) | gzip | 1,040 ms | 1,142 ms |
| 운영 경로 재현: 고친 뒤 | `/problems` | 121 KB (11개) | gzip/none | 1,034 ms | 1,477 ms |

→ 메인 FCP 9.8초 → 1.0초 (9.5배), 받은 JS 1,741 KB → 111 KB
