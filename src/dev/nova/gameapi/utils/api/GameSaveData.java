package dev.nova.gameapi.utils.api;

import dev.nova.gameapi.game.base.GameBase;

public record GameSaveData(GameBase base, String instance) {
}
