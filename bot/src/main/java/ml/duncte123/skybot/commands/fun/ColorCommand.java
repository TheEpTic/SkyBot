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

package ml.duncte123.skybot.commands.fun;

import me.duncte123.botcommons.messaging.EmbedUtils;
import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.objects.command.CommandCategory;
import ml.duncte123.skybot.objects.command.CommandContext;
import net.dv8tion.jda.api.EmbedBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Matcher;

import static me.duncte123.botcommons.messaging.MessageUtils.sendEmbed;
import static me.duncte123.botcommons.messaging.MessageUtils.sendMsg;
import static ml.duncte123.skybot.commands.guild.owner.settings.SettingsCommand.COLOR_REGEX;
import static ml.duncte123.skybot.utils.AirUtils.colorToInt;

public class ColorCommand extends Command {

    public ColorCommand() {
        this.category = CommandCategory.FUN;
        this.name = "color";
        this.aliases = new String[]{
            "colour",
        };
        this.help = "Shows a random color";
    }

    @Override
    public void execute(@Nonnull CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        String color = "random";

        if (!args.isEmpty()) {
            final String colorString = args.get(0);
            final Matcher colorMatcher = COLOR_REGEX.matcher(colorString);

            if (!colorMatcher.matches()) {
                sendMsg(ctx, "That color does not look like a valid hex color, hex colors start with a pound sign and have 6 alphanumeric characters.\n" +
                    "Tip: you can use <http://colorpicker.com/> to generate a hex code.");
                return;
            }

            color = colorString.substring(1);
        }

        ctx.getAlexFlipnote().getColour(color).async((data) -> {
            final String hex = data.hex;
            final String image = data.image;
            final int integer = data.integer;
            final int brightness = data.brightness;
            final String name = data.name;
            final String rgb = data.rgb;

            final EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
                .setColor(colorToInt(hex))
                .setThumbnail(image);

            final String desc = String.format("Name: %s%nHex: %s%nInt: %s%nRGB: %s%nBrightness: %s%nText Color: %s",
                name, hex, integer, rgb, brightness, data.blackorwhite_text);
            embed.setDescription(desc);

            sendEmbed(ctx, embed, true);
        });
    }
}
