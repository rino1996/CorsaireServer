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

package net.channel;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import client.BuddyList;
import client.BuddylistEntry;
import client.MapleCharacter;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.MaplePet;
import java.sql.PreparedStatement;
import tools.DatabaseConnection;
import net.ByteArrayMaplePacket;
import net.MaplePacket;
import net.channel.remote.ChannelWorldInterface;
import net.world.CharacterTransfer;
import net.world.MapleMessenger;
import net.world.MapleMessengerCharacter;
import net.world.MapleParty;
import net.world.MaplePartyCharacter;
import net.world.PartyOperation;
import net.world.guild.MapleGuildSummary;
import server.ShutdownServer;
import server.TimerManager;
import tools.factory.BuddyFactory;
import tools.factory.EffectFactory;
import tools.factory.GuildFactory;
import tools.factory.InterPersonalFactory;
import tools.factory.PartyFactory;
import tools.factory.PetFactory;


/**
 * @name        ChannelWorldInterfaceImpl
 * @author      Matze
 *              Modified by x711Li
 */
public class ChannelWorldInterfaceImpl extends UnicastRemoteObject implements ChannelWorldInterface {
    private static final long serialVersionUID = 7815256899088644192L;
    private ChannelServer server;
    private int world;

    public ChannelWorldInterfaceImpl(ChannelServer server, int world) throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        this.server = server;
        this.world = world;
    }

    public void setChannelId(int id) throws RemoteException {
        server.setChannel(id);
    }

    public int getChannelId() throws RemoteException {
        return server.getChannel();
    }

    public int getWorld() throws RemoteException {
        return world;
    }

    public String getIP() throws RemoteException {
        return server.getIP();
    }

    public void broadcastMessage(String sender, byte[] message) throws RemoteException {
        server.broadcastPacket(new ByteArrayMaplePacket(message));
    }

    public void broadcastAnnouncement(byte[] message) throws RemoteException {
        server.broadcastAnnouncementPacket(new ByteArrayMaplePacket(message));
    }

    public void whisper(String sender, String target, int channel, String message) throws RemoteException {
        if (isConnected(target)) {
            server.getPlayerStorage().getCharacterByName(target).getClient().announce(InterPersonalFactory.getWhisper(sender, channel, message));
        }
    }

    public boolean isConnected(String charName) throws RemoteException {
        return server.getPlayerStorage().getCharacterByName(charName) != null;
    }

    public void shutdown(int time) throws RemoteException {
        server.broadcastPacket(EffectFactory.serverNotice(0, "The world will be shut down in " + (time / 60000) + " minutes, please log off safely"));
        TimerManager.getInstance().schedule(new ShutdownServer(server.getChannel(), world), time);
    }

    public int getConnected() throws RemoteException {
        return server.getConnectedClients();
    }

    @Override
    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException {
        updateBuddies(characterId, channel, buddies, true);
    }

    @Override
    public void loggedOn(String name, int characterId, int channel, int buddies[]) throws RemoteException {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int buddy : buddies) {
            final MapleCharacter chr = playerStorage.getCharacterById(buddy);
            if (chr != null) {
                final BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel(-1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = channel - 1;
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient().announce(BuddyFactory.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel()) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    if (operation == PartyOperation.DISBAND) {
                        chr.setParty(null);
                    } else {
                        chr.setParty(party);
                    }
                    chr.getClient().announce(PartyFactory.updateParty(chr.getClient().getChannel(), party, operation, target));
                }
            }
        }
        switch (operation) {
        case LEAVE:
        case EXPEL:
            if (target.getChannel() == server.getChannel()) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(target.getName());
                if (chr != null) {
                    chr.getClient().announce(PartyFactory.updateParty(chr.getClient().getChannel(), party, operation, target));
                    chr.setParty(null);
                }
            }
        }
    }

    @Override
    public void partyChat(MapleParty party, String chattext, String namefrom) throws RemoteException {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == server.getChannel() && !(partychar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.getClient().announce(PartyFactory.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    public boolean isAvailable() throws RemoteException {
        return true;
    }

    public int getLocation(String name) throws RemoteException {
        MapleCharacter chr = server.getPlayerStorage().getCharacterByName(name);
        if (chr != null) {
            return server.getPlayerStorage().getCharacterByName(name).getMapId();
        }
        return -1;
    }

    @Override
    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        final MapleCharacter addChar = server.getPlayerStorage().getCharacterByName(addName);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(cidFrom)) {
                buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
            } else if (buddylist.containsVisible(cidFrom)) {
                return BuddyAddResult.ALREADY_ON_LIST;
            }
        }
        return BuddyAddResult.OK;
    }

    @Override
    public boolean isConnected(int characterId) throws RemoteException {
        return server.getPlayerStorage().getCharacterById(characterId) != null;
    }

    @Override
    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        final MapleCharacter addChar = server.getPlayerStorage().getCharacterById(cid);
        if (addChar != null) {
            final BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
            case ADDED:
                if (buddylist.contains(cidFrom)) {
                    buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, channel, true));
                    addChar.getClient().announce(BuddyFactory.updateBuddyChannel(cidFrom, channel - 1));
                }
                break;
            case DELETED:
                if (buddylist.contains(cidFrom)) {
                    buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, -1, buddylist.get(cidFrom).isVisible()));
                    addChar.getClient().announce(BuddyFactory.updateBuddyChannel(cidFrom, -1));
                }
                break;
            }
        }
    }

    @Override
    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException {
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : recipientCharacterIds) {
            final MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().announce(PartyFactory.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    @Override
    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException {
        List<Integer> ret = new ArrayList<Integer>(characterIds.length);
        final PlayerStorage playerStorage = server.getPlayerStorage();
        for (int characterId : characterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i.intValue();
        }
        return retArr;
    }

    @Override
    public void sendPacket(List<Integer> targetIds, MaplePacket packet, int exception)  throws RemoteException {
        MapleCharacter c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = server.getPlayerStorage().getCharacterById(i);
            if (c != null && c.getClient() != null && c.getClient().getSession() != null) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    @Override
    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) throws RemoteException {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    @Override
    public void setGuildAndRank(int cid, int guildid, int rank) throws RemoteException {
        final MapleCharacter mc = server.getPlayerStorage().getCharacterById(cid);
        if (mc == null) {
            return;
        }
        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            mc.getMap().broadcastMessage(mc, InterPersonalFactory.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, InterPersonalFactory.spawnPlayerMapobject(mc), false);
            MaplePet[] pets = mc.getPets();
            for (int i = 0; i < 3; i++) {
                if (pets[i] != null) {
                    mc.getMap().broadcastMessage(mc, PetFactory.showPet(mc, pets[i], false, false), false);
                }
            }
        }
    }

    @Override
    public void setOfflineGuildStatus(int guildid, byte guildrank, int cid) throws RemoteException {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
            ps.close();
            ps = null;
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    @Override
    public void reloadGuildCharacters() throws RemoteException {
        for (MapleCharacter mc : server.getPlayerStorage().getAllCharacters()) {
            if (mc.getGuildId() > 0) {
                server.getWorldInterface().setGuildMemberOnline(mc.getMGC(), true, server.getChannel());
                server.getWorldInterface().memberLevelJobUpdate(mc.getMGC());
            }
        }
        ChannelServer.getInstance(this.getChannelId()).reloadGuildSummary();
    }

    @Override
    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) throws RemoteException {
        ChannelServer.getInstance(this.getChannelId()).updateGuildSummary(gid, mgs);
        this.sendPacket(affectedPlayers, GuildFactory.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        this.setGuildAndRank(affectedPlayers, -1, -1, -1);    // respawn player
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException {
        if (isConnected(target)) {
            final MapleMessenger messenger = server.getPlayerStorage().getCharacterByName(target).getMessenger();
            if (messenger == null) {
                server.getPlayerStorage().getCharacterByName(target).getClient().announce(InterPersonalFactory.messengerInvite(sender, messengerid));
                MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from.getClient().announce(InterPersonalFactory.messengerNote(target, 4, 1));
            } else {
                MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                from.getClient().announce(InterPersonalFactory.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
            }
        }
    }

    public void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    final MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                    chr.getClient().announce(InterPersonalFactory.addMessengerPlayer(namefrom, from, position, fromchannel - 1));
                    from.getClient().announce(InterPersonalFactory.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), messengerchar.getChannel() - 1));
                }
            } else if (messengerchar.getChannel() == server.getChannel() && (messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().announce(InterPersonalFactory.joinMessenger(messengerchar.getPosition()));
                }
            }
        }
    }

    public void removeMessengerPlayer(MapleMessenger messenger, int position) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel()) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().announce(InterPersonalFactory.removeMessengerPlayer(position));
                }
            }
        }
    }

    public void messengerChat(MapleMessenger messenger, String chattext, String namefrom) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().announce(InterPersonalFactory.messengerChat(chattext));
                }
            }
        }
    }

    public void declineChat(String target, String namefrom) throws RemoteException {
        if (isConnected(target)) {
            final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(target);
            if (chr == null) {
                return;
            }
            final MapleMessenger messenger = chr.getMessenger();
            if (messenger != null) {
                chr.getClient().announce(InterPersonalFactory.messengerNote(namefrom, 5, 0));
            }
        }
    }

    public void updateMessenger(MapleMessenger messenger, String namefrom, int position, int fromchannel) throws RemoteException {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (messengerchar.getChannel() == server.getChannel() && !(messengerchar.getName().equals(namefrom))) {
                final MapleCharacter chr = server.getPlayerStorage().getCharacterByName(messengerchar.getName());
                if (chr != null) {
                    chr.getClient().announce(InterPersonalFactory.updateMessengerPlayer(namefrom, ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom), position, fromchannel - 1));
                }
            }
        }
    }

    public void sendSpouseChat(String sender, String target, String message) throws RemoteException {
    }

    public void broadcastGMMessage(String sender, byte[] message) throws RemoteException {
        server.broadcastGMPacket(new ByteArrayMaplePacket(message));
    }

    public String getAllPlayerNames() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        List<MapleCharacter> allplayers = new ArrayList<MapleCharacter>(server.getPlayerStorage().getAllCharacters());
        for (MapleCharacter c : allplayers) {
            sb.append(MapleCharacter.makeMapleReadable(c.getName()));
            sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void channelChange(CharacterTransfer transfer, int characterid) throws RemoteException {
        server.getPlayerStorage().registerPendingPlayer(transfer, characterid);
    }

    @Override
    public boolean isCharacterListConnected(List<String> charName) throws RemoteException {
        for (final String c : charName) {
            if (server.getPlayerStorage().getCharacterByName(c) != null) {
                return true;
            }
        }
        return false;
    }
}