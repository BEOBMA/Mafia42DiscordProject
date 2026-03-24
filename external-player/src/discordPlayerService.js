import { Client, GatewayIntentBits } from "discord.js";
import {
  AudioPlayerStatus,
  NoSubscriberBehavior,
  StreamType,
  VoiceConnectionStatus,
  createAudioPlayer,
  createAudioResource,
  entersState,
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

    const connection = joinVoiceChannel({
      channelId: voiceChannelId,
      guildId,
      adapterCreator: guild.voiceAdapterCreator,
      selfDeaf: false,
      selfMute: false
    });

    await entersState(connection, VoiceConnectionStatus.Ready, 20_000);

    const player = this.getOrCreatePlayer(guildId, connection);
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
    const resource = createAudioResource(transcoder, {
      inputType: StreamType.Raw,
      inlineVolume: true
    });
    resource.volume?.setVolume(1.0);

    player.play(resource);
    await entersState(player, AudioPlayerStatus.Playing, 20_000);

    return {
      accepted: true,
      mode: "discord-voice",
      guildId,
      voiceChannelId
    };
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
        noSubscriber: NoSubscriberBehavior.Pause
      }
    });
    connection.subscribe(player);
    this.players.set(guildId, { player, connection });
    return player;
  }
}
