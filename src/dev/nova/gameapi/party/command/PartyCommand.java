package dev.nova.gameapi.party.command;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.Party;
import dev.nova.gameapi.party.poll.Poll;
import dev.nova.gameapi.party.poll.PollAnswer;
import dev.nova.gameapi.party.role.PartyRole;
import dev.nova.gameapi.party.settings.PartySettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
;

import java.util.ArrayList;
import java.util.Map;

public class PartyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender instanceof Player bukkitPlayer)) {
            commandSender.sendMessage("§cOnly players can execute this command!");
            return true;
        }

        GamePlayer player = GamePlayer.getPlayer(bukkitPlayer);

        if (args.length == 0) {
            bukkitPlayer.sendMessage("§cMissing arguments!");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("create")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text(ChatColor.GREEN + "Created an empty party!"));
                    player.setParty(new Party(player));
                    return true;
                }
                Party.sendMessage(player, Component.text(ChatColor.RED + "You are already in a party!"));
            } else if (args[0].equalsIgnoreCase("list")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                ArrayList<Component> messages = new ArrayList<>();

                for (Map.Entry<GamePlayer, PartyRole> partyPlayer : party.getPartyMembers().entrySet()) {
                    messages.add(Component.text("§3- §7" + partyPlayer.getKey().getPlayer().getName() + " §3(" + partyPlayer.getValue().getName() + ")"));
                }

                Party.sendMessage(player, messages.toArray(new Component[0]));
                return true;
            } else if (args[0].equalsIgnoreCase("disband")) {

                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                if (party.getLeader() == player) {
                    party.disband();
                } else {
                    Party.sendMessage(player, Component.text("§cOnly the leader can disband the party!"));
                }

            } else if (args[0].equalsIgnoreCase("leave")) {

                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                party.leave(player);

            } else if (args[0].equalsIgnoreCase("settings")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                ArrayList<Component> messages = new ArrayList<>();

                for (PartySettings settings : PartySettings.values()) {
                    messages.add(Component.text("§7" + settings.displayName + "§3: " + settings.isEnabled(party)));
                }

                Party.sendMessage(player, messages.toArray(new Component[0]));

            } else if (args[0].equalsIgnoreCase("toggleChat")) {
                if (player.partyChat) {
                    Party.sendMessage(player, Component.text("§7You are now not chatting in party chat!"));
                } else {
                    Party.sendMessage(player, Component.text("§7You are now chatting in party chat!"));
                }
                player.partyChat = !player.partyChat;
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(player);
            } else if (args[0].equalsIgnoreCase("polls")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "You can only use this command while in a party!"));
                    return true;
                }

                if (player.getParty().getLeader() != player) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "Only the party leader can see polls!"));
                    return true;
                }

                player.getParty().displayPolls();
            } else if (args[0].equalsIgnoreCase("poll")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "You can only use this command while in a party!"));
                    return true;
                }

                if (player.getParty().getLeader() != player) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "Only the party leader can create polls!"));
                    return true;
                }

                player.getParty().createPoll();
            } else {
                Player bukkitPlayerExact = Bukkit.getPlayer(args[0]);

                if (bukkitPlayerExact == null) {
                    sendHelpMessage(player);
                    return true;
                }

                GamePlayer invited = GamePlayer.getPlayer(bukkitPlayerExact);

                if (invite(player, invited)) return true;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite")) {
                Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                if (bukkitPlayerExact == null) {
                    bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                    return true;
                }

                GamePlayer invited = GamePlayer.getPlayer(bukkitPlayerExact);

                if (invite(player, invited)) return true;

            } else if (args[0].equalsIgnoreCase("accept")) {
                if (player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are already in a party!"));
                    return true;
                }

                Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                if (bukkitPlayerExact == null) {
                    bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                    return true;
                }

                GamePlayer accepting = GamePlayer.getPlayer(bukkitPlayerExact);

                if (player.isBeingInvitedBy(accepting)) {
                    player.getInviteOf(accepting).accept();
                } else {
                    Party.sendMessage(player, Component.text("§c" + args[1] + " did not invite you to their party!"));
                }

            } else if (args[0].equalsIgnoreCase("decline")) {
                Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                if (bukkitPlayerExact == null) {
                    bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                    return true;
                }

                GamePlayer accepting = GamePlayer.getPlayer(bukkitPlayerExact);

                if (player.isBeingInvitedBy(accepting)) {
                    player.getInviteOf(accepting).decline();
                    Party.sendMessage(player, Component.text("§cYou declined to join: " + args[1] + "'s party!"));
                } else {
                    Party.sendMessage(player, Component.text("§c" + args[1] + " did not invite you to their party!"));
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {

                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                if (party.getPartyMembers().get(player).equals(PartyRole.LEADER) || party.getPartyMembers().get(player).equals(PartyRole.MODERATOR)) {
                    PartySettings setting;
                    try {
                        setting = PartySettings.valueOf(args[1].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        Party.sendMessage(player, Component.text("§cUnknown setting: " + args[1].toUpperCase()));
                        return true;
                    }

                    if (setting.isEnabled(party)) {
                        party.getPartySettings().remove(setting);
                    } else {
                        party.getPartySettings().add(setting);
                    }

                    party.sendGlobalMessage(Component.text("§7Toggled setting: " + args[1].toUpperCase() + " to: §3" + setting.isEnabled(party)));
                } else {
                    Party.sendMessage(player, Component.text("§cYou cannot modify settings!"));
                }

            } else if (args[0].equalsIgnoreCase("cycleRole")) {
                Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                if (bukkitPlayerExact == null) {
                    bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                    return true;
                }

                GamePlayer toCycle = GamePlayer.getPlayer(bukkitPlayerExact);

                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                if (party.getLeader() != player) {
                    Party.sendMessage(player, Component.text("§cOnly the leader can change roles!"));
                    return true;
                }

                if (!party.getPartyMembers().containsKey(toCycle)) {
                    Party.sendMessage(player, Component.text("§c" + args[1] + " is not in your party!"));
                    return true;
                }

                PartyRole current = party.getPartyMembers().get(toCycle);

                if (current.equals(PartyRole.MODERATOR)) {
                    party.getPartyMembers().put(toCycle, PartyRole.MEMBER);
                    party.sendGlobalMessage(Component.text("§7" + args[1] + " has been demoted to a Member!"));
                } else {
                    party.getPartyMembers().put(toCycle, PartyRole.MODERATOR);
                    party.sendGlobalMessage(Component.text("§7" + args[1] + " has been promoted to a Moderator!"));
                }
            } else if (args[0].equalsIgnoreCase("transfer")) {
                Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                if (bukkitPlayerExact == null) {
                    bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                    return true;
                }

                GamePlayer toTransfer = GamePlayer.getPlayer(bukkitPlayerExact);

                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text("§cYou are not in a party!"));
                    return true;
                }
                Party party = player.getParty();

                if (party.getLeader() != player) {
                    Party.sendMessage(player, Component.text("§cOnly the leader can transfer the party leadership!"));
                    return true;
                }

                if (!party.getPartyMembers().containsKey(toTransfer)) {
                    Party.sendMessage(player, Component.text("§c" + toTransfer.getPlayer().getName() + " is not in your party!"));
                    return true;
                }

                party.transfer(toTransfer);
            } else if (args[0].equalsIgnoreCase("yoink")) {
                if (player.getPlayer().hasPermission("gameapi.party.yoink")) {
                    Player bukkitPlayerExact = Bukkit.getPlayerExact(args[1]);

                    if (bukkitPlayerExact == null) {
                        bukkitPlayer.sendMessage("§cCould not find a player with the name: " + args[1]);
                        return true;
                    }

                    GamePlayer toYoink = GamePlayer.getPlayer(bukkitPlayerExact);

                    if (!player.isInParty()) {
                        player.setParty(new Party(player));
                    }

                    Party party = player.getParty();

                    if (party.getPartyMembers().containsKey(toYoink)) {
                        Party.sendMessage(player, Component.text("§c" + args[1] + " is already in your party!"));
                        return true;
                    }

                    Party otherParty = toYoink.getParty();
                    if (otherParty != null) {
                        if (otherParty.getLeader() == toYoink) {
                            otherParty.disband();
                        } else {
                            otherParty.leave(toYoink);
                        }
                    }

                    party.getPartyMembers().put(toYoink, PartyRole.MEMBER);
                    toYoink.setParty(party);

                    Party.sendMessage(toYoink, Component.text("§7You have been yoinked by: §3" + player.getPlayer().getName() + " §7into their party!"));
                    party.sendGlobalMessage(Component.text("§3" + toYoink.getPlayer().getName() + " §7has been yoinked into the party by: §3" + bukkitPlayer.getName()));

                } else {
                    Party.sendMessage(player, Component.text("§cYou don't have permission to execute this command!"));
                    return true;
                }
            } else {
                sendHelpMessage(player);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("vote")) {
                if (!player.isInParty()) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "You must be in a party!"));
                    return true;
                }
                try {
                    int indexPoll = Integer.parseInt(args[1]);
                    int indexAnswer = Integer.parseInt(args[2]);

                    if (indexPoll >= player.getParty().getRunningPolls().keySet().size()) {
                        throw new NumberFormatException();
                    }

                    Poll poll = player.getParty().getRunningPolls().keySet().toArray(new Poll[0])[indexPoll];

                    if (indexAnswer >= poll.getData().getAnswers().size()) {
                        throw new NumberFormatException();
                    }

                    if (poll.hasEnded()) {
                        Party.sendMessage(player, Component.text(ChatColor.RED + "This poll has already ended!"));
                        return true;
                    }

                    if (player.getParty().hasVoted(poll, player)) {
                        Party.sendMessage(player, Component.text(ChatColor.RED + "You already voted to this poll!"));
                        return true;
                    }

                    PollAnswer answer = poll.getData().getAnswers().get(indexAnswer);

                    ArrayList<GamePlayer> voters = poll.getVotes().getOrDefault(answer, new ArrayList<>());

                    voters.add(player);

                    poll.getVotes().put(answer, voters);

                    Party.sendMessage(player, Component.text(ChatColor.GRAY + "You answered: "+ChatColor.DARK_AQUA+answer.answer()));
                    return true;

                } catch (NumberFormatException e) {
                    Party.sendMessage(player, Component.text(ChatColor.RED + "Invalid index!"));
                    return true;
                }
            }
        } else {
            sendHelpMessage(player);
        }

        return true;
    }

    private boolean invite(GamePlayer player, GamePlayer invited) {
        if (invited == player) {
            Party.sendMessage(player, Component.text("§cYou cannot invite yourself!"));
            return true;
        }

        if (!player.isInParty()) {
            player.setParty(new Party(player));
        }
        Party party = player.getParty();

        party.invite(invited, player);
        return false;
    }

    private void sendHelpMessage(GamePlayer player) {
        Party.sendMessage(player,
                Component.text("§3<>§7 means required§3 []§7 means optional"),
                Component.text("  "),
                Component.text("§3/party help§7 - Shows this message.")
                , Component.text("§3/party list§7 - Lists all the players in your party."),
                Component.text("§3/party disband§7 - Disbands the party (Leader only)."),
                Component.text("§3/party leave§7 - Leave your current party."),
                Component.text("§3/party toggleChat§7 - Toggles party chat on or off."),
                Component.text("§3/party settings§7 - Lists the settings of the party."),
                Component.text("§3/party [invite] <player>§7 - Invites a player to your party."),
                Component.text("§3/party accept <player>§7 - Accepts a party invite."),
                Component.text("§3/party decline <player>§7 - Declines a player's invite."),
                Component.text("§3/party toggle <setting>§7 - Toggles a setting on or off."),
                Component.text("§3/party cyclerole <player>§7 - Changes a player's role (Leader only)."),
                Component.text("§3/party transfer <player>§7 - Transfers the party to an other player!"),
                Component.text("§3/party yoink <player>§7 - Forcefully makes a player join."),
                Component.text("§3/party vote <poll-index> <answer-index>§7 - Vote for a poll, (however, most of the time this is done by just clicking a chat message)")
        );
    }
}
