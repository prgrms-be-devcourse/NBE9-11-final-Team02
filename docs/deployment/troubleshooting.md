# 배포 운영 트러블슈팅

## 프론트엔드 환경변수 변경 시 컨테이너가 갱신되지 않는 경우

`docker-compose.yaml`의 frontend 환경변수(예: `BACKEND_API_URL`)만 변경한 경우,
compose 파일은 서버에 갱신되더라도 기존 frontend 컨테이너가 재생성되지 않아
이전 환경변수를 계속 사용하는 경우가 있다.

현재 배포 구조에서는

- 백엔드 CD는 `docker-compose.yaml`만 갱신하고 frontend 컨테이너는 재생성하지 않는다.
- 프론트엔드 CD는 `docker-compose.yaml` 변경을 트리거로 인식하지 않는다.

따라서 frontend 환경변수를 변경한 경우에는 아래 명령으로 컨테이너를 수동 재생성한다.

```bash
docker-compose \
  -f /home/ec2-user/app/docker-compose.yaml \
  --profile prod \
  up -d --force-recreate --no-deps frontend_1
```

적용 확인:

```bash
docker exec frontend_1 printenv | grep BACKEND_API_URL
```

## 프론트엔드 SSR에서 백엔드 호출 시 Host 헤더 문제

`BACKEND_API_URL`에 언더스코어가 포함된 컨테이너명(`nginx_1`)을 쓰면, SSR 요청의
Host 헤더로 전달될 때 Tomcat이 유효하지 않은 Host로 판단해 요청을 거부한다.

Docker Compose 서비스명(`nginx`)을 사용한다.
