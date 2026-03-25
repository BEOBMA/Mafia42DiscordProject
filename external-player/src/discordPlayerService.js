import { Client, GatewayIntentBits } from "discord.js";
import {
  AudioPlayerStatus,
  NoSubscriberBehavior,
  StreamType,
  VoiceConnectionStatus,
  createAudioPlayer,
  createAudioResource,
  entersState,
  getVoiceConnection,
  joinVoiceChannel
} from "@discordjs/voice";
import prism from "prism-media";

export class DiscordPlayerService {
  constructor({ token }) {
    this.token = token;
    this.client = new Client({
      intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildVoiceStates]
    });
    this.players = new Map();
  }

  async start() {
    await this.client.login(this.token);
  }

  async play({ guildId, voiceChannelId, source }) {
    const guild = await this.client.guilds.fetch(guildId);
    if (!guild) {
      throw new Error(`Guild not found: ${guildId}`);
    }

    const voiceChannel = await guild.channels.fetch(voiceChannelId);
    if (!voiceChannel || !voiceChannel.isVoiceBased()) {
      throw new Error(`Voice channel not found: ${voiceChannelId}`);
    }

    const connection = this.getOrCreateConnection({
      guildId,
      voiceChannelId,
      adapterCreator: guild.voiceAdapterCreator
    });

    await entersState(connection, VoiceConnectionStatus.Ready, 20_000);

    const player = this.getOrCreatePlayer(guildId, connection);
    const ffmpegErrorLogs = [];
    const transcoder = new prism.FFmpeg({
      args: [
        "-loglevel",
        "warning",
        "-i",
        source,
        "-f",
        "s16le",
        "-ar",
        "48000",
        "-ac",
        "2",
        "pipe:1"
      ],
      shell: false
    });
    transcoder.on("error", (error) => {
      console.error("[external-player] ffmpeg stream error:", error);
    });
    transcoder.stderr?.on("data", (chunk) => {
      const line = chunk.toString().trim();
      if (!line) return;
      ffmpegErrorLogs.push(line);
      if (ffmpegErrorLogs.length > 20) {
        ffmpegErrorLogs.shift();
      }
      console.warn(`[external-player] ffmpeg: ${line}`);
    });
    const resource = createAudioResource(transcoder, {
      inputType: StreamType.Raw,
      inlineVolume: true,
      metadata: {
        guildId,
        voiceChannelId,
        source
      }
    });
    resource.volume?.setVolume(1.0);

    player.play(resource);

    return {
      accepted: true,
      mode: "discord-voice",
      guildId,
      voiceChannelId,
      note: "Playback dispatched. Check external-player logs for ffmpeg/runtime errors."
    };
  }

  getOrCreateConnection({ guildId, voiceChannelId, adapterCreator }) {
    const existingConnection = getVoiceConnection(guildId);
    if (existingConnection) {
      if (existingConnection.joinConfig.channelId === voiceChannelId) {
        return existingConnection;
      }

      existingConnection.destroy();
    }

    return joinVoiceChannel({
      channelId: voiceChannelId,
      guildId,
      adapterCreator,
      selfDeaf: false,
      selfMute: false
    });
  }

  getOrCreatePlayer(guildId, connection) {
    const existing = this.players.get(guildId);
    if (existing) {
      existing.connection = connection;
      existing.connection.subscribe(existing.player);
      return existing.player;
    }

    const player = createAudioPlayer({
      behaviors: {
        noSubscriber: NoSubscriberBehavior.Play
      }
    });
    player.on("error", (error) => {
      console.error("[external-player] audio player error:", error);
    });
    player.on("stateChange", (oldState, newState) => {
      if (
        oldState.status !== newState.status &&
        (newState.status === AudioPlayerStatus.Idle ||
          newState.status === AudioPlayerStatus.Playing)
      ) {
        console.log(
          `[external-player] player state ${oldState.status} -> ${newState.status} (guild=${guildId})`
        );
      }
    });
    connection.subscribe(player);
    this.players.set(guildId, { player, connection });
    return player;
  }
}
