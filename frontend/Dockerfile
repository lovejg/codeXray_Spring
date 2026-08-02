# ─────────────────────────────────────────────────────────────
# 1단계 (build): Node 로 React 를 빌드해 정적파일(dist/) 을 만든다.
# ─────────────────────────────────────────────────────────────
FROM node:20 AS build
WORKDIR /app

# 의존성 먼저 설치 (package*.json 만 먼저 복사하면 소스만 바뀔 때 이 레이어는 캐시 재사용)
COPY package.json package-lock.json ./
RUN npm ci

# 소스 복사 후 프로덕션 빌드 → /app/dist
COPY . .
RUN npm run build

# ─────────────────────────────────────────────────────────────
# 2단계 (runtime): nginx 에 정적파일 + 우리 설정만 얹는다.
# ─────────────────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# 우리 nginx 설정으로 기본 설정 교체
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 1단계에서 만든 정적파일을 nginx 가 서빙하는 폴더로 복사
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
# (nginx:alpine 기본 CMD 가 nginx 를 foreground 로 실행하므로 ENTRYPOINT 불필요)
