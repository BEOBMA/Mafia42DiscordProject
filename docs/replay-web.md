# 웹 리플레이 아카이브

게임 종료 시 `data/replay-render-data`에 저장된 JSON은 웹 아카이브에서 자동으로 노출됩니다.

- `/`: 전체 리플레이 목록과 검색
- `/history/{uuid}`: 리플레이 상세 타임라인
- `/api/replays`: 목록 API
- `/api/replays/{uuid}`: 상세 API
- `/notepad`: 기존 게임 메모장

과거 스키마 1 파일에는 UUID가 없으므로 `guildId`와 `replayStartedAtMillis`를 조합한 안정적인 32자리 UUID를 자동으로 계산합니다. 새로 저장하는 파일에는 `replayUuid`가 함께 기록됩니다.

외부에서 접속할 주소는 `REPLAY_PUBLIC_BASE_URL`로 설정합니다. 필요하면 `WEB_HOST`와 `WEB_PORT`로 바인딩 주소와 포트도 변경할 수 있습니다.

```text
REPLAY_PUBLIC_BASE_URL=https://replay.example.com
WEB_HOST=127.0.0.1
WEB_PORT=8080
```

공개 주소를 설정하지 않으면 기본값은 `http://127.0.0.1:8080`입니다.
