package dev.nova.gameapi.party.settings;

import dev.nova.gameapi.party.Party;

public enum PartySettings {

    ALL_INVITE("All Invite"),
    MUTED("Muted party"),
    BROADCAST_DECLINE("Broadcast Invite Decline"),
    BROADCAST_JOIN("Broadcast Player Join"),
    BROADCAST_LEAVE("Broadcast Player Leave"),
    BROADCAST_INVITE("Broadcast Player Invite");

    public final String displayName;

    PartySettings(String s) {
        this.displayName = s;
    }

    public boolean isEnabled(Party party){
        return party.getPartySettings().contains(this);
    }

}
