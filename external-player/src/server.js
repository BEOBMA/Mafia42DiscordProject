import express from "express";
import { config } from "./config.js";
import { DiscordPlayerService } from "./discordPlayerService.js";

const app = express();
app.use(express.json({ limit: "1mb" }));

const playerService = new DiscordPlayerService({
  token: config.playerBotToken
});

function parseBearer(headerValue) {
  if (!headerValue) return "";
  const [scheme, token] = headerValue.split(" ");
  if (scheme?.toLowerCase() !== "bearer") return "";
  return token?.trim() ?? "";
}

function authMiddleware(req, res, next) {
  const token = parseBearer(req.header("authorization"));
  if (!token || token !== config.wrapperAuthToken) {
    return res.status(401).json({ message: "Unauthorized" });
  }
  return next();
}

function validatePlayRequest(req, res, next) {
  const { guildId, voiceChannelId, source } = req.body ?? {};
  if (!guildId || !voiceChannelId || !source) {
    return res.status(400).json({
      message: "guildId, voiceChannelId, source are required"
    });
  }

  const isHttp = /^https?:\/\//i.test(source);
  if (!isHttp) {
    return res.status(400).json({
      message: "source must be an absolute http(s) URL"
    });
  }

  return next();
}

app.get("/health", (_req, res) => {
  return res.status(200).json({
    ok: true,
    mode: "discord-voice"
  });
});

app.get("/play", (_req, res) => {
  return res.status(405).json({
    message: "Method Not Allowed",
    hint: "Use POST /play with JSON body: { guildId, voiceChannelId, source }"
  });
});

app.post("/play", authMiddleware, validatePlayRequest, async (req, res) => {
  const { guildId, voiceChannelId, source } = req.body;

  try {
    const result = await playerService.play({ guildId, voiceChannelId, source });
    return res.status(202).json(result);
  } catch (error) {
    return res.status(500).json({
      message: "Playback dispatch failed",
      reason: error instanceof Error ? error.message : "unknown"
    });
  }
});

app.listen(config.port, () => {
  console.log(
    `[external-player] listening on :${config.port} (mode=discord-voice)`
  );
});

playerService.start().then(() => {
  console.log("[external-player] discord player bot login success");
}).catch((error) => {
  console.error("[external-player] discord player bot login failed:", error);
  process.exit(1);
});
