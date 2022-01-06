package dev.nova.gameapi.game.base.instance.formats.chat;

import dev.nova.gameapi.game.player.GamePlayer;

public record Message(GamePlayer player, String message) {
}
