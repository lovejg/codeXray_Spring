# ─────────────────────────────────────────────────────────────
# 1단계 (build): Node 로 React 를 빌드해 정적파일(dist/) 을 만든다.
#   - 결과물(HTML/JS)은 CPU 와 상관없어서 만드는 컴퓨터의 CPU 로 돌린다(서버 Arm 흉내 불필요).
# ─────────────────────────────────────────────────────────────
FROM --platform=$BUILDPLATFORM node:20 AS build
WORKDIR /app

# 의존성 먼저 설치 (package*.json 만 먼저 복사하면 소스만 바뀔 때 이 레이어는 캐시 재사용)
COPY package.json package-lock.json ./
RUN npm ci

# 소셜 로그인 Client ID. VITE_ 값은 빌드할 때 번들 안에 박힌다(공개값이라 괜찮다. secret 은 백엔드에만).
# CI 가 --build-arg 로 넣는다. 비면 소셜 로그인 버튼이 안내 alert 만 띄운다
ARG VITE_GOOGLE_CLIENT_ID=
ARG VITE_NAVER_CLIENT_ID=
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID VITE_NAVER_CLIENT_ID=$VITE_NAVER_CLIENT_ID

# 소스 복사 후 프로덕션 빌드 → /app/dist
COPY . .
RUN npm run build

# ─────────────────────────────────────────────────────────────
# 2단계 (runtime): nginx 에 정적파일 + 우리 설정만 얹는다. 이 단계가 서버 CPU(Arm)용이다.
# ─────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# 우리 nginx 설정으로 기본 설정 교체
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 1단계에서 만든 정적파일을 nginx 가 서빙하는 폴더로 복사
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
# (nginx:alpine 기본 CMD 가 nginx 를 foreground 로 실행하므로 ENTRYPOINT 불필요)
