package dev.nova.gameapi.party.role;

public enum PartyRole {

    MEMBER("Member"),
    MODERATOR("Moderator"),
    LEADER("Leader");

    private final String name;

    PartyRole(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
