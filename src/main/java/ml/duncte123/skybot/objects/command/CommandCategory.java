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

package ml.duncte123.skybot.objects.command;

import ml.duncte123.skybot.Author;

@Author(nickname = "duncte123", author = "Duncan Sterken")
public enum CommandCategory {

    ANIMALS("animals"),
    MAIN("main"),
    FUN("fun"),
    MUSIC("music"),
    MODERATION("mod"),
    ADMINISTRATION("admin"),
    UTILS("utils"),
    PATRON("patron"),
    WEEB("weeb"),
    NSFW("nsfw"),
    LGBTQ("lgbtq+"),
    UNLISTED("null");

    private final String search;

    CommandCategory(String search) {
        this.search = search;
    }

    public String getSearch() {
        return search;
    }

    public static CommandCategory fromSearch(String input) {
        for (CommandCategory value : values()) {
            if (value.name().equalsIgnoreCase(input) || value.getSearch().equalsIgnoreCase(input)) {
                return value;
            }
        }

        return null;
    }
}
