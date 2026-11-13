# Bakku-5th-Server
JECT 5기 가챠교환 플랫폼 '바꾸' Server Repository

## 로컬 실행

로컬은 PostgreSQL · Redis · ES를 Docker로 띄워 각자 환경에서 테스트합니다. dev DB는 쓰지 않습니다.

| 서비스 | 이미지 | 주소 |
|---|---|---|
| PostgreSQL | `postgres:16` | `localhost:5432` (DB `bakku_local`, 계정 `bakku` / `bakku`) |
| Redis | `redis:8-alpine` | `localhost:6379` |
| Elasticsearch | `elasticsearch:9.5.5` | `localhost:9200` |

### 준비

1. Docker Desktop 설치
2. `src/.env.example`을 `src/.env`로 복사 (값은 그대로 쓰면 됨)

### 실행

```bash
docker compose -f src/docker-compose.yml up -d
```

```bash
cd src && ./gradlew :app:bootRun
```

- `docker compose -f src/docker-compose.yml ps`에서 세 서비스가 `healthy`인지 확인합니다.
- 기동 시 Flyway가 로컬 DB에 테이블을 만듭니다.
- Swagger: http://localhost:8080/swagger-ui/index.html

### 종료 · 초기화

```bash
docker compose -f src/docker-compose.yml down
```

DB · ES 데이터까지 지우려면 `-v`를 붙입니다. 머지 전 마이그레이션 파일을 고쳐 Flyway 체크섬 에러가 날 때 사용합니다.

```bash
docker compose -f src/docker-compose.yml down -v
```

### 문제 해결

- 포트 충돌: PC에 PostgreSQL(5432) · Redis(6379)가 이미 실행 중이면 끄고 다시 띄웁니다.
- 메모리 부족: ES를 빼고 띄울 수 있습니다. `docker compose -f src/docker-compose.yml up -d postgres redis`
