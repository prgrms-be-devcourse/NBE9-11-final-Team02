# GraalVM JDK 24 기반 Gradle 이미지 (빌드 전용, 최종 이미지에 포함되지 않음)
FROM gradle:jdk24-graal AS builder

WORKDIR /app

# 의존성 파일만 먼저 복사하여 Docker 레이어 캐시 활용
# → 소스 변경 시에도 의존성 레이어는 재사용되어 빌드 속도 향상
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon

# 소스 복사
COPY src ./src

# --no-daemon: 컨테이너 내 일회성 빌드이므로 데몬 불필요
RUN gradle build --no-daemon
RUN rm -f /app/build/libs/*-plain.jar

# =========================
# 2️⃣ Run Stage
# =========================
# Oracle GraalVM JDK 24 런타임 (Graal JIT 컴파일러 포함)
FROM container-registry.oracle.com/graalvm/jdk:24

WORKDIR /app

# 멀티스테이지 빌드: builder에서 생성된 JAR만 복사 → 최종 이미지 경량화
COPY --from=builder /app/build/libs/*.jar app.jar

RUN microdnf install -y curl && microdnf clean all

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ── t3.micro (1 vCPU, 1GB RAM + 4GB swap) 환경 최적화 JVM 옵션 ──
#
# -XX:+UseJVMCICompiler
#   GraalVM Graal JIT 컴파일러 사용 (C2 대체)
#   → 워밍업 후 더 나은 최적화 코드 생성, peak 성능 향상
#
# -XX:+UseSerialGC
#   Serial GC 사용 (단일 스레드 GC)
#   → 1 vCPU 환경에서 GC 스레드 오버헤드 제거, 메모리 사용량 최소화
#   → G1/ZGC 대비 힙이 작을 때(~512MB 이하) 오히려 효율적
#
# -XX:MaxRAMPercentage=55
#   컨테이너 인식 메모리의 55%를 최대 힙으로 설정
#   → 1GB 기준 약 564MB 힙 (나머지는 메타스페이스, 스레드 스택, OS 등에 할당)
#
# -XX:InitialRAMPercentage=25
#   시작 시 힙을 컨테이너 메모리의 25%로 할당
#   → 약 256MB로 시작하여 불필요한 초기 메모리 점유 방지
#
# -XX:+ExitOnOutOfMemoryError
#   OOM 발생 시 즉시 프로세스 종료
#   → 컨테이너 오케스트레이션(Docker restart)에 의한 자동 복구 유도
#   → OOM 상태에서 좀비처럼 떠있는 것 방지
#
# -XX:+AlwaysPreTouch
#   JVM 시작 시 힙 메모리 전체를 OS에 미리 커밋(물리 페이지 할당)
#   → 런타임 중 페이지 폴트 지연 방지, 응답 시간 안정화
#   → 단, 초기 메모리 사용량이 즉시 올라감 (swap 4GB 있으므로 허용 가능)
#
# -Dspring.profiles.active=prod
#   Spring 프로파일: 운영 환경 설정 활성화
#
ENTRYPOINT ["java", \
"-XX:+UseJVMCICompiler", \
"-XX:+UseSerialGC", \
"-XX:MaxRAMPercentage=55", \
"-XX:InitialRAMPercentage=25", \
"-XX:+ExitOnOutOfMemoryError", \
"-XX:+AlwaysPreTouch", \
"-Dspring.profiles.active=prod", \
"-jar", \
"app.jar"]