# 외부 오디오 재생 API 연동 가이드

이 프로젝트의 `!debug playsound <URL>` 기능은 더 이상 디스코드 사운드보드를 만들지 않습니다.  
대신 **외부 오디오 재생 API**를 호출해서, 외부 플레이어가 디스코드 음성 채널에서 소리를 재생하도록 동작합니다.

## 1) 어디서 API를 가져와야 하나요?

이 API는 디스코드 공식에서 제공하는 고정 서비스가 아니라, 아래 중 하나를 직접 준비해야 합니다.

- 이미 운영 중인 사내/개인 오디오 플레이어 서버
- Lavalink + 별도 래퍼 서버(HTTP 엔드포인트 제공)
- 직접 만든 경량 API 서버(Node/Kotlin/Python 등)

즉, 봇 코드만으로는 완성되지 않고, **별도의 외부 재생 서버 주소**가 필요합니다.

---

## 1-1) Lavalink 기준 권장 구성 (상세)

가장 운영이 쉬운 패턴은 아래 2계층입니다.

1. **Lavalink 서버**: 실제 음성 재생 엔진
2. **Wrapper API 서버**: 현재 봇이 호출하는 `POST /play`를 받아 Lavalink REST/WebSocket으로 전달

이 프로젝트는 Wrapper API만 호출하므로, Lavalink를 직접 몰라도 붙일 수 있습니다.

### 구성도

`Mafia42DiscordProject` → `Wrapper API (/play)` → `Lavalink` → `Discord Voice`

### 왜 Wrapper를 두나요?

- 현재 봇 코드의 요청 스펙을 고정(`guildId`, `voiceChannelId`, `source`)해서 유지 가능
- 인증/로깅/재시도/큐 정책을 게임 서버와 분리 가능
- Lavalink 버전 업그레이드 시 봇 코드 수정 최소화

---

## 1-2) Lavalink 빠른 시작 (Docker)

### A. `application.yml` 예시

```yaml
server:
  port: 2333

lavalink:
  server:
    password: "change-me-strong-password"
    sources:
      youtube: false
      bandcamp: true
      soundcloud: true
      twitch: true
      vimeo: true
      http: true
    bufferDurationMs: 400
    frameBufferDurationMs: 5000
```

레포에 기본 템플릿 파일을 추가해두었습니다:  
`deploy/lavalink/application.yml`

### B. `docker-compose.yml` 예시

```yaml
services:
  lavalink:
    image: ghcr.io/lavalink-devs/lavalink:4
    container_name: lavalink
    restart: unless-stopped
    ports:
      - "2333:2333"
    volumes:
      - ./application.yml:/opt/Lavalink/application.yml:ro
```

레포에 기본 템플릿 파일을 추가해두었습니다:  
`deploy/lavalink/docker-compose.yml`

실행:

```bash
cd deploy/lavalink
docker compose up -d
```

헬스 확인(예시):

```bash
curl -i http://localhost:2333/version
```

> 주의: Lavalink API 스펙/엔드포인트는 버전에 따라 차이가 있을 수 있으니, 실제 운영 시 사용 버전의 공식 문서를 기준으로 Wrapper 구현을 맞춰주세요.

## 2) 봇 환경 변수 설정

아래 환경 변수를 런타임에 설정해야 합니다.

- `EXTERNAL_AUDIO_PLAYER_URL` (필수)
  - 외부 API 베이스 URL
  - 예: `https://audio-player.example.com`
- `EXTERNAL_AUDIO_PLAYER_PATH` (선택, 기본값: `/play`)
  - 재생 요청 경로
  - 예: `/v1/audio/play`
- `EXTERNAL_AUDIO_PLAYER_TOKEN` (선택)
  - 있으면 `Authorization: Bearer <token>` 헤더로 전송

## 3) 봇이 보내는 요청 스펙

- Method: `POST`
- URL: `${EXTERNAL_AUDIO_PLAYER_URL}${EXTERNAL_AUDIO_PLAYER_PATH}`
- Headers:
  - `Content-Type: application/json`
  - (선택) `Authorization: Bearer <EXTERNAL_AUDIO_PLAYER_TOKEN>`
- JSON Body:

