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

package net.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import tools.Pair;

/**
 * @name        PlayerBuffStorage
 * @author      Leifde
 *              Modified by x711Li
 */
public class PlayerBuffStorage implements Serializable {
    private static final long serialVersionUID = 7273161726055369650L;
    private int id = (int) (Math.random() * 100);
    private List<Pair<Integer, List<PlayerBuffValueHolder>>> buffs = new ArrayList<Pair<Integer, List<PlayerBuffValueHolder>>>();
    private final List<Pair<Integer, List<PlayerCoolDownValueHolder>>> coolDowns = new ArrayList<Pair<Integer, List<PlayerCoolDownValueHolder>>>();
    private final List<Pair<Integer, List<PlayerDiseaseValueHolder>>> diseases = new ArrayList<Pair<Integer, List<PlayerDiseaseValueHolder>>>();

    public final void addBuffsToStorage(final int chrid, final List<PlayerBuffValueHolder> toStore) {
        for (final Pair<Integer, List<PlayerBuffValueHolder>> stored : buffs) {
            if (stored.getLeft() == Integer.valueOf(chrid)) {
                buffs.remove(stored);
            }
        }
        buffs.add(new Pair<Integer, List<PlayerBuffValueHolder>>(Integer.valueOf(chrid), toStore));
    }

    public final void addCooldownsToStorage(final int chrid, final List<PlayerCoolDownValueHolder> toStore) {
        for (final Pair<Integer, List<PlayerCoolDownValueHolder>> stored : coolDowns) {
            if (stored.getLeft() == Integer.valueOf(chrid)) {
                coolDowns.remove(stored);
            }
        }
        coolDowns.add(new Pair<Integer, List<PlayerCoolDownValueHolder>>(Integer.valueOf(chrid), toStore));
    }

    public final void addDiseaseToStorage(final int chrid, final List<PlayerDiseaseValueHolder> toStore) {
        for (final Pair<Integer, List<PlayerDiseaseValueHolder>> stored : diseases) {
            if (stored.getLeft() == Integer.valueOf(chrid)) {
                diseases.remove(stored);
            }
        }
        diseases.add(new Pair<Integer, List<PlayerDiseaseValueHolder>>(Integer.valueOf(chrid), toStore));
    }

    public final List<PlayerBuffValueHolder> getBuffsFromStorage(final int chrid) {
        List<PlayerBuffValueHolder> ret = null;
        for (int i = 0; i < buffs.size(); i++) {
            final Pair<Integer, List<PlayerBuffValueHolder>> stored = buffs.get(i);
            if (stored.getLeft().equals(chrid)) {
                ret = stored.getRight();
                buffs.remove(stored);
            }
        }
        return ret;
    }

    public final List<PlayerCoolDownValueHolder> getCooldownsFromStorage(final int chrid) {
        List<PlayerCoolDownValueHolder> ret = null;

        for (int i = 0; i < coolDowns.size(); i++) {
            final Pair<Integer, List<PlayerCoolDownValueHolder>> stored = coolDowns.get(i);
            if (stored.getLeft().equals(chrid)) {
                ret = stored.getRight();
                coolDowns.remove(stored);
            }
        }
        return ret;
    }

    public final List<PlayerDiseaseValueHolder> getDiseaseFromStorage(final int chrid) {
        List<PlayerDiseaseValueHolder> ret = null;

        for (int i = 0; i < diseases.size(); i++) {
            final Pair<Integer, List<PlayerDiseaseValueHolder>> stored = diseases.get(i);
            if (stored.getLeft().equals(chrid)) {
                ret = stored.getRight();
                diseases.remove(stored);
            }
        }
        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerBuffStorage other = (PlayerBuffStorage) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }
}
