/**
    This file is part of the CorsaireServer, a fork of OdinMS
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
            Matthias Butz <matze@odinms.de>
            Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
**/

package server.quest;

/**
 * @name        MapleQuestRequirementType
 * @author      Matze
 *              Modified by x711Li
 */
public enum MapleQuestRequirementType {
    UNDEFINED(-1), job(0), item(1), quest(2), lvmin(3), lvmax(4), end(5), mob(6), npc(7), fieldEnter(8), interval(9), startscript(10), endscript(10), pet(11), pettamenessmin(12), mbmin(13), questComplete(14), pop(15), skill(16), tamingmoblevelmin(17);

    public MapleQuestRequirementType getITEM() {
    return item;
    }
    final byte type;

    private MapleQuestRequirementType(int type) {
    this.type = (byte) type;
    }

    public byte getType() {
    return type;
    }

    public static MapleQuestRequirementType getByType(byte type) {
    for (MapleQuestRequirementType l : MapleQuestRequirementType.values()) {
        if (l.getType() == type) {
        return l;
        }
    }
    return null;
    }

    public static MapleQuestRequirementType getByWZName(String name) {
    try {
        return valueOf(name);
    } catch (IllegalArgumentException ex) {
        return UNDEFINED;
    }
    }
}
