package ml.duncte123.skybot.commands.fun;

import ml.duncte123.skybot.Command;
import ml.duncte123.skybot.utils.AirUtils;
import ml.duncte123.skybot.utils.URLConnectionReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class DogCommand extends Command {

    @Override
    public boolean called(String[] args, GuildMessageReceivedEvent event) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void action(String[] args, GuildMessageReceivedEvent event) {
        String base = "https://random.dog/";
        try {
            String jsonString = URLConnectionReader.getText(base + "woof");
            String finalS = base + jsonString;

            if (finalS.contains(".mp4")) {
                event.getChannel().sendMessage(AirUtils.embedField("A video", "[OMG LOOK AT THIS CUTE VIDEO](" + finalS + ")")).queue();
            } else {
                event.getChannel().sendMessage(AirUtils.embedImage(finalS)).queue();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            event.getChannel().sendMessage(AirUtils.embedMessage("**[OOPS]** Something broke, blame duncte")).queue();
        }

    }

    @Override
    public String help() {
        // TODO Auto-generated method stub
        return "here is a dog.";
    }
}