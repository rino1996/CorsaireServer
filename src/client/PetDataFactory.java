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

package client;

import java.io.File;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;
import java.util.HashMap;

/**
 * @name        PetDataFactory
 * @author      Leifde
 *              Modified by x711Li
 */
public class PetDataFactory {
    private static MapleDataProvider dataRoot = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Item.wz"));
    private static Map<String, PetCommand> petCommands = new HashMap<String, PetCommand>();
    private static Map<Integer, Integer> petHunger = new HashMap<Integer, Integer>();

    public static PetCommand getPetCommand(int petId, int skillId) {
        String key = petId + "." + skillId;
        PetCommand ret = petCommands.get(key);
        if (ret != null) {
            return ret;
        }
        synchronized (petCommands) {
            ret = petCommands.get(key);
            if (ret == null) {
                MapleData skillData = dataRoot.getData("Pet/" + petId + ".img");
                int prob = 0;
                int inc = 0;
                if (skillData != null) {
                    prob = MapleDataTool.getInt("interact/" + skillId + "/prob", skillData, 0);
                    inc = MapleDataTool.getInt("interact/" + skillId + "/inc", skillData, 0);
                }
                ret = new PetCommand(petId, skillId, prob, inc);
                petCommands.put(key, ret);
            }
            return ret;
        }
    }

    public static int getHunger(int petId) {
        Integer ret = petHunger.get(Integer.valueOf(petId));
        if (ret != null) {
            return ret;
        }
        synchronized (petHunger) {
            ret = petHunger.get(Integer.valueOf(petId));
            if (ret == null) {
                ret = Integer.valueOf(MapleDataTool.getInt(dataRoot.getData("Pet/" + petId + ".img").getChildByPath("info/hungry"), 1));
            }
            return ret;
        }
    }
}
