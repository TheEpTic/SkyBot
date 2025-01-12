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

package ml.duncte123.skybot;

import gnu.trove.map.TIntLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.events.GatewayPingEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static gnu.trove.impl.Constants.DEFAULT_CAPACITY;
import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;
import static ml.duncte123.skybot.utils.AirUtils.setJDAContext;

public class ShardWatcher implements EventListener {
    private final TIntLongMap shardMap;
    private final Logger logger = LoggerFactory.getLogger(ShardWatcher.class);

    /* package */ ShardWatcher() {
        this.shardMap = new TIntLongHashMap(
            DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR,
            -1, -1
        );

        @SuppressWarnings("PMD.CloseResource")
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor((r) -> {
            final Thread thread = new Thread(r, "Shard-watcher");
            thread.setDaemon(true);
            return thread;
        });
        service.scheduleAtFixedRate(this::checkShards, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if (event instanceof GatewayPingEvent pingEvent) {
            this.onGatewayPing(pingEvent);
        }
    }

    // Add shard to list on ping
    // check if shards are in the list in checkShards
    // If the shard is on 0 or -1 alert
    // set shard to -1

    private void onGatewayPing(@Nonnull GatewayPingEvent event) {
        final JDA shard = event.getEntity();
        final ShardInfo info = shard.getShardInfo();

//        logger.debug("Ping event from {} ({})", info, event.getNewPing());

        this.shardMap.put(info.getShardId(), event.getNewPing());
    }

    private void checkShards() {
        final ShardManager shardManager = SkyBot.getInstance().getShardManager();

        logger.debug("Checking shards");

        for (final JDA shard : shardManager.getShardCache()) {
            setJDAContext(shard);
            final ShardInfo info = shard.getShardInfo();
            final int shardId = info.getShardId();

            if (this.shardMap.get(shardId) < 1) {
               if (Settings.AUTO_REBOOT_SHARDS) {
                   logger.warn("{} is down, rebooting it", info);
                   // We need to make sure that there are no useless reboots
                   shardManager.restart(shardId);
               } else {
                   logger.warn("{} is possibly down", info);
               }
            }


            this.shardMap.put(shardId, -1);
        }

        logger.debug("Checking done");
    }
}
