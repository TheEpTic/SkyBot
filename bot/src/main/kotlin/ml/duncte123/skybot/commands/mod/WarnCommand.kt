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

package ml.duncte123.skybot.commands.mod

import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.DateUtils
import me.duncte123.botcommons.messaging.MessageUtils
import me.duncte123.botcommons.messaging.MessageUtils.sendErrorWithMessage
import me.duncte123.botcommons.messaging.MessageUtils.sendMsg
import ml.duncte123.skybot.commands.guild.mod.ModBaseCommand
import ml.duncte123.skybot.commands.guild.mod.TempBanCommand.getDuration
import ml.duncte123.skybot.entities.jda.DunctebotGuild
import ml.duncte123.skybot.objects.command.CommandContext
import ml.duncte123.skybot.objects.command.Flag
import ml.duncte123.skybot.utils.CommandUtils
import ml.duncte123.skybot.utils.ModerationUtils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorResponseException.ignore
import net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER
import java.util.concurrent.TimeUnit

class WarnCommand : ModBaseCommand() {

    init {
        this.requiresArgs = true
        this.name = "warn"
        this.help = "Warns a user\nWhen a user has reached 3 warnings they will be kicked from the server"
        this.usage = "<@user> [-r reason]"
        this.userPermissions = arrayOf(Permission.KICK_MEMBERS)
        this.botPermissions = arrayOf(Permission.KICK_MEMBERS, Permission.BAN_MEMBERS)
        this.flags = arrayOf(
            Flag(
                'r',
                "reason",
                "Sets the reason for this warning"
            )
        )
    }

    override fun execute(ctx: CommandContext) {
        val mentioned = ctx.getMentionedArg(0)

        if (mentioned.isEmpty()) {
            sendMsg(ctx, "I could not find any members with name ${ctx.args[0]}")
            return
        }

        val target = mentioned[0]
        val targetUser = target.user

        // The bot cannot be warned
        if (targetUser == ctx.jda.selfUser) {
            sendErrorWithMessage(ctx.message, "You can not warn me")
            return
        }

        val guild = ctx.guild
        val moderator = ctx.member
        val channel = ctx.channel
        val modUser = ctx.author

        // Check if we can interact
        if (!canInteract(moderator, target, "warn", channel)) {
            return
        }

        var reason = ""
        val flags = ctx.getParsedFlags(this)
        val args = ctx.args

        if (flags.containsKey("r")) {
            reason = flags["r"]!!.joinToString(" ")
        } else if (args.size > 1) {
            val example = "\nExample: `${ctx.prefix}warn ${args[0]} -r ${args.subList(1, args.size).joinToString(" ")}`"
            sendMsg(ctx, "Hint: if you want to set a reason, use the `-r` flag$example")
        }

        val dmMessage = """You have been warned by ${modUser.asTag}
            |Reason: ${if (reason.isEmpty()) "No reason given" else "`$reason`"}
        """.trimMargin()

        // add the new warning to the database
        val future = ctx.database.createWarning(
            modUser.idLong,
            target.idLong,
            guild.idLong,
            reason
        )

        modLog(modUser, targetUser, "warned", reason, null, guild)

        // Yes we can warn bots (cuz why not) but we cannot dm them
        if (!targetUser.isBot) {
            targetUser.openPrivateChannel()
                .flatMap { it.sendMessage(dmMessage) }
                .queue(null, ignore(CANNOT_SEND_TO_USER))
        }

        MessageUtils.sendSuccess(ctx.message)

        // Wait for the request to pass and then get the updated warn count
        future.get()

        val warnCount = ctx.database.getWarningCountForUser(targetUser.idLong, guild.idLong).get()
        val action = getSelectedWarnAction(warnCount, ctx)

        if (action != null) {
            invokeAction(warnCount, action, modUser, target, ctx)
        }
    }

    private fun getSelectedWarnAction(threshold: Int, ctx: CommandContext): WarnAction? {
        val guild = ctx.guild

        if (!CommandUtils.isGuildPatron(guild)) {
            val action = guild.settings.warnActions.firstOrNull()

            return if (action != null && threshold >= action.threshold) {
                action
            } else {
                null
            }
        }

        return guild.settings
            .warnActions
            // we reverse the list so we start with the highest one
            // preventing that everything is selected for the lowest number
            .reversed()
            .find { threshold >= it.threshold }
    }

    private fun invokeAction(warnings: Int, action: WarnAction, modUser: User, target: Member, ctx: CommandContext) {
        val guild = ctx.guild
        val targetUser = target.user

        if ((action.type == WarnAction.Type.MUTE || action.type == WarnAction.Type.TEMP_MUTE) &&
            !muteRoleCheck(guild)
        ) {
            modLog("[warn actions] Failed to apply automatic mute `${targetUser.asTag}` as there is no mute role set in the settings of this server", guild)
            return
        }

        // That's a lot of duped mod logs
        when (action.type) {
            WarnAction.Type.MUTE -> {
                applyMuteRole(target, guild)
                modLog(modUser, targetUser, "muted", "Reached $warnings warnings", null, guild)
            }
            WarnAction.Type.TEMP_MUTE -> {
                applyMuteRole(target, guild)

                val (finalDate, dur) = "${action.duration}m".toDuration()

                ctx.database.createMute(
                    modUser.idLong,
                    targetUser.idLong,
                    targetUser.asTag,
                    finalDate,
                    guild.idLong
                )

                modLog(modUser, targetUser, "muted", "Reached $warnings warnings", dur, guild)
            }
            WarnAction.Type.KICK -> {
                ctx.jdaGuild.kick(target).reason("Reached $warnings warnings").queue()
                modLog(modUser, targetUser, "kicked", "Reached $warnings warnings", null, guild)
            }
            WarnAction.Type.TEMP_BAN -> {
                val (finalUnbanDate, dur) = "${action.duration}d".toDuration()

                ctx.database.createBan(
                    modUser.idLong,
                    targetUser.idLong,
                    finalUnbanDate,
                    guild.idLong
                )

                ctx.jdaGuild.ban(target, 0, TimeUnit.DAYS)
                    .reason("Reached $warnings warnings").queue()
                modLog(modUser, targetUser, "banned", "Reached $warnings warnings", dur, guild)
            }
            WarnAction.Type.BAN -> {
                ctx.jdaGuild.ban(target, 0, TimeUnit.DAYS)
                    .reason("Reached $warnings warnings").queue()
                modLog(modUser, targetUser, "banned", "Reached $warnings warnings", null, guild)
            }
        }
    }

    private fun applyMuteRole(target: Member, guild: DunctebotGuild) {
        val roleId = guild.settings.muteRoleId
        val role = guild.getRoleById(roleId)

        if (role == null) {
            modLog("[warn actions] Failed to apply automatic mute `${target.user.asTag}` as I could not find the role that is specified in the settings", guild)
            return
        }

        guild.addRoleToMember(target, role).queue()
    }

    private fun String.toDuration(): Pair<String, String> {
        val duration = getDuration(this, null, null, null)!!

        return DateUtils.getDatabaseDateFormat(duration) to duration.toString()
    }

    private fun muteRoleCheck(guild: DunctebotGuild) = guild.settings.muteRoleId > 0
}
