# jeju-localtime-api

제주 로컬타임 AI 백엔드 스캐폴드입니다. Spring Boot(Java 21) + Docker로 구성되어 있고,
GitHub 저장소를 Render에 연결하면 `main` 브랜치에 push할 때마다 Render가 Docker 이미지를
자동으로 빌드·배포합니다(별도의 GitHub Actions 설정 없이 CI/CD 완성).

## 구성

```
jeju-localtime-api/
├── build.gradle           # 의존성: web, actuator, data-jpa, postgresql
├── settings.gradle
├── Dockerfile              # 멀티스테이지 빌드 (gradle → eclipse-temurin JRE)
├── .dockerignore
├── render.yaml             # Render Blueprint: 웹서비스 + PostgreSQL DB 동시 정의
└── src/main/
    ├── java/com/jejulocaltime/api/
    │   ├── JejuLocaltimeApiApplication.java
    │   └── controller/PingController.java   # GET / 헬스 확인용
    └── resources/application.yml
```

- `GET /` : 서비스 상태 확인용 엔드포인트
- `GET /actuator/health` : Render 헬스체크가 사용하는 경로

## 1. 로컬에서 Docker로 먼저 확인

```bash
docker build -t jeju-localtime-api .
docker run -p 8080:8080 jeju-localtime-api
curl http://localhost:8080/actuator/health
```

DB 연결이 필요한 API를 테스트하려면 로컬 Postgres를 띄우고 `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` 환경변수를 `docker run -e ...`로 넘겨주면 됩니다. 기본값은
`localhost:5432/jeju_localtime` (postgres/postgres)로 되어 있습니다.

## 2. GitHub 저장소 생성 및 push

```bash
git init
git add .
git commit -m "chore: init jeju-localtime-api spring boot + docker scaffold"
git branch -M main
git remote add origin <내 GitHub 저장소 URL>
git push -u origin main
```

## 3. Render에서 Blueprint로 한 번에 배포

1. https://dashboard.render.com 접속 → **New +** → **Blueprint** 선택
2. 방금 push한 GitHub 저장소 연결 (최초 1회 GitHub 계정 연동 필요)
3. Render가 저장소 루트의 `render.yaml`을 자동으로 인식해서
   - `jeju-localtime-api` (Docker 웹 서비스)
   - `jeju-localtime-db` (관리형 PostgreSQL)
   두 개를 한 번에 생성하고, DB 접속 정보(`DB_HOST`, `DB_USER`, `DB_PASSWORD` 등)를
   웹 서비스 환경변수에 자동으로 주입합니다.
4. **Apply** 를 누르면 Dockerfile 기준으로 첫 빌드·배포가 시작됩니다.

Blueprint 없이 수동으로 하고 싶다면: 대시보드에서 **New + → Web Service** 로
Docker 런타임을 선택해 저장소를 연결하고, **New + → PostgreSQL** 로 DB를 별도로 만든 뒤
DB 정보를 Web Service의 Environment 탭에 직접 입력해도 됩니다.

## 4. 이후 배포 흐름 (CI/CD)

`render.yaml`에 `autoDeployTrigger: commit`으로 설정되어 있어서, 이후에는

```bash
git add .
git commit -m "feat: 상품 등록 API 추가"
git push origin main
```

이렇게 push만 하면 Render가 자동으로 감지해서 Dockerfile로 이미지를 새로 빌드하고
배포까지 끝냅니다. 별도 GitHub Actions 워크플로우는 필요 없습니다.

## 5. 무료 티어 유의사항

- **웹 서비스**: 15분간 요청이 없으면 슬립 상태로 전환되고, 다음 요청 시 재기동까지
  수십 초가 걸릴 수 있습니다.
- **PostgreSQL**: 무료 DB는 생성 후 **30일이 지나면 만료**되고, 이후 14일 유예기간 안에
  유료 플랜으로 전환하지 않으면 데이터가 삭제됩니다. 장기 운영 시에는 유료 플랜 전환이나
  Supabase/Neon 같은 만료 없는 무료 Postgres로의 이전을 고려하세요.

## 다음에 추가하면 좋은 것

- 기능명세서(SEL-01~06, BUY-01~06, AI-01~04)에 맞춘 Entity/Repository/Controller 구현
- `application.yml`의 `ddl-auto: update`는 개발 단계용입니다. 운영 전환 시 Flyway/Liquibase로
  마이그레이션 관리로 바꾸는 것을 권장합니다.

## AI Render 서비스 연결

무료 실행 시간 750시간을 BE와 AI가 공유하지 않도록 AI는 별도 Hobby 워크스페이스에
배포합니다. AI 배포를 먼저 완료한 다음 이 BE 서비스의 Environment 탭에 아래 값을 넣습니다.

```dotenv
AI_SERVICE_BASE_URL=https://jeju-localtime-ai.onrender.com
AI_SERVICE_API_KEY=<AI 서비스에 입력한 것과 같은 긴 임의 문자열>
```

`AI_SERVICE_BASE_URL` 끝에는 `/`를 붙이지 않습니다. 서로 다른 워크스페이스는 Render
사설망을 공유하지 않으므로 AI의 공개 `onrender.com` HTTPS 주소를 사용합니다.

현재 자동가격 스케줄러는 활성 상품을 10분마다 갱신합니다. 따라서 AI 무료 서비스가 15분
유휴 슬립에 들어가지 않고 월 약 720~744시간 실행되며, AI 워크스페이스에는 계속 실행되는
다른 무료 Web Service를 추가하지 않는 것이 안전합니다.

