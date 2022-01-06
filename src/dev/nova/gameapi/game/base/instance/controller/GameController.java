package dev.nova.gameapi.game.base.instance.controller;

/**
 *
 * An enum class that has special options that affect the game and/or the world.
 *
 */
public enum GameController {

    /**
     * Enables weather cycling
     */
    WEATHER,
    PLACE_BLOCKS,
    BREAK_BLOCKS_MAP,
    /**
     *
     * Enables the ability for players to break their blocks.
     *
     */
    BREAK_BLOCKS,
    ITEM_PICKUP,
    ITEM_DROP,
    ARROW_PICKUPS,
    DEATH_DROPS;

}
