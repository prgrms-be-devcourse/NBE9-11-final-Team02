# Prometheus/Grafana 모니터링 사용 가이드

## 1. 목적

Spring Boot Actuator, Prometheus, Grafana를 이용해 애플리케이션 상태와 성능 지표를 확인한다.

이 구성을 통해 다음과 같은 정보를 볼 수 있다.

- 애플리케이션 생존 여부
- HTTP 요청 수
- API 응답 상태 코드
- API 응답 시간
- JVM 메모리 사용량
- GC 관련 지표
- HikariCP DB 커넥션풀 지표

운영 중 장애나 성능 저하가 발생했을 때, 어떤 API에서 문제가 발생했는지 빠르게 확인하기 위한 기본 모니터링 환경이다.

## 2. 구성 요소

```text
Spring Boot Actuator
        |
        | /actuator/prometheus
        v
Prometheus
        |
        v
Grafana
```

### Spring Boot Actuator

Spring Boot 애플리케이션의 상태와 메트릭을 외부에 노출한다.

현재 노출된 주요 엔드포인트는 다음과 같다.

```text
/actuator/health
/actuator/prometheus
```

### Micrometer Prometheus Registry

Spring Boot 내부 메트릭을 Prometheus가 읽을 수 있는 형식으로 변환한다.

### Prometheus

Spring Boot의 `/actuator/prometheus` 엔드포인트를 주기적으로 호출해서 메트릭을 수집한다.

현재 설정 파일은 다음 위치에 있다.

```text
monitoring/prometheus/prometheus.yml
```

### Grafana

Prometheus에 저장된 메트릭을 대시보드나 쿼리 화면으로 시각화한다.

## 3. 실행 방법

### 3.1 Spring Boot 서버 실행

IntelliJ에서 애플리케이션을 실행하거나, 터미널에서 다음 명령어를 실행한다.

```bash
./gradlew bootRun
```

현재 로컬 서버 포트는 `8090`이다.

서버가 정상 실행되면 다음 주소로 health check를 확인할 수 있다.

```text
http://localhost:8090/actuator/health
```

### 3.2 Prometheus/Grafana 실행

```bash
docker compose up -d prometheus grafana
```

컨테이너 상태 확인:

```bash
docker compose ps
```

다음 두 컨테이너가 `Up` 상태이면 정상이다.

```text
prometheus_1
grafana_1
```

## 4. 접속 주소

### Prometheus

```text
http://localhost:9090
```

### Grafana

```text
http://localhost:3000
```

Grafana 기본 계정:

```text
ID: admin
PW: admin
```

## 5. Prometheus 확인 방법

Prometheus에 접속한 뒤 다음 메뉴로 이동한다.

```text
Status -> Target health
```

또는 버전에 따라:

```text
Status -> Targets
```

`spring-boot` job이 `UP` 상태이면 Prometheus가 Spring Boot 메트릭을 정상 수집하고 있는 것이다.

현재 Prometheus는 Docker 컨테이너 내부에서 로컬 Spring Boot 서버를 바라보기 위해 다음 주소를 사용한다.

```text
host.docker.internal:8090
```

## 6. Grafana 설정 방법

### 6.1 Prometheus Data Source 추가

Grafana에 접속한 뒤 다음 메뉴로 이동한다.

```text
Connections -> Data sources -> Add data source -> Prometheus
```

URL에는 다음 값을 입력한다.

```text
http://prometheus:9090
```

주의할 점:

```text
올바른 값: http://prometheus:9090
잘못된 값: http://localhost:9090
잘못된 값: http://http/localhost:9090
```

Grafana도 Docker 컨테이너 안에서 실행되기 때문에, Grafana 기준의 `localhost`는 Prometheus가 아니라 Grafana 자기 자신이다.
같은 Docker Compose 네트워크 안에서는 서비스 이름인 `prometheus`로 접근해야 한다.

입력 후 `Save & test`를 클릭한다.

## 7. Grafana Explore에서 확인할 쿼리

Grafana 왼쪽 메뉴에서 `Explore`로 이동한 뒤, Data source를 Prometheus로 선택한다.

### 7.1 Spring Boot 수집 상태 확인

```promql
up
```

정상 예시:

```text
up{instance="host.docker.internal:8090", job="spring-boot"} 1
```

`1`이면 수집 성공, `0`이면 수집 실패다.

### 7.2 JVM 메모리 사용량

```promql
jvm_memory_used_bytes
```

Heap, Non-Heap, Metaspace 등의 메모리 사용량을 확인할 수 있다.

### 7.3 HTTP 요청 수

```promql
http_server_requests_seconds_count
```

API별 요청 수, HTTP Method, 상태 코드, 성공/실패 여부를 확인할 수 있다.

특정 API만 보고 싶다면 다음처럼 조회한다.

```promql
http_server_requests_seconds_count{uri="/api/v1/matches"}
```

### 7.4 평균 응답 시간

```promql
rate(http_server_requests_seconds_sum{uri="/api/v1/matches"}[1m])
/
rate(http_server_requests_seconds_count{uri="/api/v1/matches"}[1m])
```

### 7.5 p95 응답 시간

```promql
histogram_quantile(
  0.95,
  sum(rate(http_server_requests_seconds_bucket{uri="/api/v1/matches"}[1m])) by (le)
)
```

### 7.6 5xx 에러율

```promql
sum(rate(http_server_requests_seconds_count{uri="/api/v1/matches", status=~"5.."}[1m]))
/
sum(rate(http_server_requests_seconds_count{uri="/api/v1/matches"}[1m]))
```

### 7.7 DB 커넥션풀

```promql
hikaricp_connections_active
```

만약 조회되지 않으면 다음 쿼리로 실제 메트릭 이름을 검색한다.

```promql
{__name__=~".*hikari.*"}
```

## 8. API 요청 지표 확인 예시

매칭 목록 조회 API를 호출한다.

```text
GET http://localhost:8090/api/v1/matches
```

Grafana Explore에서 다음 쿼리를 실행한다.

```promql
http_server_requests_seconds_count{uri="/api/v1/matches"}
```

정상 수집 예시:

```text
method="GET"
outcome="SUCCESS"
status="200"
uri="/api/v1/matches"
```

이 값이 보이면 Prometheus가 실제 API 요청 지표를 정상 수집하고 있는 것이다.

## 9. 대시보드 구성

처음에는 직접 대시보드를 만들기보다 Grafana Dashboard Import를 사용할 수 있다.

```text
Dashboards -> New -> Import
```

Micrometer/JVM 계열 대시보드를 import한 뒤, Prometheus datasource를 선택하면 JVM, GC, HTTP 관련 지표를 화면으로 확인할 수 있다.

팀에서 필요한 경우 다음 지표를 별도 패널로 구성하면 좋다.

- `/api/v1/matches` 요청 수
- `/api/v1/matches` 평균 응답 시간
- `/api/v1/matches` p95 응답 시간
- 4xx/5xx 에러율
- JVM Heap 사용량
- HikariCP active connection
