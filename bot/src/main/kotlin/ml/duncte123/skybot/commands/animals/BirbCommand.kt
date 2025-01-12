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

package ml.duncte123.skybot.commands.animals

import com.github.natanbc.reliqua.limiter.RateLimiter
import me.duncte123.botcommons.messaging.EmbedUtils
import me.duncte123.botcommons.messaging.MessageUtils.sendEmbed
import me.duncte123.botcommons.web.WebUtils
import ml.duncte123.skybot.objects.command.Command
import ml.duncte123.skybot.objects.command.CommandCategory
import ml.duncte123.skybot.objects.command.CommandContext

class BirbCommand : Command() {

    init {
        this.category = CommandCategory.ANIMALS
        this.name = "bird"
        this.aliases = arrayOf("birb")
        this.help = "Shows a bird"
    }

    override fun execute(ctx: CommandContext) {
        WebUtils.ins.getJSONArray("https://shibe.online/api/birds") { it.setRateLimiter(RateLimiter.directLimiter()) }.async {
            sendEmbed(ctx, EmbedUtils.embedImage(it[0].asText()))
        }
    }
}
