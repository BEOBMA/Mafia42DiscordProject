# 웹 리플레이 아카이브

게임 종료 시 `data/replay-render-data`에 저장된 JSON은 웹 아카이브에서 자동으로 노출됩니다.

- `/`: 전체 리플레이 목록과 검색
- `/history/{uuid}`: 리플레이 상세 타임라인
- `/api/replays`: 목록 API
- `/api/replays/{uuid}`: 상세 API
- `/notepad`: 기존 게임 메모장

과거 스키마 1 파일에는 UUID가 없으므로 `guildId`와 `replayStartedAtMillis`를 조합한 안정적인 32자리 UUID를 자동으로 계산합니다. 새로 저장하는 파일에는 `replayUuid`가 함께 기록됩니다.

메모장 명령어와 게임 종료 후 전송되는 리플레이 링크는 모두 `WEB_PUBLIC_URL` 하나를 기준으로 생성합니다. 외부 주소가 바뀌면 이 환경 변수만 변경하고 봇을 다시 시작하면 두 링크에 함께 반영됩니다. `WEB_HOST`와 `WEB_PORT`는 로컬 웹 서버의 바인딩 주소에만 사용됩니다.

```text
WEB_HOST=127.0.0.1
WEB_PORT=8080
WEB_PUBLIC_URL=https://example.trycloudflare.com
```

`WEB_PUBLIC_URL`에는 Cloudflare Tunnel 등에서 발급받은 외부 HTTPS 주소를 지정합니다. 공개 주소를 설정하지 않으면 기본값은 `http://127.0.0.1:8080`입니다.
