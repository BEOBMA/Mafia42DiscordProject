# external-player (Node + Discord Voice API)

`Mafia42DiscordProject`가 호출하는 외부 오디오 Wrapper API의 **실재생 가능한 기본 구현**입니다.

## 제공 엔드포인트

- `GET /health`
- `GET /play` (`405 Method Not Allowed`, 안내용 응답)
- `POST /play`
  - Header: `Authorization: Bearer <WRAPPER_AUTH_TOKEN>`
  - Body:
    ```json
    {
      "guildId": "123",
      "voiceChannelId": "456",
      "source": "https://example.com/audio.mp3"
    }
    ```

## 빠른 실행

```bash
cd external-player
cp .env.example .env
npm install
npm start
```

추가 요구사항:

- 서버에 `ffmpeg`가 설치되어 있어야 합니다.
- `PLAYER_BOT_TOKEN`은 음성 채널 접속 권한이 있는 봇 토큰이어야 합니다.

## 현재 동작

- `/play` 요청이 오면 재생용 봇이 `voiceChannelId`에 입장
- `source`(http/https URL)를 ffmpeg로 디코딩해 디스코드 음성으로 재생
- 성공 시 `202` 반환

## 현재 봇(Mafia42DiscordProject)과 연동 환경변수

`Mafia42DiscordProject`:

- `EXTERNAL_AUDIO_PLAYER_URL=http://localhost:8080`
- `EXTERNAL_AUDIO_PLAYER_PATH=/play`
- `EXTERNAL_AUDIO_PLAYER_TOKEN=<WRAPPER_AUTH_TOKEN>`
