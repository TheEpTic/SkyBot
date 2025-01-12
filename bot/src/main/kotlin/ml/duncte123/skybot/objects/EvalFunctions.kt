/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ml.duncte123.skybot.objects

import me.duncte123.botcommons.messaging.EmbedUtils
import me.duncte123.botcommons.messaging.MessageConfig
import me.duncte123.botcommons.messaging.MessageUtils
import ml.duncte123.skybot.Variables
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager

@Suppress("unused")
object EvalFunctions {
    @JvmStatic
    fun isEven(number: Int): Boolean {
        return number % 2 == 0
    }

    @Suppress("UnnecessaryVariable", "LocalVariableName")
    @JvmStatic
    fun quick_mafs(x: Int): Int {
        val the_thing = x + 2 - 1
        return the_thing
    }

    @JvmStatic
    fun stats(shardManager: ShardManager, channel: MessageChannel): RestAction<Message> {
        val embed = EmbedUtils.getDefaultEmbed()
            .addField("Guilds", shardManager.guildCache.size().toString(), true)
            .addField("Users", shardManager.userCache.size().toString(), true)
            .addField("Channels", (shardManager.textChannelCache.size() + shardManager.privateChannelCache.size()).toString(), true)
            .addField("Socket-Ping", shardManager.averageGatewayPing.toString(), false).build()
        return channel.sendMessageEmbeds(embed)
    }

    @JvmStatic
    fun getSharedGuilds(event: MessageReceivedEvent): String {
        return getSharedGuilds(event.jda, event.member!!)
    }

    @JvmStatic
    fun getSharedGuilds(jda: JDA, member: Member): String {
        val shardManager = jda.shardManager

        var out = ""

        shardManager!!.getMutualGuilds(member.user).forEach {
            out += "[Shard: ${it.jda.shardInfo.shardId}]: $it\n"
        }

        return out
    }

    @JvmStatic
    fun pinnedMessageCheck(channel: MessageChannelUnion) {
        channel.retrievePinnedMessages().queue {
            MessageUtils.sendMsg(
                MessageConfig.Builder()
                    .setChannel(channel)
                    .setMessage("${it.size}/50 messages pinned in this channel")
                    .build()
            )
        }
    }

    @JvmStatic
    fun restoreCustomCommand(commandId: Int, variables: Variables): String {
        // TODO: db support?
        val (bool, customCommand) = variables.apis.restoreCustomCommand(commandId)

        if (bool) {
            variables.commandManager.customCommands.add(customCommand!!) // cc is never null here
            return "Command Restored"
        }

        return "Could not restore command"
    }
}
