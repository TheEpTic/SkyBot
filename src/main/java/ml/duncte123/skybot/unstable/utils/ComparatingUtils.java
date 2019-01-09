/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2019  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan
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

package ml.duncte123.skybot.unstable.utils;

import ml.duncte123.skybot.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(nickname = "Sanduhr32", author = "Maurice R S")
public class ComparatingUtils {

    private static final Logger logger = LoggerFactory.getLogger(ComparatingUtils.class);

    // Needs to be fixed
    public static void execCheck(Throwable t) {
        logger.error("An error occurred", t);
        t.printStackTrace();
        Throwable cause = t.getCause();
        while (cause != null) {
            cause.printStackTrace();
            cause = cause.getCause();
        }
    }
}
