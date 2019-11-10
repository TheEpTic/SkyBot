/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2019  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ml.duncte123.skybot.web.controllers.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.entities.OAuth2Guild
import ml.duncte123.skybot.Author
import ml.duncte123.skybot.web.WebHelpers
import ml.duncte123.skybot.web.WebRouter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import spark.Request
import spark.Response
import java.util.concurrent.ThreadLocalRandom

@Author(nickname = "duncte123", author = "Duncan Sterken")
object GetUserGuilds {

    fun show(request: Request, response: Response, oAuth2Client: OAuth2Client, shardManager: ShardManager, mapper: ObjectMapper): Any {
        val attributes = request.session().attributes()

        if (!attributes.contains(WebRouter.USER_ID) || !attributes.contains(WebRouter.SESSION_ID)) {
            request.session().invalidate()

            return mapper.createObjectNode()
                .put("status", "error")
                .put("message", "SESSION_INVALID")
                .put("code", response.status())
        }

        val guilds = mapper.createArrayNode()
        val guildsRequest = oAuth2Client.getGuilds(WebHelpers.getSession(request, oAuth2Client)).complete()

        guildsRequest.forEach {
            if (it.hasPermission(Permission.ADMINISTRATOR) || it.hasPermission(Permission.MANAGE_SERVER)) {
                guilds.add(guildToJson(it, shardManager, mapper))
            }
        }

        val node = mapper.createObjectNode()
            .put("status", "success")
            .put("total", guildsRequest.size)
            .put("code", response.status())

        node.set<ObjectNode>("guilds", guilds)

        return node
    }

    private fun guildToJson(guild: OAuth2Guild, shardManager: ShardManager, mapper: ObjectMapper): JsonNode {

        val jdaGuild = shardManager.getGuildById(guild.id)

        val icon = if (!guild.iconUrl.isNullOrEmpty()) {
            guild.iconUrl
        } else {
            val number = ThreadLocalRandom.current().nextInt(0, 5)
            "https://cdn.discordapp.com/embed/avatars/$number.png"
        }

        return mapper.createObjectNode()
            .put("name", guild.name)
            .put("iconId", guild.iconId)
            .put("iconUrl", icon)
            .put("owner", guild.isOwner)
            .put("members", jdaGuild?.memberCache?.size() ?: -1)
            .put("id", guild.id)
    }
}
