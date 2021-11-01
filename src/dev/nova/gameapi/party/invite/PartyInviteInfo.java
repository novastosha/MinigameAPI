package dev.nova.gameapi.party.invite;

import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.Party;

public record PartyInviteInfo(Party party, GamePlayer inviter, GamePlayer invited, int inviteTaskID) {

    public void accept(){
        party.accept(this);
    }

    public void decline(){
        party.decline(this);
    }

}
