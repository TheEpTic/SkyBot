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

package ml.duncte123.skybot.entities.jda

import com.dunctebot.models.settings.GuildSetting
import me.duncte123.botcommons.messaging.EmbedUtils
import ml.duncte123.skybot.Variables
import ml.duncte123.skybot.utils.AirUtils
import ml.duncte123.skybot.utils.GuildSettingsUtils
import net.dv8tion.jda.api.entities.Guild

class DunctebotGuild(private val guild: Guild, private val variables: Variables) : Guild by guild {
    var settings: GuildSetting
        get() = GuildSettingsUtils.getGuild(this.idLong, this.variables)
        set(settings) = GuildSettingsUtils.updateGuildSettings(this.idLong, settings, this.variables)

    @Deprecated("Stored in settings now")
    var color: Int
        get() = EmbedUtils.getColorOrDefault(this.idLong) // TODO: have something that returns default
        set(color) = GuildSettingsUtils.updateEmbedColor(this.idLong, color, this.variables)

    val hexColor = AirUtils.colorToHex(color)

    override fun toString() = this.guild.toString()
}