package dev.nova.gameapi.party;

import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.invite.PartyInviteInfo;
import dev.nova.gameapi.party.role.PartyRole;
import dev.nova.gameapi.party.settings.PartySettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static dev.nova.gameapi.party.settings.PartySettings.*;

public class Party {

    private GamePlayer leader;
    private final ArrayList<PartyInviteInfo> pendingInvites;
    private final HashMap<GamePlayer, PartyRole> partyMembers;
    private final ArrayList<PartySettings> partySettings;

    public Party(GamePlayer leader) {
        this.leader = leader;
        leader.setParty(this);
        this.pendingInvites = new ArrayList<>();
        this.partyMembers = new HashMap<GamePlayer, PartyRole>();
        partyMembers.put(leader, PartyRole.LEADER);
        this.partySettings = new ArrayList<PartySettings>();
        partySettings.add(BROADCAST_INVITE);
        partySettings.add(BROADCAST_JOIN);
        partySettings.add(BROADCAST_LEAVE);
        partySettings.add(BROADCAST_DECLINE);
    }

    public GamePlayer getLeader() {
        return leader;
    }

    public ArrayList<PartyInviteInfo> getPendingInvites() {
        return pendingInvites;
    }

    public ArrayList<PartySettings> getPartySettings() {
        return partySettings;
    }

    public HashMap<GamePlayer, PartyRole> getPartyMembers() {
        return partyMembers;
    }

    public void disband() {
        sendGlobalMessage(Component.text("§c" + leader.getPlayer().getName() + " disbanded the party!"));
        for (GamePlayer player : partyMembers.keySet()) {
            player.setParty(null);
        }

        for (PartyInviteInfo info : pendingInvites) {
            info.invited().getPendingPartyInvites().remove(info);
        }

        pendingInvites.clear();
        partySettings.clear();
        pendingInvites.clear();
    }

    public void invite(GamePlayer player, GamePlayer inviter) {
        boolean allow = !ALL_INVITE.isEnabled(this) ? (partyMembers.get(inviter).equals(PartyRole.MODERATOR) || partyMembers.get(inviter).equals(PartyRole.LEADER)) : true;

        if (allow) {
            if (partyMembers.containsKey(player)) {
                sendMessage(inviter, Component.text("§c" + player.getPlayer().getName() + " is already in your party!"));
                return;
            }

            int task = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(GAPIPlugin.getPlugin(GAPIPlugin.class), new Runnable() {
                @Override
                public void run() {
                    sendMessage(player, Component.text("§cThe party invite to join: " + leader.getPlayer().getName() + "'s party has expired!"));
                }
            }, 1200L);

            sendMessage(inviter, Component.text("§7You invited: " + player.getPlayer().getName() + " to join this party!")
                    , Component.text("§7They have §360 §7seconds to accept!"));

            if (BROADCAST_INVITE.isEnabled(this)) {
                sendGlobalMessage(Component.text("§3" + inviter.getPlayer().getName() + "§7 invited: §3" + player.getPlayer().getName() + " §7to your party!"));
            }

            if (inviter == leader) {
                sendMessage(player, Component.text("§3" + inviter.getPlayer().getName() + " §7invited you to join their party!")
                        , Component.text("   §a[ACCEPT]").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + inviter.getPlayer().getName())),
                        Component.text("   §c[DECLINE]").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline " + inviter.getPlayer().getName()))
                );
            } else {
                sendMessage(player, Component.text("§3" + inviter.getPlayer().getName() + " §7invited you to join §3" + leader.getPlayer().getName() + "'s §7party!"), Component.text("   §a[ACCEPT]").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + inviter.getPlayer().getName())),
                        Component.text("   §c[DECLINE]").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline " + inviter.getPlayer().getName())));
            }

            PartyInviteInfo info = new PartyInviteInfo(this, inviter, player, task);
            this.pendingInvites.add(info);
            player.getPendingPartyInvites().add(info);

        } else {
            sendMessage(inviter, Component.text("§cYou cannot invite people to this party!"));
        }
    }

    public static void sendMessage(GamePlayer player, Component... messages) {
        player.getPlayer().sendMessage("§3§l§m---------------------------------");
        Arrays.stream(messages).forEach(message -> {
            player.getPlayer().sendMessage(message);
        });
        player.getPlayer().sendMessage("§3§l§m---------------------------------");
    }

    public void accept(PartyInviteInfo partyInviteInfo) {
        Bukkit.getServer().getScheduler().cancelTask(partyInviteInfo.inviteTaskID());
        getPendingInvites().remove(partyInviteInfo);
        partyInviteInfo.invited().getPendingPartyInvites().remove(partyInviteInfo);
        partyMembers.put(partyInviteInfo.invited(), PartyRole.MEMBER);

        if (BROADCAST_JOIN.isEnabled(this)) {
            sendGlobalMessage(Component.text("§3" + partyInviteInfo.invited().getPlayer().getName() + " §7has joined your party!"));
        }

        partyInviteInfo.invited().setParty(this);
    }

    public void decline(PartyInviteInfo partyInviteInfo) {
        Bukkit.getServer().getScheduler().cancelTask(partyInviteInfo.inviteTaskID());
        getPendingInvites().remove(partyInviteInfo);
        partyInviteInfo.invited().getPendingPartyInvites().remove(partyInviteInfo);

        if (BROADCAST_DECLINE.isEnabled(this)) {
            sendGlobalMessage(Component.text("§3" + partyInviteInfo.invited().getPlayer().getName() + " §7has refused to join your party!"));
        }
    }

    public void leave(GamePlayer player) {

        if (partyMembers.get(player).equals(PartyRole.LEADER)) {
            sendMessage(player, Component.text("§cYou cannot leave the party as you are the leader! You must disband it!"));
            return;
        }
        player.setParty(null);
        partyMembers.remove(player);
        sendMessage(player, Component.text("§cYou left the party!"));

        if (BROADCAST_LEAVE.isEnabled(this)) {
            sendGlobalMessage(Component.text("§7" + player.getPlayer().getName() + " left the party!"));
        }
    }

    public void sendGlobalMessage(Component... messages) {

        for (GamePlayer player : partyMembers.keySet()) {
            player.getPlayer().sendMessage("§3§l§m---------------------------------");
            Arrays.stream(messages).forEach(message -> {
                player.getPlayer().sendMessage(message);
            });
            player.getPlayer().sendMessage("§3§l§m---------------------------------");
        }
    }

    public void transfer(GamePlayer toTransfer) {
        getPartyMembers().put(leader, PartyRole.MODERATOR);
        sendMessage(leader, Component.text("§7You are now a party moderator!"));
        sendGlobalMessage(Component.text("§7The party was transferred to: §3" + toTransfer.getPlayer().getName()));
        leader = toTransfer;
        getPartyMembers().put(leader, PartyRole.LEADER);
    }
}
