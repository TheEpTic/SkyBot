package ml.duncte123.skybot.commands.fun;

import ml.duncte123.skybot.objects.command.Command;
import ml.duncte123.skybot.utils.EmbedUtils;
import ml.duncte123.skybot.utils.Settings;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Random;

public class CoinCommand extends Command {

    /**
     * This helps us to make the coinflip work
     */
    private Random rand = new Random();

    public final static String help = "flips a coin.\nUsage: `"+ Settings.prefix+"coin`";
    /**
     * this are our images
     */
    private final String[] imagesArr = { "heads.png", "tails.png" };

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeCommand(String[] args, GuildMessageReceivedEvent event) {
        String coinUrl = "https://dshelmondgames.ml/img/coin/";

        event.getChannel().sendTyping().queue();
        event.getChannel().sendMessage("*Flips a coin*").queue();
        MessageEmbed eb = EmbedUtils.embedImage(coinUrl+imagesArr[rand.nextInt(2)]);
        sendEmbed(event, eb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String help() {
        // TODO Auto-generated method stub
        return help;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "coin";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAliases() {
        return new String[]{"flip"};
    }
}