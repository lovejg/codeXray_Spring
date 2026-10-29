# ─────────────────────────────────────────────────────────────
# 1단계 (build): JDK 21 + Gradle 로 실행 가능한 jar 파일을 만든다.
#   - 서버는 Arm(linux/arm64)이고 이미지는 GitHub 의 x86 러너에서 만든다.
#     이 단계는 만드는 컴퓨터의 CPU($BUILDPLATFORM)로 돌린다. jar 는 CPU 와 상관없는 파일이라
#     Arm 흉내(QEMU)로 Gradle 을 돌릴 필요가 없다(흉내로 돌리면 몇 배 느리다).
#   - 테스트는 CI(GitHub Actions)에서 이미 돌리므로 여기선 -x test 로 건너뜀.
# ─────────────────────────────────────────────────────────────
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 빌드 설정만 먼저 복사해 의존성을 받아 둔다. 소스만 바뀌면 이 층을 다시 써서 의존성을 또 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

# 소스 복사 후 실행 가능한 jar 생성 → build/libs/backend-0.0.1-SNAPSHOT.jar
# (bootJar 만 돌리므로 -plain.jar 는 생기지 않아 아래 *.jar 가 파일 하나만 가리킨다)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────────────────────
# 2단계 (runtime): 가벼운 JRE 이미지에 jar 하나만 복사해 실행한다. 이 단계가 서버 CPU(Arm)용이다.
#   - JDK(개발도구 포함) 대신 JRE(실행만) 라 이미지가 훨씬 작다.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# root 로 돌리지 않는다. 앱이 뚫려도 컨테이너 안에서 할 수 있는 일이 줄어든다
RUN groupadd --system app && useradd --system --gid app app
# 1단계에서 만든 jar 만 가져온다 (버전명이 바뀌어도 되도록 와일드카드)
COPY --from=build /app/build/libs/*.jar app.jar
USER app

EXPOSE 8080
# 컨테이너에 준 메모리(compose 의 mem_limit)의 75% 까지 힙으로 쓴다. 나머지는 스레드, 메타데이터 같은 힙 밖 메모리 몫
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
