package org.beobma.mafia42discordproject.command

import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import org.beobma.mafia42discordproject.discord.DiscordMessageManager
import org.beobma.mafia42discordproject.web.NotepadAccessResult
import org.beobma.mafia42discordproject.web.WebNotepadServer

object NotepadCommand : DiscordCommand {
    override val name: String = "notepad"
    override val description: String = "현재 게임의 개인 메모장을 엽니다."
    override val koreanName: String = "메모장"
    override val aliases: Set<String> = setOf("메모장", "노트")

    override suspend fun handle(event: GuildChatInputCommandInteractionCreateEvent) {
        when (val result = WebNotepadServer.issueAccessUrl(event.interaction.user.id)) {
            is NotepadAccessResult.Success -> DiscordMessageManager.respondEphemeral(
                event,
                "개인 메모장 링크입니다. 이 링크는 다른 사람에게 공유하지 마세요.\n<${result.url}>",
                trackReplay = false
            )
            is NotepadAccessResult.Failure -> DiscordMessageManager.respondEphemeral(
                event,
                result.message,
                trackReplay = false
            )
        }
    }

    override suspend fun handleMessage(event: MessageCreateEvent, args: List<String>) {
        val author = event.message.author ?: return
        when (val result = WebNotepadServer.issueAccessUrl(author.id)) {
            is NotepadAccessResult.Success -> {
                runCatching {
                    author.getDmChannel().createMessage(
                        "개인 메모장 링크입니다. 이 링크는 다른 사람에게 공유하지 마세요.\n<${result.url}>"
                    )
                }.onSuccess {
                    event.message.channel.createMessage("개인 메모장 링크를 DM으로 전송했습니다.")
                }.onFailure {
                    event.message.channel.createMessage("DM을 보낼 수 없습니다. Discord 개인 메시지 설정을 확인해 주세요.")
                }
            }
            is NotepadAccessResult.Failure -> event.message.channel.createMessage(result.message)
        }
    }
}
