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

package ml.duncte123.skybot.utils;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import gnu.trove.map.TLongObjectMap;
import lavalink.client.LavalinkUtil;
import ml.duncte123.skybot.Variables;
import ml.duncte123.skybot.audio.AudioLoader;
import ml.duncte123.skybot.audio.GuildMusicManager;
import ml.duncte123.skybot.audio.sourcemanagers.DBAudioRef;
import ml.duncte123.skybot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager;
import ml.duncte123.skybot.objects.command.CommandContext;
import ml.duncte123.skybot.objects.config.DunctebotConfig;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class AudioUtils {
    // public static final String EMBED_TITLE = "AirPlayer";
    private final TLongObjectMap<GuildMusicManager> musicManagers;
    private final Variables variables;
    private final AudioPlayerManager playerManager;
    private final YoutubeAudioSourceManager youtubeManager;

    public AudioUtils(DunctebotConfig.Apis config, Variables variables) {
        this.variables = variables;
        musicManagers = MapUtils.newLongObjectMap();

        playerManager = new DefaultAudioPlayerManager();
        //playerManager.enableGcMonitoring();

//        this.youtubeManager = new YoutubeAudioSourceManagerOverride(config.googl);
        this.youtubeManager = new YoutubeAudioSourceManager();

        playerManager.registerSourceManager(new SpotifyAudioSourceManager(this.youtubeManager, config));
        playerManager.registerSourceManager(this.youtubeManager);

        DuncteBotSources.registerDuncteBot(playerManager, "en-AU", 6);
        DuncteBotSources.registerDuncteBot(LavalinkUtil.getPlayerManager(), "en-AU", 6);

        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
    }

    public AudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }

    @Nullable("If the playlist is not a basic audio playlist")
    public BasicAudioPlaylist searchYoutube(String query) {
        final AudioItem audioItem = this.youtubeManager.loadItem(null, new AudioReference("ytsearch:" + query, null));

        if (audioItem instanceof BasicAudioPlaylist playlist) {
            return playlist;
        }

        return null;
    }

    public Future<Void> loadAndPlay(final CommandContext ctx, final String trackUrlRaw,
                                    final boolean announce) {
        final boolean isPatron = CommandUtils.isUserTagPatron(ctx.getAuthor());
//        final boolean isPatron = false;
        final String trackUrl;

        //Strip <>'s that prevent discord from embedding link resources
        if (trackUrlRaw.charAt(0) == '<' && trackUrlRaw.endsWith(">")) {
            trackUrl = trackUrlRaw.substring(1, trackUrlRaw.length() - 1);
        } else {
            trackUrl = trackUrlRaw;
        }

        final GuildMusicManager mng = getMusicManager(ctx.getJDAGuild());
        final AudioLoader loader = new AudioLoader(ctx, mng, announce, trackUrl, isPatron);
        final DBAudioRef reference = new DBAudioRef(trackUrl, null, isPatron);

        return getPlayerManager().loadItemOrdered(mng, reference, loader);
    }

    // transition period
    public GuildMusicManager getMusicManager(Guild guild) {
        return getMusicManager(guild.getIdLong());
    }

    public GuildMusicManager getMusicManager(long guildId) {
        synchronized(this) {
            GuildMusicManager mng = musicManagers.get(guildId);

            if (mng == null) {
                mng = new GuildMusicManager(guildId, variables);
                musicManagers.put(guildId, mng);
            }

            return mng;
        }
    }

    public void removeMusicManager(Guild guild) {
        removeMusicManager(guild.getIdLong());
    }

    public void removeMusicManager(long guildId) {
        synchronized(this) {
            final GuildMusicManager mng = musicManagers.get(guildId);

            if (mng != null) {
                mng.stopAndClear();
                musicManagers.remove(guildId);
            }
        }
    }

    public TLongObjectMap<GuildMusicManager> getMusicManagers() {
        return musicManagers;
    }

    public static String getTimestamp(long milliseconds) {
        final int seconds = (int) (milliseconds / 1000) % 60;
        final int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        final int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
