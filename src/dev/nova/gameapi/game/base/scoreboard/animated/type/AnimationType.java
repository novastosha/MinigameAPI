package dev.nova.gameapi.game.base.scoreboard.animated.type;

import org.bukkit.ChatColor;

public class AnimationType {

    private AnimationType() {}

    public static class COLOR_ROLL extends AnimationType{

        private final ChatColor doneCharacters;
        private final ChatColor characterIn;
        private final long delay;
        private final ChatColor charactersNotDone;

        public COLOR_ROLL(ChatColor doneCharacters, ChatColor characterIn,ChatColor charactersNotDone,long delay) {
            this.doneCharacters = doneCharacters;
            this.charactersNotDone = charactersNotDone;
            this.delay = delay;
            this.characterIn = characterIn;
        }

        public ChatColor getCharactersNotDone() {
            return charactersNotDone;
        }

        public long getDelay() {
            return delay;
        }

        public ChatColor getCharacterIn() {
            return characterIn;
        }

        public ChatColor getDoneCharacters() {
            return doneCharacters;
        }
    }

}
