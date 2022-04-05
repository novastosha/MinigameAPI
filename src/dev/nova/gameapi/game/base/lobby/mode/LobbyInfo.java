package dev.nova.gameapi.game.base.lobby.mode;

import java.time.LocalDate;

public record LobbyInfo(LobbyMode mode, LocalDate start, LocalDate end,String name) {
}