```json
{
  "guildId": "123456789012345678",
  "voiceChannelId": "234567890123456789",
  "source": "https://example.com/sound.mp3"
}
```

### 응답 규칙

- `2xx`면 성공으로 처리
- `2xx`가 아니면 실패로 처리하며, 봇 로그/응답에 상태 코드와 본문이 포함됩니다.

## 4) 외부 API 구현 예시 (Node.js)

아래는 동작 개념을 보여주는 최소 예시입니다. 실제 운영에서는 인증, 입력 검증, 에러 처리, 재시도 로직을 강화하세요.

레포에는 위 개념을 바로 실행해볼 수 있는 Wrapper 구현이 `external-player/`에 포함되어 있습니다.
해당 구현은 Lavalink 없이도 Discord Voice + ffmpeg로 바로 재생할 수 있습니다.

```js
import express from "express";

const app = express();
app.use(express.json());

app.post("/play", async (req, res) => {
  const { guildId, voiceChannelId, source } = req.body ?? {};
  if (!guildId || !voiceChannelId || !source) {
    return res.status(400).json({ message: "guildId, voiceChannelId, source are required" });
  }

  // TODO:
  // 1) 외부 플레이어(예: Lavalink/자체 Voice Client)에 재생 요청
  // 2) 음성 채널 접속/이동
  // 3) source URL 스트리밍 재생
  // 4) 완료/실패 상태를 적절히 반환
  return res.status(202).json({ message: "accepted" });
});

app.listen(8080, () => {
  console.log("External audio player API listening on :8080");
});
```

---

## 4-1) Wrapper API가 Lavalink와 연동할 때 필수 처리

`POST /play` 수신 후 Wrapper에서 보통 아래 순서로 처리합니다.

1. `guildId` 기준 플레이어 세션 확인/생성
2. bot(재생용 봇)을 `voiceChannelId`로 이동
3. `source`를 Lavalink 트랙 로드 API로 조회
4. 트랙/큐에 삽입 후 재생 시작
5. 성공 시 `2xx`, 실패 시 명확한 상태코드와 메시지 반환

### 권장 에러 매핑

- 400: `guildId/voiceChannelId/source` 누락, URL 형식 오류
- 401/403: 토큰 인증 실패
- 404: 길드/채널/트랙 조회 실패
- 409: 이미 다른 채널에서 강제 점유 중(정책상 이동 불가)
- 429: 과도한 재생 요청(레이트 제한)
- 500+: Lavalink 또는 내부 플레이어 장애

### 권장 운영 정책

- 길드 단위 재생 큐(선입선출)
- 동일 `source` 짧은 시간 중복 요청 디바운스
- 최대 재생 길이 제한(예: 30초~2분)
- 길드별 동시 재생 1개 제한
- 타임아웃(트랙 로드/채널 조인/재생 시작 각각 분리)

---

## 4-2) Wrapper API 환경 변수 예시

Wrapper 서버(별도 프로젝트)에서 자주 쓰는 변수 예시:

- `LAVALINK_URL=http://lavalink:2333`
- `LAVALINK_PASSWORD=change-me-strong-password`
- `PLAYER_BOT_TOKEN=<재생용 디스코드 봇 토큰>`
- `PLAYER_BOT_USER_ID=<재생용 디스코드 봇 ID>`
- `WRAPPER_AUTH_TOKEN=<Mafia42DiscordProject와 공유할 Bearer 토큰>`

그리고 이 프로젝트에는 아래처럼 매핑합니다.

- `EXTERNAL_AUDIO_PLAYER_URL=https://wrapper.example.com`
- `EXTERNAL_AUDIO_PLAYER_PATH=/play`
- `EXTERNAL_AUDIO_PLAYER_TOKEN=<WRAPPER_AUTH_TOKEN>`

## 5) 점검 체크리스트

- 봇과 외부 플레이어가 같은 길드에 존재하는지
- 외부 플레이어(또는 해당 봇)가 음성 채널 접속/발화 권한이 있는지
- `source` URL이 외부 서버에서 실제로 접근 가능한지 (방화벽, 서명 URL 만료 여부)
- `EXTERNAL_AUDIO_PLAYER_URL/PATH/TOKEN` 값이 배포 환경과 일치하는지
