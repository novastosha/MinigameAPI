package dev.nova.gameapi.party;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import dev.nova.gameapi.GAPIPlugin;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.invite.PartyInviteInfo;
import dev.nova.gameapi.party.poll.Poll;
import dev.nova.gameapi.party.poll.PollAnswer;
import dev.nova.gameapi.party.poll.PollData;
import dev.nova.gameapi.party.role.PartyRole;
import dev.nova.gameapi.party.settings.PartySettings;
import dev.nova.gameapi.utils.api.gui.ObjectBoundGUI;
import dev.nova.gameapi.utils.api.gui.item.ClickAction;
import dev.nova.gameapi.utils.api.gui.item.Item;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static dev.nova.gameapi.party.settings.PartySettings.*;

public class Party {

    private GamePlayer leader;
    private final ArrayList<PartyInviteInfo> pendingInvites;
    private final HashMap<GamePlayer, PartyRole> partyMembers;
    private final HashMap<Poll, Integer> runningPolls;
    private final ArrayList<PartySettings> partySettings;
    private boolean onPollCreation;
    public PollData data;
    private final ObjectBoundGUI gui = new ObjectBoundGUI("Poll settings", 3, GAPIPlugin.getPlugin(GAPIPlugin.class)) {
        @Override
        protected void onClose(InventoryCloseEvent event) {
            if (onPollCreation && data == null) {
                sendMessage(leader, Component.text(ChatColor.RED + "Poll creation canceled!"));
                onPollCreation = false;
                data = null;
            }
        }
    };

    private final ObjectBoundGUI durationGui = new ObjectBoundGUI("Duration settings", 2, GAPIPlugin.getPlugin(GAPIPlugin.class)) {
        @Override
        protected void onClose(InventoryCloseEvent event) {
            if (onPollCreation && data == null) {
                sendMessage(leader, Component.text(ChatColor.RED + "Poll creation canceled!"));
                onPollCreation = false;
                data = null;
            }
        }
    };

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
        runningPolls = new HashMap<>();
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

        pollLoop:
        for (Poll poll : runningPolls.keySet()) {
            for (ArrayList<GamePlayer> players : poll.getVotes().values()) {
                if (players.contains(partyInviteInfo.invited())) {
                    continue pollLoop;
                }
            }

            sendPolls(poll, partyInviteInfo.invited());
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

    public void displayPolls() {
        ArrayList<Component> components = new ArrayList<>();

        components.add(Component.text(" "));
        components.add(Component.text(ChatColor.GRAY + "Polls: "));

        int index = 1;
        for (Map.Entry<Poll, Integer> poll : runningPolls.entrySet()) {
            components.add(Component.text("  "));
            components.add(Component.text(ChatColor.GRAY + String.valueOf(index) + ": "));
            String toDisplay = "";

            if (poll.getKey().hasEnded()) toDisplay = "Ended";
            else {
                toDisplay = millisToTime(poll.getValue() * 1000);
            }

            components.add(Component.text(ChatColor.GRAY + "  Time Left: " +ChatColor.DARK_AQUA+ toDisplay));
            components.add(Component.text(ChatColor.GRAY + "  Question: " +ChatColor.DARK_AQUA+ poll.getKey().getData().getQuestion()));
            components.add(Component.text(ChatColor.GRAY + "  Original Duration: " +ChatColor.DARK_AQUA+ millisToTime(poll.getKey().getData().getDuration())));
            components.add(Component.text(ChatColor.GRAY + "  Answers: "));

            int allVoters = getVoters(poll.getKey());

            for (Map.Entry<PollAnswer, ArrayList<GamePlayer>> answers : poll.getKey().getVotes().entrySet()) {
                components.add(Component.text(ChatColor.GRAY+"    - Answer: "+ChatColor.DARK_AQUA+answers.getKey().answer()+" "+ChatColor.DARK_GRAY+"("+((answers.getValue().size()/allVoters)*100)+"%) "+ChatColor.GRAY+":"));
                components.add(Component.text(ChatColor.GRAY+"        People who answered this:"));
                for(GamePlayer player : answers.getValue()){
                    components.add(Component.text(ChatColor.GRAY+"          - "+ChatColor.DARK_AQUA+player.getPlayer().getName()));
                }
            }

            index++;
        }

        sendMessage(leader,components.toArray(new Component[0]));
    }

    public int getVoters(Poll key) {

        int sum = 0;
        for (Map.Entry<PollAnswer, ArrayList<GamePlayer>> answers : key.getVotes().entrySet()) {

            for (GamePlayer players : answers.getValue()) {
                sum++;
            }

        }
        return sum;
    }

    public String millisToTime(long millisS) {
        Duration duration = Duration.ofMillis(millisS);
        long millis = duration.toMillis();
        long seconds = millis / 1000;
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        long days = seconds / 86400;
        StringBuilder builder = new StringBuilder();

        if (days > 0) {
            String length = days > 1 ? "days" : "day";
            builder.append(days).append(" ").append(length).append(", ");
        }

        if (hours > 0) {
            String length = hours > 1 ? "hours" : "hour";
            builder.append(hours).append(" ").append(length).append(", ");
        }
        if (minutes > 0) {
            String length = minutes > 1 ? "minutes" : "minute";
            builder.append(minutes).append(" ").append(length).append(", ");
        }
        if (sec > 0) {
            String length = sec > 1 ? "seconds" : "second";
            builder.append(sec).append(" ").append(length).append(", ");
        }

        String s = builder.toString();

        return s.substring(s.lastIndexOf(","));
    }

    public void createPoll() {

        try {
            if (!onPollCreation) {
                PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);

                leader.getPlayer().sendBlockChange(new Location(leader.getPlayer().getWorld(), 0, 0, 0), Material.OAK_SIGN.createBlockData());
                packet.getBlockPositionModifier().write(0, new BlockPosition(0, 0, 0));

                ProtocolLibrary.getProtocolManager().sendServerPacket(leader.getPlayer(), packet);
                onPollCreation = true;
            } else {
                onPollCreation = false;
                data = null;
                throw new IllegalStateException("Tried to create a poll while onPollCreation was still true!");
            }
        } catch (Exception e) {
            sendMessage(leader, Component.text(ChatColor.RED + "An unexpected error occurred, if the error persists please report it to an an admin! aborting poll creation!"));
            e.printStackTrace();
        }
    }

