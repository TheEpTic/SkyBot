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

package ml.duncte123.skybot.web.handlers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import io.sentry.Sentry
import ml.duncte123.skybot.Settings
import ml.duncte123.skybot.SkyBot
import ml.duncte123.skybot.Variables
import ml.duncte123.skybot.objects.api.AllPatronsData
import ml.duncte123.skybot.objects.api.Ban
import ml.duncte123.skybot.objects.api.Mute
import ml.duncte123.skybot.objects.api.Reminder
import ml.duncte123.skybot.utils.AirUtils
import ml.duncte123.skybot.utils.CommandUtils
import ml.duncte123.skybot.utils.ModerationUtils
import ml.duncte123.skybot.web.WebSocketClient
import ml.duncte123.skybot.websocket.SocketHandler
import net.dv8tion.jda.api.entities.Role
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class DataUpdateHandler(private val variables: Variables, client: WebSocketClient) : SocketHandler(client) {
    private val jackson = variables.jackson
    private val updateLock = ReentrantLock()
    private val thread = Executors.newSingleThreadExecutor()

    override fun handleInternally(data: JsonNode) {
        // swallow the update if locked
        if (updateLock.isLocked) {
            return
        }

        thread.execute {
            updateLock.lock()

            try {
                if (data.has("new_one_guild")) {
                    handleNewOneGuild(data["new_one_guild"])
                }

                if (data.has("patrons")) {
                    handlePatrons(data["patrons"])
                }

                if (data.has("unbans")) {
                    handleUnbans(data["unbans"])
                }

                if (data.has("unmutes")) {
                    handleUnmutes(data["unmutes"])
                }

                // Uses complete, must be handled last
                if (data.has("reminders")) {
                    handleReminders(data["reminders"])
                }
            } catch (e: Exception) {
                LOG.error("Data update failure!", e)
                Sentry.captureException(e)
            } finally {
                updateLock.unlock()
            }
        }
    }

    private fun handleNewOneGuild(data: JsonNode) {
        val userId = data["user_id"].asLong()
        val guildId = data["guild_id"].asLong()

        if (CommandUtils.ONEGUILD_PATRONS.containsKey(userId)) {
            return
        }

        variables.database.addOneGuildPatrons(userId, guildId).thenAccept { (_, _) ->
            val instance = SkyBot.getInstance()
            val dbGuild = instance.shardManager.getGuildById(Settings.SUPPORT_GUILD_ID) ?: return@thenAccept
            val newPatron = dbGuild.getMemberById(userId) ?: return@thenAccept

            val hasRole = newPatron.roles
                .map(Role::getIdLong)
                .any { it == Settings.ONE_GUILD_PATRONS_ROLE }

            if (hasRole) {
                CommandUtils.ONEGUILD_PATRONS.put(userId, guildId)
            }
        }
    }

    private fun handlePatrons(patrons: JsonNode) {
        if (patrons.has("add")) {
            val addedPatrons = jackson.readValue(patrons["add"].traverse(), AllPatronsData::class.java)

            CommandUtils.addPatronsFromData(addedPatrons)
        }

        if (patrons.has("remove")) {
            val removedPatrons = jackson.readValue(patrons["remove"].traverse(), AllPatronsData::class.java)

            CommandUtils.removePatronsFromData(removedPatrons)
        }
    }

    private fun handleUnbans(unbans: JsonNode) {
        val bans: List<Ban> = jackson.readValue(unbans.traverse(), object : TypeReference<List<Ban>>() {})

        ModerationUtils.handleUnban(bans, variables.database, variables)
    }

    private fun handleUnmutes(unmutes: JsonNode) {
        val mutes: List<Mute> = jackson.readValue(unmutes.traverse(), object : TypeReference<List<Mute>>() {})

        ModerationUtils.handleUnmute(mutes, variables.database, variables)
    }

    private fun handleReminders(reminders: JsonNode) {
        try {
            val parsedReminders: List<Reminder> =
                jackson.readValue(reminders.traverse(), object : TypeReference<List<Reminder>>() {})

            // Uses complete, must be handled last
            if (parsedReminders.isNotEmpty()) {
                AirUtils.handleExpiredReminders(parsedReminders, variables.database)
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
            LOG.error("Updating reminders failed", e)
        }
    }
}
