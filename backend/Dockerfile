# ─────────────────────────────────────────────────────────────
# 1단계 (build): JDK 21 + Gradle 로 실행 가능한 jar 파일을 만든다.
#   - 이 단계는 무겁지만, 결과물(jar)만 다음 단계로 넘기고 통째로 버린다.
#   - 테스트는 CI(GitHub Actions)에서 이미 돌리므로 여기선 -x test 로 건너뜀.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 소스 전체 복사 후 실행 가능한 jar 생성 → build/libs/backend-0.0.1-SNAPSHOT.jar
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────────────────────
# 2단계 (runtime): 가벼운 JRE 이미지에 jar 하나만 복사해 실행한다.
#   - JDK(개발도구 포함) 대신 JRE(실행만) 라 이미지가 훨씬 작다.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 1단계에서 만든 jar 만 가져온다 (버전명이 바뀌어도 되도록 와일드카드)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
