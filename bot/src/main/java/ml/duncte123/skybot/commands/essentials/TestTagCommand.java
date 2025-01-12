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

package ml.duncte123.skybot.commands.essentials;

import me.duncte123.botcommons.messaging.EmbedUtils;
import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.objects.command.CommandCategory;
import ml.duncte123.skybot.objects.command.CommandContext;
import ml.duncte123.skybot.utils.CommandUtils;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import javax.annotation.Nonnull;

import static me.duncte123.botcommons.messaging.MessageUtils.sendEmbed;
import static me.duncte123.botcommons.messaging.MessageUtils.sendMsg;

public class TestTagCommand extends Command {

    public TestTagCommand() {
        this.category = CommandCategory.UTILS;
        this.name = "testtag";
        this.aliases = new String[]{
            "tt",
        };
        this.help = "Test your jagtag format before you save it as custom command etc.";
        this.usage = "<JagTag syntax>";
    }

    @Override
    public void execute(@Nonnull CommandContext ctx) {

        if (ctx.getArgs().isEmpty()) {
            this.sendUsageInstructions(ctx);
            return;
        }

        final String input = ctx.getArgsRaw();

        if (input.length() > 1000) {
            sendMsg(ctx, "Please limit your input to 1000 characters.");
            return;
        }

        final String output = CommandUtils.parseJagTag(ctx, input);

        final MessageCreateData created = new MessageCreateBuilder()
            .addContent("**Input:**")
            .addContent("```pascal\n" + input + "\n```")
            .addContent("\n")
            .addContent("**Output:**\n")
            .addContent(output)
            .build();

        try (created) {
            final String message = created.getContent();

            sendEmbed(ctx, EmbedUtils.embedMessage(message));
        }
    }
}
