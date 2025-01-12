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

package ml.duncte123.skybot.audio;

import com.dunctebot.sourcemanagers.IWillUseIdentifierInstead;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import ml.duncte123.skybot.CommandManager;
import ml.duncte123.skybot.commands.music.RadioCommand;
import ml.duncte123.skybot.exceptions.LimitReachedException;
import ml.duncte123.skybot.extensions.AudioTrackKt;
import ml.duncte123.skybot.extensions.StringKt;
import ml.duncte123.skybot.objects.RadioStream;
import ml.duncte123.skybot.objects.TrackUserData;
import ml.duncte123.skybot.objects.command.CommandContext;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static me.duncte123.botcommons.messaging.EmbedUtils.embedMessage;
import static me.duncte123.botcommons.messaging.MessageUtils.sendEmbed;
import static me.duncte123.botcommons.messaging.MessageUtils.sendMsg;

public class AudioLoader implements AudioLoadResultHandler {

    private final CommandContext ctx;
    private final long requester;
    private final GuildMusicManager mng;
    private final boolean announce;
    private final String trackUrl;
    private final boolean isPatron;

    public AudioLoader(CommandContext ctx, GuildMusicManager mng, boolean announce, String trackUrl, boolean isPatron) {
        this.ctx = ctx;
        this.requester = ctx.getAuthor().getIdLong();
        this.mng = mng;
        this.announce = announce;
        this.trackUrl = trackUrl;
        this.isPatron = isPatron;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        track.setUserData(new TrackUserData(this.requester));

        final TrackScheduler scheduler = this.mng.getScheduler();

        if (!this.isPatron && !scheduler.canQueue()) {
            sendMsg(this.ctx, String.format("Could not queue track because limit of %d tracks has been reached.\n" +
                "Consider supporting us on patreon to queue up unlimited songs.", TrackScheduler.MAX_QUEUE_SIZE));
            return;
        }

        try {
            scheduler.addToQueue(track, this.isPatron);

            if (this.announce) {
                final AudioTrackInfo info = track.getInfo();
                final String uri;
                if (track instanceof IWillUseIdentifierInstead) {
                    uri = info.identifier;
                } else {
                    uri = info.uri;
                }

                final String title = getSteamTitle(track, info.title, this.ctx.getCommandManager());
                final String msg = "Adding to queue: [" + StringKt.abbreviate(title, 500) + "](" + uri + ')';

                sendEmbed(this.ctx,
                    embedMessage(msg)
                        .setThumbnail(AudioTrackKt.getImageUrl(track, true))
                );
            }
        }
        catch (LimitReachedException e) {
            sendMsg(this.ctx, String.format("You exceeded the maximum queue size of %s tracks", e.getSize()));
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.getTracks().isEmpty()) {
            sendEmbed(this.ctx, embedMessage("Error: This playlist is empty."));

            return;
        }

        try {
            final TrackScheduler trackScheduler = this.mng.getScheduler();

            List<AudioTrack> tracksRaw = playlist.getTracks();
            final AudioTrack selectedTrack = playlist.getSelectedTrack();

            if (selectedTrack != null) {
                final int index = tracksRaw.indexOf(selectedTrack);

                if (index > -1) {
                    tracksRaw = tracksRaw.subList(index, tracksRaw.size());
                }
            }

            final List<AudioTrack> tracks = tracksRaw.stream().peek((track) -> {
                // don't store this externally since it will cause issues
                track.setUserData(new TrackUserData(this.requester));
            }).toList();

            for (final AudioTrack track : tracks) {
                trackScheduler.addToQueue(track, this.isPatron);
            }

            if (this.announce) {
                final String sizeMsg;

                if (playlist instanceof BigChungusPlaylist bigBoi && bigBoi.isBig()) {
                    sizeMsg = tracks.size() + "/" + bigBoi.getOriginalSize();
                } else {
                    sizeMsg = String.valueOf(tracks.size());
                }

                final String msg = String.format(
                    "Adding **%s** tracks to the queue from **%s**",
                    sizeMsg,
                    playlist.getName()
                );

                sendEmbed(this.ctx, embedMessage(msg));
            }
        }
        catch (LimitReachedException e) {
            if (this.announce) {
                sendMsg(this.ctx, String.format("The first %s tracks from %s have been queued up\n" +
                    "Consider supporting us on patreon to queue up unlimited songs.", e.getSize(), playlist.getName()));
            }
        }
    }

    @Override
    public void noMatches() {
        if (this.announce) {
            sendEmbed(this.ctx, embedMessage("Nothing found by *" + StringKt.abbreviate(this.trackUrl, MessageEmbed.VALUE_MAX_LENGTH) + '*'));
        }
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        if (exception.getCause() != null && exception.getCause() instanceof final LimitReachedException cause) {
            sendMsg(this.ctx, String.format("%s, maximum of %d tracks exceeded", cause.getMessage(), cause.getSize()));

            return;
        }

        if (!this.announce) {
            return;
        }

        if (exception.getMessage().endsWith("Playback on other websites has been disabled by the video owner.")) {
            sendEmbed(this.ctx, embedMessage("Could not play: " + this.trackUrl
                + "\nExternal playback of this video was blocked by YouTube."));
            return;
        }

        @Nullable Throwable root = ExceptionUtils.getRootCause(exception);

        if (root == null) {
            root = exception;
        }

        sendEmbed(this.ctx, embedMessage("Could not play: " + StringKt.abbreviate(root.getMessage(), MessageEmbed.VALUE_MAX_LENGTH)
            + "\nIf this happens often try another link or join our [discord server](https://duncte.bot/server) to get help!"));

    }

    private static String getSteamTitle(AudioTrack track, String rawTitle, CommandManager commandManager) {
        String title = rawTitle;

        if (track.getInfo().isStream) {
            final Optional<RadioStream> stream = ((RadioCommand) commandManager.getCommand("radio"))
                .getRadioStreams()
                .stream()
                .filter(s -> s.getUrl().equals(track.getInfo().uri)).findFirst();

            if (stream.isPresent()) {
                title = stream.get().getName();
            }
        }

        return title;
    }
}
