# 빌드 컨텍스트: src/ (Gradle 루트)
#   docker build -t bakku-5th-server src

# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle wrapper와 빌드 설정을 먼저 복사해 의존성 다운로드 레이어를 캐시한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version --no-daemon

COPY . .
RUN ./gradlew :app:bootJar --no-daemon

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# root가 아닌 전용 사용자로 실행
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/app/build/libs/*.jar app.jar

USER app
EXPOSE 8080

# 헬스 체크는 Kubernetes readiness/liveness probe가 담당한다
#   /actuator/health/readiness, /actuator/health/liveness
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
