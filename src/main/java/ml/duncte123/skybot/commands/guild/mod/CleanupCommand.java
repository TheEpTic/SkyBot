/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2018  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
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

package ml.duncte123.skybot.commands.guild.mod;

import me.duncte123.botCommons.messaging.MessageUtils;
import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.objects.command.CommandCategory;
import ml.duncte123.skybot.objects.command.CommandContext;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CleanupCommand extends Command {

    public final static String help = "performs a cleanup in the channel where the command is run.";

    public CleanupCommand() {
        this.category = CommandCategory.MOD_ADMIN;
    }

    @Override
    public void executeCommand(@NotNull CommandContext ctx) {

        GuildMessageReceivedEvent event = ctx.getEvent();
        List<String> args = ctx.getArgs();

        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)) {
            MessageUtils.sendMsg(event, "You don't have permission to run this command!");
            return;
        }

        int total = 5;
        //Little hack for lambda
        boolean keepPinned = false;
        boolean clearBots = false;

        if (args.size() > 0) {

            switch (args.size()) {
                case 1: {
                    String arg = args.get(0);
                    if (arg.equalsIgnoreCase("keep-pinned")) {
                        keepPinned = true;
                    } else if (arg.equalsIgnoreCase("bots-only")) {
                        clearBots = true;
                    }
                    break;
                }
                default: {
                    if (args.size() > 3) {
                        MessageUtils.sendErrorWithMessage(event.getMessage(), "You provided more than three arguments.");
                        return;
                    }

                    for (String arg : args) {
                        if (arg.equalsIgnoreCase("keep-pinned")) {
                            keepPinned = true;
                        } else if (arg.equalsIgnoreCase("bots-only")) {
                            clearBots = true;
                        } else if (isInteger(arg)) {
                            try {
                                total = Integer.parseInt(args.get(0));
                            } catch (NumberFormatException e) {
                                MessageUtils.sendError(event.getMessage());
                                MessageUtils.sendMsg(event, "Error: Amount to clear is not a valid number");
                                return;
                            }
                            if (total < 1 || total > 1000) {
                                MessageUtils.sendMsgAndDeleteAfter(event, 5, TimeUnit.SECONDS, "Error: count must be minimal 2 and maximal 1000");
                                return;
                            }
                        }
                    }
                    break;
                }
            }
        }

        final boolean keepPinnedFinal = keepPinned;
        final boolean clearBotsFinal = clearBots;
        final int totalFinal = total;
        TextChannel channel = event.getChannel();
        channel.getIterableHistory().takeAsync(total).thenApplyAsync((msgs) -> {
            Stream<Message> msgStream = msgs.stream();

            if (keepPinnedFinal)
                msgStream = msgStream.filter(msg -> !msg.isPinned());
            if (clearBotsFinal)
                msgStream = msgStream.filter(msg -> msg.getAuthor().isBot());

            return channel.purgeMessages(msgStream.collect(Collectors.toList()));
        }).whenCompleteAsync((aVoid, thr) -> {
            if (thr != null) {
                String cause = "";
                if (thr.getCause() != null)
                    cause = " caused by: " + thr.getCause().getMessage();
                MessageUtils.sendMsg(event, "ERROR: " + thr.getMessage() + cause);
                return;
            }
            MessageUtils.sendMsgFormatAndDeleteAfter(event, 10, TimeUnit.SECONDS,
                    "Removed %d messages!", totalFinal);
        }).exceptionally((thr) -> {
            String cause = "";
            if (thr.getCause() != null)
                cause = " caused by: " + thr.getCause().getMessage();
            MessageUtils.sendMsg(event, "ERROR: " + thr.getMessage() + cause);
            return Collections.emptyList();
        });
    }

    @Override
    public String help() {
        return "Performs a cleanup in the channel where the command is run.\n" +
                "Usage: `" + PREFIX + getName() + "[ammount] [keep-pinned]`";
    }

    @Override
    public String getName() {
        return "cleanup";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"clear", "purge"};
    }
}