    public boolean isOnPollCreation() {
        return onPollCreation;
    }

    public void handleError(Error error) {
        if (error.equals(Error.EMPTY_POLL_QUESTION)) {
            sendMessage(leader, Component.text(ChatColor.RED + "You cannot have an empty poll question!"));
            onPollCreation = false;
        }
    }

    public void startCreation(String question) {
        openGUI(new PollData(question));
    }


    public void openGUI(PollData data) {

        gui.clear();

        ItemStack cancelStack = new ItemStack(Material.RED_WOOL);

        ItemMeta metaCancel = cancelStack.getItemMeta();
        metaCancel.displayName(Component.text(ChatColor.RED + "Cancel"));
        metaCancel.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Cancels poll creation!"),
                Component.text("  ")));
        cancelStack.setItemMeta(metaCancel);


        ItemStack questionStack = new ItemStack(Material.WHITE_WOOL);

        ItemMeta metaQuestion = questionStack.getItemMeta();
        metaQuestion.displayName(Component.text(ChatColor.RESET + "Question"));
        metaQuestion.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Question: " + ChatColor.WHITE + data.getQuestion()),
                Component.text("  ")));
        questionStack.setItemMeta(metaQuestion);

        ItemStack durationStack = new ItemStack(Material.GRAY_WOOL);

        ItemMeta metaDuration = durationStack.getItemMeta();
        metaDuration.displayName(Component.text(ChatColor.GRAY + "Duration"));
        metaDuration.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Current Duration: " + ChatColor.WHITE + data.getDuration() + " seconds"),
                Component.text("  ")));
        durationStack.setItemMeta(metaDuration);

        ItemStack saveStack = new ItemStack(Material.GREEN_WOOL);

        ItemMeta metaSave = saveStack.getItemMeta();
        metaSave.displayName(Component.text(ChatColor.GREEN + "Create"));
        metaSave.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Creates your poll!"),
                Component.text("  ")));
        saveStack.setItemMeta(metaSave);

        Item<Integer> saveItem = new Item<>(8, saveStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);

                if (data.getAnswers().size() == 0) {
                    sendMessage(leader, Component.text(ChatColor.RED + "You must have 1 or more answers!"));
                    return;
                }

                Party.this.data = data;
                event.getWhoClicked().closeInventory();
                onPollCreation = false;
                Party.this.data = null;
                Poll poll = new Poll(Party.this, data);
                runningPolls.put(poll, data.getDuration());
                for (GamePlayer player : Party.this.partyMembers.keySet()) {
                    sendPolls(poll, player);
                }
            }
        });

        Item<Integer> cancelItem = new Item<>(0, cancelStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                Party.this.data = null;
                event.getWhoClicked().closeInventory();
            }
        });


        Item<Integer> questionItem = new Item<>(3, questionStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
            }
        });


        Item<Integer> durationItem = new Item<>(5, durationStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                Party.this.data = data;
                event.getWhoClicked().closeInventory();

                openDurationGUI(data);
            }
        });

        gui.addItem(questionItem);
        gui.addItem(cancelItem);
        gui.addItem(durationItem);

        int slot = 9;
        for (PollAnswer answer : data.getAnswers()) {


            ItemStack answerStack = new ItemStack(Material.YELLOW_WOOL);

            ItemMeta answerMeta = answerStack.getItemMeta();
            answerMeta.displayName(Component.text(ChatColor.GRAY + "Answer: " + ChatColor.YELLOW + answer.answer()));
            answerMeta.lore(List.of(Component.text(" "),
                    Component.text(ChatColor.GRAY + "Click to remove!"),
                    Component.text("  ")));

            answerStack.setItemMeta(answerMeta);

            Item<Integer> answerItem = new Item<>(slot, answerStack, 0).setClickAction(new ClickAction() {
                @Override
                public void onClick(InventoryClickEvent event) {
                    event.setCancelled(true);
                    Party.this.data = data;
                    data.getAnswers().remove(answer);
                    openGUI(data);
                }
            });
            gui.addItem(answerItem);
            slot++;
        }

        if (GAPIPlugin.getPlugin(GAPIPlugin.class).getConfig().getInt("maximum-poll-answers") - (slot - 9) > 0) {

            ItemStack stack = new ItemStack(Material.BEDROCK);
            ItemMeta itemMeta = stack.getItemMeta();

            itemMeta.displayName(Component.text(ChatColor.GREEN + "Add a new answer!"));
            List<Component> list = Arrays.asList(
                    Component.text("   "),
                    Component.text(ChatColor.GRAY + "Click here to add an answer!"),
                    Component.text("    ")
            );

            itemMeta.lore(list);
            stack.setItemMeta(itemMeta);

            Item<Integer> item = new Item<>(slot, stack, slot - 9).setClickAction(new ClickAction() {
                @Override
                public void onClick(InventoryClickEvent event) {
                    Party.this.data = data;
                    leader.getPlayer().closeInventory();
                    sendMessage(leader, Component.text(ChatColor.GREEN + "Creating a new answer!"));

                    PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);

                    leader.getPlayer().sendBlockChange(new Location(leader.getPlayer().getWorld(), 0, 0, 0), Material.OAK_SIGN.createBlockData());
                    packet.getBlockPositionModifier().write(0, new BlockPosition(0, 0, 0));

                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(leader.getPlayer(), packet);
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
            });

            gui.addItem(item);
        }

        gui.addItem(saveItem);
        gui.open(leader.getPlayer());

    }

    private void sendPolls(Poll poll, GamePlayer player) {

        ArrayList<Component> components = new ArrayList<>(Arrays.asList(Component.text("\n" + ChatColor.GRAY + "Poll: " + ChatColor.DARK_AQUA + poll.getData().getQuestion()),
                Component.text(ChatColor.GRAY + "Answers:"), Component.text(" ")));

        List<Poll> mainList = new ArrayList<>(runningPolls.keySet());

        int i = 1;
        for (PollAnswer answer : poll.getData().getAnswers()) {
            components.add(Component.text(ChatColor.GRAY + String.valueOf(i) + ": " + ChatColor.DARK_AQUA + answer.answer() + " [CLICK TO VOTE]").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party vote " + mainList.indexOf(poll) + " " + (i - 1))));
            i++;
        }

        sendMessage(player, components.toArray(new Component[0]));
    }

    public boolean hasVoted(Poll poll, GamePlayer player) {

        for (ArrayList<GamePlayer> players : poll.getVotes().values()) {
            if (players.contains(player)) {
                return true;
            }
        }

        return false;
    }

    private void openDurationGUI(PollData data) {

        durationGui.clear();

        int duration = data.getDuration();

        ItemStack backStack = new ItemStack(Material.RED_WOOL);

        ItemMeta metaBack = backStack.getItemMeta();
        metaBack.displayName(Component.text(ChatColor.RED + "Back"));
        metaBack.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Back to poll creation!"),
                Component.text("  ")));
        backStack.setItemMeta(metaBack);

        Item<Integer> backItem = new Item<>(0, backStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);

                Party.this.data = data;
                event.getWhoClicked().closeInventory();
                openGUI(data);
            }
        });

        ItemStack saveStack = new ItemStack(Material.GREEN_WOOL);

        ItemMeta metaSave = saveStack.getItemMeta();
        metaSave.displayName(Component.text(ChatColor.GREEN + "Save"));
        metaSave.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Saves the current duration!"),
                Component.text("  ")));
        saveStack.setItemMeta(metaSave);

        Item<Integer> saveItem = new Item<>(8, saveStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);

                Party.this.data = data;
                event.getWhoClicked().closeInventory();
                data.setDuration(duration);
                openGUI(data);
            }
        });

        ItemStack durationStack = new ItemStack(Material.GRAY_WOOL);

        ItemMeta metaDuration = durationStack.getItemMeta();
        metaDuration.displayName(Component.text(ChatColor.GRAY + "Duration"));
        metaDuration.lore(List.of(Component.text(" "),
                Component.text(ChatColor.GRAY + "Duration: " + ChatColor.WHITE + data.getDuration() + " seconds"),
                Component.text("  ")));
        durationStack.setItemMeta(metaDuration);

        Item<Integer> durationItem = new Item<>(13, durationStack, 0).setClickAction(new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
            }
        });

        /**
         *
         * GLASSES
         *
         */


        durationGui.addItem(backItem);
        durationGui.addItem(durationItem);
        durationGui.addItem(saveItem);
        durationGui.addItem(makeRemoveGlass(11, 1, new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (duration <= 0) {
                    return;
                }

                Party.this.data = data;
                data.setDuration(duration - 1);
                event.getWhoClicked().closeInventory();
                openDurationGUI(data);
            }
        }));
        durationGui.addItem(makeRemoveGlass(12, 10, new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (duration <= 0) {
                    return;
                }

                Party.this.data = data;
                data.setDuration(duration - 10);
                event.getWhoClicked().closeInventory();
                openDurationGUI(data);
            }
        }));
        durationGui.addItem(makeAddGlass(14, 1, new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (duration >= 120) {
                    return;
                }

                Party.this.data = data;
                data.setDuration(duration + 1);
                event.getWhoClicked().closeInventory();
                openDurationGUI(data);
            }
        }));
        durationGui.addItem(makeAddGlass(15, 10, new ClickAction() {
            @Override
            public void onClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (duration >= 120) {
                    return;
                }

                Party.this.data = data;
                data.setDuration(duration + 10);
                event.getWhoClicked().closeInventory();
                openDurationGUI(data);
            }
        }));

        durationGui.open(leader.getPlayer());
    }

    private Item<Integer> makeRemoveGlass(int i, int i1, ClickAction clickAction) {

        ItemStack stack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text(ChatColor.RED + "Remove " + i1 + " seconds!"));

        stack.setItemMeta(meta);

        return new Item<>(i, stack, 0).setClickAction(clickAction);
    }


    private Item<Integer> makeAddGlass(int i, int i1, ClickAction clickAction) {

        ItemStack stack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text(ChatColor.GREEN + "Add " + i1 + " seconds!"));

        stack.setItemMeta(meta);

        return new Item<>(i, stack, 0).setClickAction(clickAction);
    }

    public HashMap<Poll, Integer> getRunningPolls() {
        return runningPolls;
    }

    public enum Error {
        EMPTY_POLL_QUESTION
    }
}
