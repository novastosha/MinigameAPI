package dev.nova.gameapi.party.poll;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.Party;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Poll {

    private final Party party;
    private final PollData data;
    private final HashMap<PollAnswer, ArrayList<GamePlayer>> votes;
    private boolean ended = false;

    public Poll(Party party, PollData data) {
        this.party = party;
        this.data = data;
        this.votes = new HashMap<PollAnswer, ArrayList<GamePlayer>>();
    }

    public void end() {
        if (!ended) {

            for (GamePlayer player : party.getPartyMembers().keySet()) {
                ArrayList<Component> components = new ArrayList<>();

                components.add(Component.text(ChatColor.GRAY + "  Poll Ended!"));
                components.add(Component.text( "  "));
                components.add(Component.text(ChatColor.GRAY + "  Question: " +ChatColor.DARK_AQUA+ getData().getQuestion()));
                if (player == party.getLeader()) {
                    components.add(Component.text(ChatColor.GRAY + "  Original Duration: " +ChatColor.DARK_AQUA+ party.millisToTime(getData().getDuration())));
                }
                components.add(Component.text(ChatColor.GRAY + "  Answers: "));

                int allVoters = party.getVoters(this);

                for (Map.Entry<PollAnswer, ArrayList<GamePlayer>> answers : getVotes().entrySet()) {
                    components.add(Component.text(ChatColor.GRAY + "    - Answer: " + ChatColor.DARK_AQUA + answers.getKey().answer() + " " + ChatColor.DARK_GRAY + "(" + ((answers.getValue().size() / allVoters) * 100) + "%) " + ChatColor.GRAY + ":"));
                    if (player == party.getLeader()) {
                        components.add(Component.text(ChatColor.GRAY + "        People who answered this:"));
                        for (GamePlayer playerA : answers.getValue()) {
                            components.add(Component.text(ChatColor.GRAY + "          - " + ChatColor.DARK_AQUA + playerA.getPlayer().getName()));
                        }
                    }
                }
                Party.sendMessage(player, components.toArray(new Component[0]));

            }


            ended = true;
        }
    }

    public HashMap<PollAnswer, ArrayList<GamePlayer>> getVotes() {
        return votes;
    }

    public Party getParty() {
        return party;
    }

    public PollData getData() {
        return data;
    }

    public boolean hasEnded() {
        return ended;
    }
}
