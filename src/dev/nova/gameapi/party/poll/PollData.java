package dev.nova.gameapi.party.poll;

import java.util.ArrayList;

public class PollData {

    private final String question;
    private final ArrayList<PollAnswer> answers;
    private int duration = 60;

    public PollData(String question) {
        this.question = question;
        this.answers = new ArrayList<PollAnswer>();
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getQuestion() {
        return question;
    }

    public ArrayList<PollAnswer> getAnswers() {
        return answers;
    }
}
