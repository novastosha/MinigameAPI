package dev.nova.gameapi.game.base.listener;

import dev.nova.gameapi.game.base.instance.formats.chat.Message;
import dev.nova.gameapi.game.player.GamePlayer;
import dev.nova.gameapi.party.Party;
import dev.nova.gameapi.party.settings.PartySettings;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(PlayerChatEvent event){
        GamePlayer player = GamePlayer.getPlayer(event.getPlayer());

        if(player.isInParty() && player.partyChat){

            if(PartySettings.MUTED.isEnabled(player.getParty()) && player != player.getParty().getLeader()){
                Party.sendMessage(player, Component.text("§cThe party is muted"));
                event.setCancelled(true);
                return;
            }

            for(GamePlayer player1 : player.getParty().getPartyMembers().keySet()){
                player1.getPlayer().sendMessage("§3"+player.getPlayer().getName()+"§7: "+event.getMessage());
            }
            event.setCancelled(true);
        }else if(player.isInGame()){
            String message = player.getGame().getChatFormat(player).apply(new Message(player,event.getMessage()));
            player.getGame().getUtils().sendMessage(message);
            event.setCancelled(true);
        }
    }

}
